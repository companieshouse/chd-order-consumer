package uk.gov.companieshouse.chdorderconsumer.kafka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static uk.gov.companieshouse.chdorderconsumer.util.TestUtils.assertJsonsEqualIgnoringFieldOrdering;
import static uk.gov.companieshouse.chdorderconsumer.util.TestUtils.createOrder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EmbeddedKafka
@TestPropertySource(properties={"uk.gov.companieshouse.chdorderconsumer.error-consumer=false"})
class ItemOrderedKafkaConsumerIntegrationDefaultModeTest {

    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String GROUP_NAME = "chd-item-ordered-consumers";

    @Value("${spring.kafka.bootstrap-servers}")
    private String brokerAddresses;
    @Autowired
    private SerializerFactory serializerFactory;
    @Autowired
    private ItemOrderedKafkaProducer kafkaProducer;

    private KafkaMessageListenerContainer<String, ChdItemOrdered> container;

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
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ChdItemOrderedDeserializer.class);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_NAME);
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddresses);

        final DefaultKafkaConsumerFactory<String, ChdItemOrdered> consumerFactory =
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
    @DirtiesContext
    @DisplayName("chd-item-ordered-error topic consumer does not receive message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    void testItemOrderedConsumerReceivesChdItemOrderedMessage1Error() throws Exception {
        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(createOrder(), CHD_ITEM_ORDERED_TOPIC_ERROR));

        // Then
        verifyProcessChdItemOrderedNotInvoked(CHConsumerType.ERROR_CONSUMER);
    }

    @Test
    @DirtiesContext
    @DisplayName("chd-item-ordered topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    void testItemOrderedConsumerReceivesChdItemOrderedMessage2() throws Exception {

        // Given
        final ChdItemOrdered order = createOrder();

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(order, CHD_ITEM_ORDERED_TOPIC));

        // Then
        verifyProcessChdItemOrderedInvoked(order, CHConsumerType.MAIN_CONSUMER);
    }

    @Test
    @DirtiesContext
    @DisplayName("chd-item-ordered topic consumer receives message when 'error-consumer' (env var IS_ERROR_QUEUE_CONSUMER) is false")
    void testItemOrderedConsumerReceivesChdItemOrderedMessage3Retry() throws Exception {

        // Given
        final ChdItemOrdered order = createOrder();

        // When
        kafkaProducer.sendMessage(consumerWrapper.createMessage(order, CHD_ITEM_ORDERED_TOPIC_RETRY));

        // Then
        verifyProcessChdItemOrderedInvoked(order, CHConsumerType.RETRY_CONSUMER);
    }

    private void verifyProcessChdItemOrderedNotInvoked(CHConsumerType type) throws InterruptedException {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(6000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(1L)));
        String processedOrderReference = consumerWrapper.getMessagePayload();
        assertThat(processedOrderReference, emptyOrNullString());
    }

    private void verifyProcessChdItemOrderedInvoked(final ChdItemOrdered order,
                                                    final CHConsumerType type) throws Exception {
        consumerWrapper.setTestType(type);
        consumerWrapper.getLatch().await(6000, TimeUnit.MILLISECONDS);
        assertThat(consumerWrapper.getLatch().getCount(), is(equalTo(0L)));
        final String messagePayload = consumerWrapper.getMessagePayload();
        assertJsonsEqualIgnoringFieldOrdering(messagePayload, order.toString());
    }

}
