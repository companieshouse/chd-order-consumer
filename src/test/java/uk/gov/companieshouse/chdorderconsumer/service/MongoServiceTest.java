package uk.gov.companieshouse.chdorderconsumer.service;

import com.mongodb.MongoClient;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.Document;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.environment.EnvironmentReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MongoServiceTest {

    @InjectMocks
    private MongoService mongoService;

    @Mock
    private EnvironmentReader environmentReader;

    @Mock
    private MongoClient mockMongoClient;

    @Mock
    private MongoDatabase mockMongoDatabase;

    @Mock
    private MongoCollection mockMongoCollection;

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";

    private static final String TRANSACTION_ID = "MDEzNzQ1OTcyOGFkaXF6a2N4";

    @Test
    @DisplayName("Entity id returned successfully from mongo collection")
    void entityIdReturnedSuccessfully() {

        final List<Document> documentsMocked = new ArrayList<>();
        documentsMocked.add(new Document("_id", "MDEzNzQ1OTcyOGFkaXF6a2N4"));
        documentsMocked.add(new Document("_entity_id", "112233445"));

        doReturn(MONGO_CONNECTION_NAME).when(environmentReader)
            .getMandatoryString(MONGO_CONNECTION_NAME);
        doReturn("company_filing_history").when(environmentReader)
            .getMandatoryString(MONGO_DATABASE_NAME);
        doReturn("company_filing_history").when(environmentReader)
            .getMandatoryString(MONGO_COLLECTION);
        doReturn(12345678).when(environmentReader)
            .getMandatoryInteger(MONGO_PORT_NUMBER);
        doReturn("_entity_id").when(environmentReader)
            .getMandatoryString(ENTITY_ID_FIELD);

        when(mockMongoClient.getDatabase(anyString())).thenReturn(mockMongoDatabase);
        when(mockMongoDatabase.getCollection(anyString())).thenReturn(mockMongoCollection);
        doThrow(new MongoTimeoutException("Mongo connection timeout - unable to return _entity_id")).when(mockMongoCollection)z

        FindIterable<Document> findIterableMocked =
            (FindIterable<Document>) Mockito.mock(FindIterable.class);

        when(mockMongoCollection.find(any(Bson.class))).thenReturn(findIterableMocked);

//        when(findIterableMocked.iterator()).thenReturn(documentsMocked.iterator());
//
//        Mockito.when(mockMongoCollection.find()).thenReturn(findIterableMocked);

        String entityId = mongoService.getEntityId(TRANSACTION_ID);
        assertEquals("112233445", entityId);
    }
}
