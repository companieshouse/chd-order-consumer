package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.kafka.consumer.resilience.CHConsumerType;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.chdorderconsumer.error-consumer=true"})
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class ItemOrderedKafkaConsumerIntegrationErrorModeTest {

    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String CONSUMER_GROUP_MAIN_RETRY = "chd-item-ordered-main-retry";
    private static final String ORDER_RECEIVED_URI = "/order/ORD-123456-123456";
    private static final String ORDER_RECEIVED_MESSAGE_JSON = "{\"order_uri\": \"/order/ORD-123456-123456\"}";

    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;
    @Autowired
    private SerializerFactory serializerFactory;
    @Autowired
    private ItemOrderedKafkaProducer kafkaProducer;

    private KafkaMessageListenerContainer<String, String> container;

    private BlockingQueue<ConsumerRecord<String, String>> records;

    @Autowired
    private ItemOrderedKafkaConsumerWrapper consumerWrapper;

    @BeforeEach
    public void setUp() {
        setUpTestKafkaItemOrderedConsumer();
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }

    private void setUpTestKafkaItemOrderedConsumer() {
        final Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_MAIN_RETRY);
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);

        final DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        final ContainerProperties containerProperties = new ContainerProperties(
                new String[]{CHD_ITEM_ORDERED_TOPIC, CHD_ITEM_ORDERED_TOPIC_RETRY, CHD_ITEM_ORDERED_TOPIC_ERROR});

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        records = new LinkedBlockingQueue<>();

        container.setupMessageListener((MessageListener<String, String>) record -> {
            records.add(record);
        });

        container.start();

        ContainerTestUtils.waitForAssignment(container, 0);
    }

    @Test
    @DisplayName("chd-item-ordered topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    void testItemOrderedConsumerReceivesOrderReceivedMessage1() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DisplayName("chd-item-ordered-retry topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER)is true")
    void testItemOrderedConsumerReceivesOrderReceivedMessage2Retry() throws InterruptedException, SerializationException, ExecutionException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC_RETRY));

        // Then
        verifyProcessOrderReceivedNotInvoked(CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessOrderReceivedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(3000, TimeUnit.MILLISECONDS);
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, isEmptyOrNullString());
    }

    @Test
    @DisplayName("chd-item-ordered-error topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is true")
    void testItemOrderedConsumerReceivesOrderReceivedMessage3Error() throws InterruptedException, ExecutionException, SerializationException {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC_ERROR));

        // Then
        verifyProcessOrderReceivedInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    private void verifyProcessOrderReceivedInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(6000, TimeUnit.MILLISECONDS);
        final String processedOrderUri = consumerWrapper.getOrderUri();
        assertThat(processedOrderUri, is(equalTo(ORDER_RECEIVED_MESSAGE_JSON)));
    }
}
