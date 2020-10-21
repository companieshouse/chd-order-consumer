package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.hamcrest.Matchers;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ItemOrderedKafkaConsumerTest {
    private static final String ORDER_RECEIVED_URI = "/orders/ORD-123456-123456";
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
    ArgumentCaptor<String> orderUriArgument;
    @Captor
    ArgumentCaptor<String> currentTopicArgument;
    @Captor
    ArgumentCaptor<String> nextTopicArgument;

    @Test
    public void createRetryMessageBuildsMessageSuccessfully() {
        // Given & When
        ItemOrderedKafkaConsumer consumerUnderTest =
                new ItemOrderedKafkaConsumer(new SerializerFactory(),
                        new ItemOrderedKafkaProducer(),
                        new KafkaListenerEndpointRegistry());
        Message actualMessage = consumerUnderTest.createRetryMessage(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC);
        byte[] actualMessageRawValue    = actualMessage.getValue();
        // Then
        ChdItemOrderedDeserializer deserializer = new ChdItemOrderedDeserializer();
        String actualChdItemOrdered = (String) deserializer.deserialize(CHD_ITEM_ORDERED_TOPIC, actualMessageRawValue).get(0);
        Assert.assertThat(actualChdItemOrdered, Matchers.is(ORDER_RECEIVED_URI));
    }

    @Test
    public void republishMessageToRetryTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        kafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC, CHD_ITEM_ORDERED_TOPIC_RETRY);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageToRetryTopicThrowsSerializationException()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenThrow(SerializationException.class);
        kafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC, CHD_ITEM_ORDERED_TOPIC_RETRY);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageToErrorTopicRunsSuccessfully()
            throws ExecutionException, InterruptedException, SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        kafkaConsumer.republishMessageToTopic(ORDER_RECEIVED_URI, CHD_ITEM_ORDERED_TOPIC_RETRY, CHD_ITEM_ORDERED_TOPIC_ERROR);
        // Then
        verify(kafkaProducer, times(1)).sendMessage(any());
    }

    @Test
    public void republishMessageSuccessfullyCalledForFirstMainMessageOnRetryableErrorException()
            throws SerializationException {
        // Given & When
        when(serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class)).thenReturn(serializer);
        when(serializer.toBinary(any())).thenReturn(new byte[4]);
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC));
        // Then
        verify(kafkaConsumer, times(1)).republishMessageToTopic(orderUriArgument.capture(),
                currentTopicArgument.capture(), nextTopicArgument.capture());
        Assert.assertEquals(ORDER_RECEIVED_URI, orderUriArgument.getValue());
        Assert.assertEquals(CHD_ITEM_ORDERED_TOPIC, currentTopicArgument.getValue());
        Assert.assertEquals(CHD_ITEM_ORDERED_TOPIC_RETRY, nextTopicArgument.getValue());
    }

    @Test
    public void republishMessageNotCalledForFirstRetryMessageOnRetryableErrorException() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).doNothing().when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC_RETRY));
        // Then
        verify(kafkaConsumer, times(0)).republishMessageToTopic(anyString(), anyString(), anyString());
    }

    @Test
    public void republishMessageNotCalledForFirstErrorMessageOnRetryableErrorException() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).doNothing().when(kafkaConsumer).logMessageReceived(any(), any());
        kafkaConsumer.handleMessage(createTestMessage(CHD_ITEM_ORDERED_TOPIC_ERROR));
        // Then
        verify(kafkaConsumer, times(0)).republishMessageToTopic(anyString(), anyString(), anyString());
    }

    @Test
    public void mainListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrdered(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrdered(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrdered(any());
    }

    @Test
    public void retryListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrderedRetry(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrderedRetry(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrderedRetry(any());
    }

    @Test
    public void errorListenerExceptionIsCorrectlyHandled() {
        // Given & When
        doThrow(new RetryableErrorException(PROCESSING_ERROR_MESSAGE)).when(kafkaConsumer).processChdItemOrderedError(any());
        RetryableErrorException exception = Assertions.assertThrows(RetryableErrorException.class, () -> {
            kafkaConsumer.processChdItemOrderedError(any());
        });
        // Then
        String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(PROCESSING_ERROR_MESSAGE));
        verify(kafkaConsumer, times(1)).processChdItemOrderedError(any());
    }

    private static org.springframework.messaging.Message createTestMessage(String receivedTopic) {
        return new org.springframework.messaging.Message<ChdItemOrdered>() {
            @Override
            public ChdItemOrdered getPayload() {
                ChdItemOrdered chdItemOrdered = new ChdItemOrdered();

                // TODO GCI-1594 deal with ChdItemOrdered attributes.
                //chdItemOrdered.setOrderUri(ORDER_RECEIVED_URI);
                chdItemOrdered.setReference(ORDER_RECEIVED_URI);

                return chdItemOrdered;
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
