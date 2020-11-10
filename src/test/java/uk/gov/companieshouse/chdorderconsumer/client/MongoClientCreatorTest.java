package uk.gov.companieshouse.chdorderconsumer.client;

import com.mongodb.MongoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.environment.EnvironmentReader;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MongoClientCreatorTest {

    @InjectMocks
    private MongoClientCreator mongoClientCreator;

    @Mock
    private EnvironmentReader mockEnvironmentReader;

    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";

    @Test
    @DisplayName("Mongo Client returned successfully")
    void testMongoClientReturnedSuccessfully() {
        when(mockEnvironmentReader.getMandatoryInteger(MONGO_PORT_NUMBER)).thenReturn(123456);
        when(mockEnvironmentReader.getMandatoryString(MONGO_CONNECTION_NAME)).thenReturn(MONGO_CONNECTION_NAME);

        MongoClient mongoClient = mongoClientCreator.createMongoClient();
        assertNotNull(mongoClient);
    }
}
