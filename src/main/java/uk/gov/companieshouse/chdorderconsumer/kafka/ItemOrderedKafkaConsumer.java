package uk.gov.companieshouse.chdorderconsumer.kafka;

import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.COMPANY_NUMBER;
import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.ORDER_REFERENCE_NUMBER;
import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.PAYMENT_REFERENCE;
import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.logIfNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.chdorderconsumer.exception.DuplicateErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils;
import uk.gov.companieshouse.chdorderconsumer.service.ItemOrderedProcessorService;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;

@Service
public class ItemOrderedKafkaConsumer implements ConsumerSeekAware {

    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_KEY_RETRY = CHD_ITEM_ORDERED_TOPIC_RETRY;
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";

    private static final String CHD_ITEM_ORDERED_GROUP =
            APPLICATION_NAMESPACE + "-" + CHD_ITEM_ORDERED_TOPIC;
    private static final String CHD_ITEM_ORDERED_GROUP_RETRY =
            APPLICATION_NAMESPACE + "-" + CHD_ITEM_ORDERED_TOPIC_RETRY;
    private static final String CHD_ITEM_ORDERED_GROUP_ERROR =
            APPLICATION_NAMESPACE + "-" + CHD_ITEM_ORDERED_TOPIC_ERROR;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long ERROR_RECOVERY_OFFSET = 0L;

    private static final Logger LOGGER = LoggingUtils.getLogger();

    private final Map<String, Integer> retryCount;

    private final SerializerFactory serializerFactory;
    private final ItemOrderedKafkaProducer kafkaProducer;
    private final KafkaListenerEndpointRegistry registry;
    private final ItemOrderedProcessorService processor;

    public ItemOrderedKafkaConsumer(SerializerFactory serializerFactory,
                                    ItemOrderedKafkaProducer kafkaProducer, KafkaListenerEndpointRegistry registry,
                                    ItemOrderedProcessorService processor) {
        this.retryCount = new HashMap<>();
        this.serializerFactory = serializerFactory;
        this.kafkaProducer = kafkaProducer;
        this.registry = registry;
        this.processor = processor;
    }

    /**
     * Main listener/consumer. Calls `handleMessage` method to process received message.
     *
     * @param message
     */
    @KafkaListener(id = CHD_ITEM_ORDERED_GROUP, groupId = CHD_ITEM_ORDERED_GROUP,
            topics = CHD_ITEM_ORDERED_TOPIC,
            autoStartup = "#{!${uk.gov.companieshouse.chdorderconsumer.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processChdItemOrdered(org.springframework.messaging.Message<ChdItemOrdered> message) {
        handleMessage(message);
    }

    /**
     * Retry (`-retry`) listener/consumer. Calls `handleMessage` method to process received message.
     *
     * @param message
     */
    @KafkaListener(id = CHD_ITEM_ORDERED_GROUP_RETRY, groupId = CHD_ITEM_ORDERED_GROUP_RETRY,
            topics = CHD_ITEM_ORDERED_TOPIC_RETRY,
            autoStartup = "#{!${uk.gov.companieshouse.chdorderconsumer.error-consumer}}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processChdItemOrderedRetry(
            org.springframework.messaging.Message<ChdItemOrdered> message) {
        handleMessage(message);
    }

    /**
     * Error (`-error`) topic listener/consumer is enabled when the application is launched in error
     * mode (IS_ERROR_QUEUE_CONSUMER=true). Receives messages up to `ERROR_RECOVERY_OFFSET` offset.
     * Calls `handleMessage` method to process received message. If the `retryable` processor is
     * unsuccessful with a `retryable` error, after maximum numbers of attempts allowed, the message
     * is republished to `-retry` topic for failover processing. This listener stops accepting
     * messages when the topic's offset reaches `ERROR_RECOVERY_OFFSET`.
     *
     * @param message
     */
    @KafkaListener(id = CHD_ITEM_ORDERED_GROUP_ERROR, groupId = CHD_ITEM_ORDERED_GROUP_ERROR,
            topics = CHD_ITEM_ORDERED_TOPIC_ERROR,
            autoStartup = "${uk.gov.companieshouse.chdorderconsumer.error-consumer}",
            containerFactory = "kafkaListenerContainerFactory")
    public void processChdItemOrderedError(
            org.springframework.messaging.Message<ChdItemOrdered> message) {
        long offset = Long.parseLong("" + message.getHeaders().get("kafka_offset"));
        if (offset <= ERROR_RECOVERY_OFFSET) {
            handleMessage(message);
        } else {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logMap.put(LoggingUtils.CHD_ITEM_ORDERED_GROUP_ERROR, ERROR_RECOVERY_OFFSET);
            logMap.put(LoggingUtils.TOPIC, CHD_ITEM_ORDERED_TOPIC_ERROR);
            LOGGER.info("Pausing error consumer as error recovery offset reached.",
                    logMap);
            registry.getListenerContainer(CHD_ITEM_ORDERED_GROUP_ERROR).pause();
        }
    }

