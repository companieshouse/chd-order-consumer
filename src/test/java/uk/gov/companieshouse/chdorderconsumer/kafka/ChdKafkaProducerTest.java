package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.kafka.producer.factory.KafkaProducerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChdKafkaProducerTest {

    private static final int TEST_RETRIES = 5;
    private static final String TEST_BROKER = "test-broker";
    private static final String KEY = "test-key";
    private static final Long OFFSET = 10L;
    private static final int PARTITION = 3;
    private static final Long TIMESTAMP = 123456L;
    private static final String TOPIC = "test-topic";
    private static final String VALUE = "test-value";
    private static final int MAX_BLOCK_MILLISECONDS = 1000;
    private static final int REQUEST_TIMEOUT_MILLISECONDS = 1000;

    private CHKafkaProducer producer;
    private Message message;

    @Mock
    private KafkaProducer<String, byte[]> mockKafkaProducer;

    @Mock
    private Future<RecordMetadata> recordMetadataFuture;

    @Mock
    private KafkaProducerFactory mockProducerFactory;

    @Before
    public void test() {
        MockitoAnnotations.initMocks(this);
        createTestMessage();

        when(mockProducerFactory.getProducer(any(Properties.class))).thenReturn(mockKafkaProducer);
        given(mockKafkaProducer.send(any(ProducerRecord.class))).willReturn(recordMetadataFuture);
    }

    @Test
    public void testSendAndReturnFuture() throws Exception {

        createTestProducer(true, Acks.NO_RESPONSE);
        producer.sendAndReturnFuture(message);

        verify(mockKafkaProducer).send(any(ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRoundRobinAcksNoResponse() throws Exception {
        createTestProducer(true, Acks.NO_RESPONSE);
        producer.send(message);
        verify(mockKafkaProducer).send(any(ProducerRecord.class));
        verify(recordMetadataFuture, times(1)).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendManualPartitionAcksNoResponse() throws Exception {
        createTestProducer(false, Acks.NO_RESPONSE);
        producer.send(message);
        verify(mockKafkaProducer).send(any(ProducerRecord.class));
        verify(recordMetadataFuture, times(1)).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRoundRobinAcksWaitForLocal() throws Exception {
        createTestProducer(true, Acks.WAIT_FOR_LOCAL);
        producer.send(message);
        verify(mockKafkaProducer).send(any(ProducerRecord.class));
        verify(recordMetadataFuture, times(1)).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSendRoundRobinAcksWaitForAll() throws Exception {
        createTestProducer(true, Acks.WAIT_FOR_ALL);
        producer.send(message);
        verify(mockKafkaProducer).send(any(ProducerRecord.class));
        verify(recordMetadataFuture, times(1)).get();
    }

    @Test
    public void testCloseRoundRobinAcksNoResponse() throws Exception {
        createTestProducer(true, Acks.NO_RESPONSE);
        producer.close();
        verify(mockKafkaProducer).close();
    }

    /**
     * Create the test configuration and a producer to test
     *
     * @param roundRobinPartitioner
     * @param acks
     * @throws Exception
     */
    private void createTestProducer(boolean roundRobinPartitioner, Acks acks) throws Exception {
        mockApacheKafkaIntegration(roundRobinPartitioner, acks.getCode());

        ProducerConfig config = new ProducerConfig();
        config.setAcks(acks);
        config.setBrokerAddresses(new String[]{TEST_BROKER});
        config.setRetries(TEST_RETRIES);
        config.setRoundRobinPartitioner(roundRobinPartitioner);

        producer = new CHKafkaProducer(config, mockProducerFactory);
    }

    /**
     * Mock interactions with Apache Kafka.
     *
     * Powermock will expect the values in the Properties generated in the test class from the
     * ProducerConfig to match the values in the Properties used as the argument to mock the
     * Kafka producer.
     *
     * @param roundRobinPartitioner
     * @param acksCode
     * @throws Exception
     */
    private void mockApacheKafkaIntegration(boolean roundRobinPartitioner, String acksCode) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", String.join(",", new String[]{TEST_BROKER}));
        props.put("acks", acksCode);
        props.put("retries", TEST_RETRIES);
        props.put("value.serializer","org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        props.put("max.block.ms", MAX_BLOCK_MILLISECONDS);
        props.put("request.timeout.ms", REQUEST_TIMEOUT_MILLISECONDS);

        if(roundRobinPartitioner) {
            props.put("partition.assignment.strategy", "roundrobin");
        }
    }

    private void createTestMessage() {
        message = new Message();
        message.setKey(KEY);
        message.setOffset(OFFSET);
        message.setPartition(PARTITION);
        message.setTimestamp(TIMESTAMP);
        message.setTopic(TOPIC);
        message.setValue(VALUE.getBytes());
    }

}
