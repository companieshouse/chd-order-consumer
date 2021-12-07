package uk.gov.companieshouse.chdorderconsumer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.environment.EnvironmentReader;

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

    @Mock
    private FindIterable<Document> findIterableMocked;

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String ID = "_id";
    private static final String TRANSACTION_ID = "MDEzNzQ1OTcyOGFkaXF6a2N4";
    private static final String COMPANY_FILING_HISTORY = "company_filing_history";
    private static final String ENTITY_ID = "_entity_id";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";
    private static final String ENTITY_ID_VALUE = "112233445";
    private static final String BARCODE = "barcode";
    private static final String BARCODE_VALUE = "001122334";

    @Test
    @DisplayName("Entity id returned successfully from mongo collection")
    void entityIdReturnedSuccessfully() {
        Document document = new Document();
        document.append(ID, TRANSACTION_ID);
        document.append(ENTITY_ID, ENTITY_ID_VALUE);

        doReturn(COMPANY_FILING_HISTORY).when(environmentReader)
            .getMandatoryString(MONGO_DATABASE_NAME);
        doReturn(COMPANY_FILING_HISTORY).when(environmentReader)
            .getMandatoryString(MONGO_COLLECTION);
        doReturn(ENTITY_ID).when(environmentReader)
            .getMandatoryString(ENTITY_ID_FIELD);

        when(mockMongoClient.getDatabase(anyString())).thenReturn(mockMongoDatabase);
        when(mockMongoDatabase.getCollection(anyString())).thenReturn(mockMongoCollection);
        when(mockMongoCollection.find(any(Bson.class))).thenReturn(findIterableMocked);
        when(findIterableMocked.projection(any(Bson.class))).thenReturn(findIterableMocked);
        when(findIterableMocked.first()).thenReturn(document);
        String entityId = mongoService.getEntityId(TRANSACTION_ID);
        assertEquals(ENTITY_ID_VALUE, entityId);
    }

    @Test
    @DisplayName("Barcode returned successfully from mongo collection")
    void BarcodeReturnedSuccessfully() {
        Document document = new Document();
        document.append(ID, TRANSACTION_ID);
        document.append(BARCODE, BARCODE_VALUE);

        doReturn(COMPANY_FILING_HISTORY).when(environmentReader)
            .getMandatoryString(MONGO_DATABASE_NAME);
        doReturn(COMPANY_FILING_HISTORY).when(environmentReader)
            .getMandatoryString(MONGO_COLLECTION);

        when(mockMongoClient.getDatabase(anyString())).thenReturn(mockMongoDatabase);
        when(mockMongoDatabase.getCollection(anyString())).thenReturn(mockMongoCollection);
        when(mockMongoCollection.find(any(Bson.class))).thenReturn(findIterableMocked);
        when(findIterableMocked.projection(any(Bson.class))).thenReturn(findIterableMocked);
        when(findIterableMocked.first()).thenReturn(document);
        String barcode = mongoService.getBarcode(TRANSACTION_ID);
        assertEquals(BARCODE_VALUE, barcode);
    }
}
