package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.producer.Acks;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class KafkaProducerTest {
    private static final String EXPECTED_CONFIG_ERROR_MESSAGE =
            "Broker addresses for kafka broker missing, check if environment variable KAFKA_BROKER_ADDR is configured. " +
                    "[Hint: The property 'kafka.broker.addresses' uses the value of this environment variable in live " +
                    "environments and that of 'spring.embedded.kafka.brokers' property in test.]";

    @InjectMocks
    private TestKafkaProducer kafkaProducerUnderTest;

    @Mock
    private ChdKafkaProducer chKafkaProducer;

    @Mock
    private ProducerConfig producerConfig;

    /**
     * Extends {@link KafkaProducer} to provide a concrete implementation for testing.
     */
    private static class TestKafkaProducer extends KafkaProducer {}

    /**
     * Extends {@link KafkaProducer} to provide a concrete implementation for testing that allows us to stub out
     * unwanted behaviour and verify the behaviour of interest.
     */
    private class TestKafkaProducer2 extends KafkaProducer {
        private boolean modifyProducerConfigCalled;
        private boolean createProducerConfigCalled;
        private boolean createChKafkaProducerCalled;

        public boolean isModifyProducerConfigCalled() {
            return modifyProducerConfigCalled;
        }

        public boolean isCreateProducerConfigCalled() {
            return createProducerConfigCalled;
        }

        public boolean isCreateChKafkaProducerCalled() {
            return createChKafkaProducerCalled;
        }

        @Override
        protected void modifyProducerConfig(ProducerConfig producerConfig) {
            modifyProducerConfigCalled = true;
        }

        @Override
        protected ProducerConfig createProducerConfig() {
            createProducerConfigCalled = true;
            return producerConfig;
        }

        @Override
        protected ChdKafkaProducer createChKafkaProducer(final ProducerConfig config) {
            createChKafkaProducerCalled = true;
            return chKafkaProducer;
        }
    }

    @Test
    @DisplayName("afterPropertiesSet() throws a ProducerConfigException if no spring.kafka.bootstrap-servers value configured")
    void afterPropertiesSetThrowsExceptionIfNoBrokersConfigured() {

        // When
        ProducerConfigException exception = Assertions.assertThrows(ProducerConfigException.class, () ->
                kafkaProducerUnderTest.afterPropertiesSet());
        // Then
        final String actualMessage = exception.getMessage();
        assertThat(actualMessage, is(EXPECTED_CONFIG_ERROR_MESSAGE));

    }

    @Test
    @DisplayName("afterPropertiesSet() calls template methods")
    void afterPropertiesSetCallsTemplateMethods() {

        // Given
        final TestKafkaProducer2 kafkaProducerUnderTest = new TestKafkaProducer2();

        // When
        kafkaProducerUnderTest.afterPropertiesSet();

        // Then
        assertThat(kafkaProducerUnderTest.isModifyProducerConfigCalled(), is(true));
        assertThat(kafkaProducerUnderTest.isCreateProducerConfigCalled(), is(true));
        assertThat(kafkaProducerUnderTest.isCreateChKafkaProducerCalled(), is(true));

    }

    @Test
    @DisplayName("afterPropertiesSet() sets the producer's kafka producer member")
    void afterPropertiesSetSetsProducerMember() {

        // Given
        final TestKafkaProducer2 kafkaProducerUnderTest = new TestKafkaProducer2();

        // When
        kafkaProducerUnderTest.afterPropertiesSet();

        // Then
        assertThat(kafkaProducerUnderTest.getChKafkaProducer(), is(notNullValue()));

    }

    @Test
    @DisplayName("afterPropertiesSet() sets producer config properties")
    void afterPropertiesSetSetsProducerConfigProperties() {

        // Given
        final TestKafkaProducer2 kafkaProducerUnderTest = new TestKafkaProducer2();

        // When
        kafkaProducerUnderTest.afterPropertiesSet();

        // Then
        verify(producerConfig).setRoundRobinPartitioner(true);
        verify(producerConfig).setAcks(Acks.WAIT_FOR_ALL);
        verify(producerConfig).setRetries(10);

    }
}
