package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.aspectj.lang.annotation.Aspect;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chdorderconsumer.util.TestUtils.createOrder;

@DirtiesContext
@Aspect
@Service
public class ItemOrderedKafkaConsumerWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private CountDownLatch latch = new CountDownLatch(1);
    private String messagePayload;
    private CHConsumerType testType = CHConsumerType.MAIN_CONSUMER;
    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;
    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String ORDER_REFERENCE = "ORD-123456-123456";
    private static final String CHD_ITEM_ORDERED_KEY_RETRY = CHD_ITEM_ORDERED_TOPIC_RETRY;
    @Autowired
    private ItemOrderedKafkaProducer kafkaProducer;
    @Autowired
    private ItemOrderedKafkaConsumer kafkaConsumer;
    @Autowired
    private SerializerFactory serializerFactory;

    /**
     * mock message processing failure scenario for main listener
     * so that the message can be published to alternate topics '-retry' and '-error'
     * @param message
     * @throws Exception
     */
    @Before(value = "execution(* uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaConsumer.processChdItemOrdered(..)) && args(message)")
    public void beforeItemOrderedProcessed(final Message message) throws Exception {
        LOGGER.info("ItemOrderedKafkaConsumer.processChdItemOrdered() @Before triggered");
        if (this.testType != CHConsumerType.MAIN_CONSUMER) {
            throw new Exception("Mock main listener exception");
        }
    }

    /**
     * mock exception handler to demonstrate publishing of unprocessed
     * message to alternate topics '-retry' and '-error'
     * @param x
     * @throws Throwable
     */
    @AfterThrowing(pointcut = "execution(* uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaConsumer.*(..))", throwing = "x")
    public void orderProcessedException(final Exception x) throws Throwable {
        LOGGER.info("ItemOrderedKafkaConsumer.processChdItemOrdered() @AfterThrowing triggered");

        setUpTestKafkaItemOrderedProducerAndSendMessageToTopic();
    }

    /**
     * emulates receiving of message from kafka topics
     * @param message
     */
    @After(value = "execution(* uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaConsumer.*(..)) && args(message)")
    public void afterItemOrderedProcessed(final Message message){
        LOGGER.info("ItemOrderedKafkaConsumer.processChdItemOrderedRetry() @After triggered");
        this.messagePayload = "" + message.getPayload();
        latch.countDown();
    }

    CountDownLatch getLatch() { return latch; }
    String getMessagePayload() { return messagePayload; }
    void setTestType(CHConsumerType type) { this.testType = type;}

    private void setUpTestKafkaItemOrderedProducerAndSendMessageToTopic()
            throws ExecutionException, InterruptedException, SerializationException {

        final ChdItemOrdered order = createOrder();
        if (this.testType == CHConsumerType.MAIN_CONSUMER) {
            kafkaProducer.sendMessage(createMessage(order, CHD_ITEM_ORDERED_TOPIC));
        } else if (this.testType == CHConsumerType.RETRY_CONSUMER) {
            kafkaProducer.sendMessage(createMessage(order, CHD_ITEM_ORDERED_TOPIC_RETRY));
        } else if (this.testType == CHConsumerType.ERROR_CONSUMER) {
            kafkaProducer.sendMessage(createMessage(order, CHD_ITEM_ORDERED_TOPIC_ERROR));
        }
    }

    public uk.gov.companieshouse.kafka.message.Message createMessage(final ChdItemOrdered order,
                                                                     final String topic) throws SerializationException {
        final uk.gov.companieshouse.kafka.message.Message message = new uk.gov.companieshouse.kafka.message.Message();
        final AvroSerializer<ChdItemOrdered> serializer =
                serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class);
        message.setKey(CHD_ITEM_ORDERED_KEY_RETRY);
        message.setValue(serializer.toBinary(order));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }
}
