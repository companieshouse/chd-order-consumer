package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.chdorderconsumer.util.TestConstants.ORDER_REFERENCE;
import static uk.gov.companieshouse.chdorderconsumer.util.TestUtils.createOrder;

@ExtendWith(MockitoExtension.class)
class ItemOrderedKafkaConsumerTest {
    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_KEY = "chd-item-ordered";
    private static final String CHD_ITEM_ORDERED_TOPIC_RETRY = "chd-item-ordered-retry";
    private static final String CHD_ITEM_ORDERED_TOPIC_ERROR = "chd-item-ordered-error";
    private static final String PROCESSING_ERROR_MESSAGE = "Order processing failed.";

    @Spy
    @InjectMocks
    private ItemOrderedKafkaConsumer kafkaConsumer;
    @Mock
    private ItemOrderedKafkaProducer kafkaProducer;
    @Mock
    private SerializerFactory serializerFactory;
    @Mock
    private AvroSerializer serializer;
    @Captor
    ArgumentCaptor<String> orderReferenceArgument;
    @Captor
    ArgumentCaptor<String> currentTopicArgument;
    @Captor
    ArgumentCaptor<String> nextTopicArgument;

    @Test
    void createRetryMessageBuildsMessageSuccessfully() {
        // Given
        final ItemOrderedKafkaConsumer consumerUnderTest =
                new ItemOrderedKafkaConsumer(new SerializerFactory(),
                        new ItemOrderedKafkaProducer(),
                        new KafkaListenerEndpointRegistry());
        final ChdItemOrdered originalOrder = createOrder();

        // When
        final Message retryMessage =
                consumerUnderTest.createRetryMessage(originalOrder, ORDER_REFERENCE, CHD_ITEM_ORDERED_TOPIC);

        // Then
        final byte[] retryMessageRawValue = retryMessage.getValue();
        final ChdItemOrderedDeserializer deserializer = new ChdItemOrderedDeserializer();
        final ChdItemOrdered deserializedOrderFromRetryMessage =
                (ChdItemOrdered) deserializer.deserialize(CHD_ITEM_ORDERED_TOPIC, retryMessageRawValue);
        assertThat(deserializedOrderFromRetryMessage, is(originalOrder));
    }

    @Test
    void republishMessageToRetryTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        kafkaConsumer.republishMessageToTopic(createOrder(),
                                              ORDER_REFERENCE,
                                              CHD_ITEM_ORDERED_TOPIC,
                                              CHD_ITEM_ORDERED_TOPIC_RETRY);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    void republishMessageToRetryTopicThrowsSerializationException()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenThrow(SerializationException.class);
        kafkaConsumer.republishMessageToTopic(createOrder(),
                                              ORDER_REFERENCE,
                                              CHD_ITEM_ORDERED_TOPIC,
                                              CHD_ITEM_ORDERED_TOPIC_RETRY);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    void republishMessageToErrorTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        kafkaConsumer.republishMessageToTopic(createOrder(),
                                              ORDER_REFERENCE,
                                              CHD_ITEM_ORDERED_TOPIC_RETRY,
                                              CHD_ITEM_ORDERED_TOPIC_ERROR);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    void republishMessageSuccessfullyCalledForFirstMainMessageOnRetryableErrorException()
            throws SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC));
        // Then
        verify(kafkaConsumer, times(1)).republishMessageToTopic(any(ChdItemOrdered.class), orderReferenceArgument.capture(),
                currentTopicArgument.capture(), nextTopicArgument.capture());
        Assert.assertEquals(ORDER_REFERENCE, orderReferenceArgument.getValue());
        Assert.assertEquals(CHD_ITEM_ORDERED_TOPIC, currentTopicArgument.getValue());
        Assert.assertEquals(CHD_ITEM_ORDERED_TOPIC_RETRY, nextTopicArgument.getValue());
    }

    @Test
    void republishMessageNotCalledForFirstRetryMessageOnRetryableErrorException() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).doNothing().when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC_RETRY));
        // Then
        verify(kafkaConsumer, times(0)).republishMessageToTopic(any(ChdItemOrdered.class), anyString(), anyString(), anyString());
    }

    @Test
    void republishMessageNotCalledForFirstErrorMessageOnRetryableErrorException() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).doNothing().when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC_ERROR));
        // Then
        verify(kafkaConsumer, times(0)).republishMessageToTopic(any(ChdItemOrdered.class), anyString(), anyString(), anyString());
    }

    @Test
    void mainListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrdered(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrdered(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrdered(any());
    }

    @Test
    void retryListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrderedRetry(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrderedRetry(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrderedRetry(any());
    }

    @Test
    void errorListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrderedError(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrderedError(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        assertThat(actualMessage, is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrderedError(any());
    }

    private static org.springframework.messaging.Message createTestMessage(String receivedTopic) {
        return new org.springframework.messaging.Message<ChdItemOrdered>() {
            @Override
            public ChdItemOrdered getPayload() {
                return createOrder();
            }

            @Override
            public MessageHeaders getHeaders() {
                Map<String, Object> headerItems = new HashMap<>();
                headerItems.put("kafka_receivedTopic", receivedTopic);
                headerItems.put("kafka_offset", 0);
                headerItems.put("kafka_receivedMessageKey", CHD_ITEM_ORDERED_KEY);
                headerItems.put("kafka_receivedPartitionId", 0);
                MessageHeaders headers = new MessageHeaders(headerItems);
                return headers;
            }
        };
    }
}
