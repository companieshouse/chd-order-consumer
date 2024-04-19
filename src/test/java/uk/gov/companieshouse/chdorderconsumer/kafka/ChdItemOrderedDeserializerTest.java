package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChdItemOrderedDeserializerTest {
    @InjectMocks
    private ChdItemOrderedDeserializer deserializer;

    @Test
    void deserializeThrowsSerializationException() {
        byte[] testData = "Test data".getBytes();

        Assertions.assertThrows(SerializationException.class, () -> deserializer.deserialize("chd-item-ordered", testData));
    }
}
