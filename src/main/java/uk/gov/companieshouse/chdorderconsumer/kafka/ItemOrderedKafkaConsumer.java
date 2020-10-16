package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.APPLICATION_NAMESPACE;

@Service
public class ItemOrderedKafkaConsumer implements ConsumerSeekAware {

    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_KEY_RETRY = CHD_ITEM_ORDERED_TOPIC_RETRY;
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String CHD_ITEM_ORDERED_GROUP =
            APPLICATION_NAMESPACE + "-" + CHD_ITEM_ORDERED_TOPIC;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final Map<String, Integer> retryCount;

    private final SerializerFactory serializerFactory;
    private final ItemOrderedKafkaProducer kafkaProducer;

    public ItemOrderedKafkaConsumer(Map<String, Integer> retryCount, SerializerFactory serializerFactory, ItemOrderedKafkaProducer kafkaProducer) {
        this.retryCount = new HashMap<>();
        this.serializerFactory = serializerFactory;
        this.kafkaProducer = kafkaProducer;
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
    public void processOrderReceived(org.springframework.messaging.Message<OrderReceived> message) {
        handleMessage(message);
    }

    /**
     * Handles processing of received message.
     *
     * @param message
     */
    protected void handleMessage(org.springframework.messaging.Message<OrderReceived> message) {
        OrderReceived msg = message.getPayload();
        String orderReceivedUri = msg.getOrderUri();
        MessageHeaders headers = message.getHeaders();
        String receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC).toString();
        try {
            logMessageReceived(message, orderReceivedUri);

            // on successful processing remove counterKey from retryCount
            if (retryCount.containsKey(orderReceivedUri)) {
                resetRetryCount(receivedTopic + "-" + orderReceivedUri);
            }
            logMessageProcessed(message, orderReceivedUri);
        } catch (RetryableErrorException ex) {
            retryMessage(message, orderReceivedUri, receivedTopic, ex);
        } catch (Exception x) {
            logMessageProcessingFailureNonRecoverable(message, x);
        }
        System.out.println(orderReceivedUri);
    }

    protected void logMessageReceived(org.springframework.messaging.Message<OrderReceived> message,
                                      String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("'chd-item-ordered' message received", logMap);
    }

    private void logMessageProcessed(org.springframework.messaging.Message<OrderReceived> message,
                                     String orderUri) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.getLogger().info("'chd-item-ordered' message processing completed", logMap);
    }

    /**
     * Retries a message that failed processing with a `RetryableErrorException`. Checks which topic
     * the message was received from and whether any retry attempts remain. The message is published
     * to the next topic for failover processing, if retries match or exceed `MAX_RETRY_ATTEMPTS`.
     *
     * @param message
     * @param orderReceivedUri
     * @param receivedTopic
     * @param ex
     */
    private void retryMessage(org.springframework.messaging.Message<OrderReceived> message,
                              String orderReceivedUri, String receivedTopic, RetryableErrorException ex) {
        String nextTopic = (receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC)
                || receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC_ERROR)) ? CHD_ITEM_ORDERED_TOPIC_RETRY
                : CHD_ITEM_ORDERED_TOPIC_ERROR;
        String counterKey = receivedTopic + "-" + orderReceivedUri;

        if (receivedTopic.equals(CHD_ITEM_ORDERED_TOPIC)
                || retryCount.getOrDefault(counterKey, 1) >= MAX_RETRY_ATTEMPTS) {
            republishMessageToTopic(orderReceivedUri, receivedTopic, nextTopic);
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

    protected void republishMessageToTopic(String orderUri, String currentTopic, String nextTopic) {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_URI, orderUri);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.CURRENT_TOPIC, currentTopic);
        LoggingUtils.logIfNotNull(logMap, LoggingUtils.NEXT_TOPIC, nextTopic);
        LoggingUtils.getLogger().info(String.format(
                "Republishing message: \"%1$s\" received from topic: \"%2$s\" to topic: \"%3$s\"",
                orderUri, currentTopic, nextTopic), logMap);
        try {
            kafkaProducer.sendMessage(createRetryMessage(orderUri, nextTopic));
        } catch (ExecutionException | InterruptedException e) {
            LoggingUtils.getLogger().error(String.format("Error sending message: \"%1$s\" to topic: \"%2$s\"",
                    orderUri, nextTopic), e, logMap);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected Message createRetryMessage(String orderUri, String topic) {
        final Message message = new Message();
        AvroSerializer serializer =
                serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(CHD_ITEM_ORDERED_KEY_RETRY);
        try {
            message.setValue(serializer.toBinary(orderReceived));
        } catch (SerializationException e) {
            Map<String, Object> logMap = LoggingUtils.createLogMap();
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.MESSAGE, orderUri);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.TOPIC, topic);
            LoggingUtils.logIfNotNull(logMap, LoggingUtils.OFFSET, message.getOffset());
            LoggingUtils.getLogger().error(String.format("Error serializing message: \"%1$s\" for topic: \"%2$s\"",
                    orderUri, topic), e, logMap);
        }
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }

    protected void logMessageProcessingFailureNonRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        LoggingUtils.getLogger().error("'chd-item-ordered' message processing failed with a non-recoverable exception",
                exception, logMap);
    }

    protected void logMessageProcessingFailureRecoverable(
            org.springframework.messaging.Message<OrderReceived> message, int attempt,
            Exception exception) {
        Map<String, Object> logMap = LoggingUtils.getMessageHeadersAsMap(message);
        logMap.put(LoggingUtils.RETRY_ATTEMPT, attempt);
        LoggingUtils.getLogger().error("'order-received' message processing failed with a recoverable exception",
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
    public void registerSeekCallback(ConsumerSeekCallback consumerSeekCallback) {

    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {

    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> map, ConsumerSeekCallback consumerSeekCallback) {

    }
}