    /**
     * Handles processing of received message.
     *
     * @param message
     */
    protected void handleMessage(org.springframework.messaging.Message<ChdItemOrdered> message) {
        final ChdItemOrdered order = message.getPayload();
        final String orderReference = order.getReference();
        final MessageHeaders headers = message.getHeaders();
        final String receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC).toString();
        try {
            logMessageReceived(message, order);

            // process message
            processor.processItemOrdered(order);

            // on successful processing remove counterKey from retryCount
            if (retryCount.containsKey(orderReference)) {
                resetRetryCount(receivedTopic + "-" + orderReference);
            }
            logMessageProcessed(message, order);
        } catch (RetryableErrorException ex) {
            retryMessage(message, order, orderReference, receivedTopic, ex);
        } catch (DuplicateErrorException dx) {
            logMessageProcessingFailureDuplicateItem(message, dx);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, x);
        }
    }

    protected void logMessageReceived(org.springframework.messaging.Message<ChdItemOrdered> message,
                                      ChdItemOrdered order) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        populateChdMessageLogMap(order, logMap);
        LOGGER.info("'chd-item-ordered' message received", logMap);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<ChdItemOrdered> message,
                                     ChdItemOrdered order) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        populateChdMessageLogMap(order, logMap);
        LOGGER.info("'chd-item-ordered' message processing completed", logMap);
    }

    /**
     * Retries a message that failed processing with a `RetryableErrorException`. Checks which topic
     * the message was received from and whether any retry attempts remain. The message is published
     * to the next topic for failover processing, if retries match or exceed `MAX_RETRY_ATTEMPTS`.
     *
     * @param message
     * @param order
     * @param orderReference
     * @param receivedTopic
     * @param ex
     */
    private void retryMessage(org.springframework.messaging.Message<ChdItemOrdered> message,
                              final ChdItemOrdered order,
                              String orderReference, String receivedTopic, RetryableErrorException ex) {
        String nextTopic = (receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC)
                || receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC_ERROR)) ? CHD_ITEM_ORDERED_TOPIC_RETRY
                : CHD_ITEM_ORDERED_TOPIC_ERROR;
        String counterKey = receivedTopic + "-" + orderReference;

        if (receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC)
                || retryCount.getOrDefault(counterKey, 1) >= MAX_RETRY_ATTEMPTS) {
            republishMessageToTopic(order, orderReference, receivedTopic, nextTopic);
            if (!receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC)) {
                resetRetryCount(counterKey);
            }
        } else {
            retryCount.put(counterKey, retryCount.getOrDefault(counterKey, 1) + 1);
            logMessageProcessingFailureRecoverable(message, retryCount.get(counterKey), ex);
            // retry
            handleMessage(message);
        }
    }

    protected void republishMessageToTopic(final ChdItemOrdered order,
                                           final String orderReference,
                                           final String currentTopic,
                                           final String nextTopic) {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, orderReference);
        logIfNotNull(logMap, LoggingUtils.CURRENT_TOPIC, currentTopic);
        logIfNotNull(logMap, LoggingUtils.NEXT_TOPIC, nextTopic);
        LOGGER.info(String.format(
                "Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
                orderReference, currentTopic, nextTopic), logMap);
        try {
            kafkaProducer.sendMessage(createRetryMessage(order, orderReference, nextTopic));
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error(String.format("Error sending message: \"%1$s\" to topic: \"%2$s\"",
                    orderReference, nextTopic), e, logMap);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected Message createRetryMessage(final ChdItemOrdered order,
                                         final String orderReference,
                                         final String topic) {
        final Message message = new Message();
        final AvroSerializer<ChdItemOrdered> serializer =
                serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class);
        message.setKey(CHD_ITEM_ORDERED_KEY_RETRY);
        try {
            message.setValue(serializer.toBinary(order));
        } catch (SerializationException e) {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            logIfNotNull(logMap, LoggingUtils.MESSAGE, orderReference);
            logIfNotNull(logMap, LoggingUtils.TOPIC, topic);
            logIfNotNull(logMap, LoggingUtils.OFFSET, message.getOffset());
            LOGGER.error(String.format("Error serializing message: \"%1$s\" for topic: \"%2$s\"",
                    orderReference, topic), e, logMap);
        }
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    protected void logMessageProcessingFailureNonRecoverable(
            org.springframework.messaging.Message<ChdItemOrdered> message, Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LOGGER.error("'chd-item-ordered' message processing failed with a non-recoverable exception",
                exception, logMap);
    }

    protected void logMessageProcessingFailureDuplicateItem(
            org.springframework.messaging.Message<ChdItemOrdered> message, Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LOGGER.error("'chd-item-ordered' message processing failed item already exists",
                exception, logMap);
    }

    protected void logMessageProcessingFailureRecoverable(
            org.springframework.messaging.Message<ChdItemOrdered> message, int attempt,
            Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.RETRY_ATTEMPT, attempt);
        LOGGER.error("'chd-item-ordered' message processing failed with a recoverable exception",
                exception, logMap);
    }

    /**
     * Resets retryCount for message identified by key `counterKey`
     *
     * @param counterKey
     */
    private void resetRetryCount(String counterKey) {
        retryCount.remove(counterKey);
    }

    @Override
    public void registerSeekCallback(@NonNull ConsumerSeekCallback consumerSeekCallback) {

    }

    @Override
    public void onPartitionsAssigned(@NonNull Map<TopicPartition, Long> map, @NonNull ConsumerSeekCallback consumerSeekCallback) {

    }

    @Override
    public void onIdleContainer(@NonNull Map<TopicPartition, Long> map, @NonNull ConsumerSeekCallback consumerSeekCallback) {

    }

    /**
     * Populates the log map provided with key values for the tracking of inbound messages.
     * @param order the order extracted from the inbound message
     * @param logMap the log map to populate values from the order with
     */
    private void populateChdMessageLogMap(final ChdItemOrdered order, final Map<String, Object> logMap) {
        logIfNotNull(logMap, ORDER_REFERENCE_NUMBER, order.getReference());
        logIfNotNull(logMap, PAYMENT_REFERENCE, order.getPaymentReference());
        final Item firstItem = order.getItem();
        logIfNotNull(logMap, ITEM_ID, firstItem.getId());
        logIfNotNull(logMap, COMPANY_NUMBER, firstItem.getCompanyNumber());
    }
}
