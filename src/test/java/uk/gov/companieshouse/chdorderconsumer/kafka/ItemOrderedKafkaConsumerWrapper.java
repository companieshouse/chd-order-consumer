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
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils.APPLICATION_NAMESPACE;

@DirtiesContext
@Aspect
@Service
public class ItemOrderedKafkaConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;
    private CHConsumerType testType = CHConsumerType.MAIN_CONSUMER;
    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;
    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String ORDER_RECEIVED_URI = "/order/ORDER-12345";
    private static final String CHD_ITEM_ORDERED_KEY_RETRY = CHD_ITEM_ORDERED_TOPIC_RETRY;
    @Autowired
    private ItemOrderedKafkaProducer ordersKafkaProducer;
    @Autowired
    private ItemOrderedKafkaConsumer ordersKafkaConsumer;
    @Autowired
    private SerializerFactory serializerFactory;

    /**
     * mock message processing failure scenario for main listener
     * so that the message can be published to alternate topics '-retry' and '-error'
     * @param message
     * @throws Exception
     */
    @Before(value = "execution(* uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaConsumer.processOrderReceived(..)) && args(message)")
    public void beforeOrderProcessed(final Message message) throws Exception {
        LOGGER.info("ItemOrderedKafkaConsumer.processOrderReceived() @Before triggered");
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
        LOGGER.info("ItemOrderedKafkaConsumer.processOrderReceived() @AfterThrowing triggered");

        setUpTestKafkaOrdersProducerAndSendMessageToTopic();
    }

    /**
     * emulates receiving of message from kafka topics
     * @param message
     */
    @After(value = "execution(* uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaConsumer.*(..)) && args(message)")
    public void afterOrderProcessed(final Message message){
        LOGGER.info("ItemOrderedKafkaConsumer.processOrderReceivedRetry() @After triggered");
        this.orderUri = "" + message.getPayload();
        latch.countDown();
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
    void setTestType(CHConsumerType type) { this.testType = type;}

    private void setUpTestKafkaOrdersProducerAndSendMessageToTopic()
            throws ExecutionException, InterruptedException, SerializationException {

        if (this.testType == CHConsumerType.MAIN_CONSUMER) {
            ordersKafkaProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC));
        } else if (this.testType == CHConsumerType.RETRY_CONSUMER) {
            ordersKafkaProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC_RETRY));
        } else if (this.testType == CHConsumerType.ERROR_CONSUMER) {
            ordersKafkaProducer.sendMessage(createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC_ERROR));
        }
    }

    public uk.gov.companieshouse.kafka.message.Message createMessage(String orderUri, String topic) throws SerializationException {
        final uk.gov.companieshouse.kafka.message.Message message = new uk.gov.companieshouse.kafka.message.Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey(CHD_ITEM_ORDERED_KEY_RETRY);
        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }
}
