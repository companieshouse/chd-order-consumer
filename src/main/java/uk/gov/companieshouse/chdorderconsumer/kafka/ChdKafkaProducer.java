package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;
import uk.gov.companieshouse.kafka.producer.factory.KafkaProducerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChdKafkaProducer {

    private KafkaProducer<String, byte[]> kafkaProducer;

    /**
     * Instantiate the necessary configuration for producing and a producer for Kafka.
     * The {@link KafkaProducerFactory} will be used to create the producer.
     *
     * @param config Configuration for the producer
     */
    public ChdKafkaProducer(ProducerConfig config) {
        this(config, new KafkaProducerFactory());
    }

    /**
     * Instantiate the necessary configuration for producing and a producer for Kafka.
     *
     * @param config Configuration for the producer
     * @param producerFactory Factory to create the producer
     */
    public ChdKafkaProducer(ProducerConfig config, KafkaProducerFactory producerFactory) {
        Properties props = new Properties();

        props.put("bootstrap.servers", String.join(",", config.getBrokerAddresses()));
        props.put("acks", config.getAcks().getCode());
        props.put("key.serializer", config.getKeySerializer());
        props.put("value.serializer", config.getValueSerializer());
        props.put("retries", config.getRetries());
        props.put("max.block.ms", config.getMaxBlockMilliseconds());
        props.put("request.timeout.ms", config.getRequestTimeoutMilliseconds());
        props.put("enable.idempotence", false);

        if (config.isRoundRobinPartitioner()) {
            props.put("partition.assignment.strategy", "roundrobin");
        }

        kafkaProducer = producerFactory.getProducer(props);
    }

    /**
     * Send a message to a topic in Kafka.
     *
     * The data in the message is sent to Kafka in a byte array
     * to avoid unrecognised encoding characters which can occur
     * when Strings are used with Kafka.
     *
     */
    public void send(Message msg) throws ExecutionException, InterruptedException {

        ProducerRecord<String, byte[]> record = getProducerRecordFromMessage(msg);

        kafkaProducer.send(record).get();
    }

    /**
     * Send a message to a topic in Kafka, and return a {@link Future < RecordMetadata >} for processing manually.
     *
     * The data in the message is sent to Kafka in a byte array
     * to avoid unrecognised encoding characters which can occur
     * when Strings are used with Kafka.
     *
     */
    public Future<RecordMetadata> sendAndReturnFuture(Message msg) {

        ProducerRecord<String, byte[]> record = getProducerRecordFromMessage(msg);

        return kafkaProducer.send(record);
    }

    private ProducerRecord<String, byte[]> getProducerRecordFromMessage(Message msg) {

        return new ProducerRecord<>(
                msg.getTopic(),
                msg.getPartition(),
                msg.getTimestamp(),
                msg.getKey(),
                msg.getValue()
        );
    }

    public void close() {
        kafkaProducer.close();
    }
}
