package uk.gov.companieshouse.chdorderconsumer.kafka;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class ChdItemOrderedDeserializerTest {
    @InjectMocks
    private ChdItemOrderedDeserializer deserializer;
    @Mock
    private BinaryDecoder binaryDecoder;
    @Mock
    private DatumReader<ChdItemOrdered> datumReader;

    @Test
    void deserializeThrowsSerializationException() throws IOException {
        byte[] testData = new String("Test data").getBytes();

        Assertions.assertThrows(SerializationException.class, () -> deserializer.deserialize("chd-item-ordered", testData));
    }
}
