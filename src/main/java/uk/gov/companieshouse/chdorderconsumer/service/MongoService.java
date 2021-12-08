package uk.gov.companieshouse.chdorderconsumer.service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.environment.EnvironmentReader;

@Service
public class MongoService {

    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";
    private static final String BARCODE_FIELD = "barcode";

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private EnvironmentReader environmentReader;

    public String getEntityId(String transactionId) {
        String mongoCollection = environmentReader
            .getMandatoryString(MONGO_COLLECTION);
        String entityIdField = environmentReader
            .getMandatoryString(ENTITY_ID_FIELD);

        MongoDatabase database = getDatabase();

        FindIterable<Document> documents = database.getCollection(mongoCollection)
            .find(Filters.eq(transactionId))
            .projection(Projections.include(entityIdField));
        Document document = documents.first();
        return (String) document.get(entityIdField);
    }

    public String getBarcode(String transactionId) {
        String mongoCollection = environmentReader
            .getMandatoryString(MONGO_COLLECTION);

        MongoDatabase database = getDatabase();

        FindIterable<Document> documents = database.getCollection(mongoCollection)
            .find(Filters.eq(transactionId))
            .projection(Projections.include(BARCODE_FIELD));
        Document document = documents.first();
        return (String) document.get(BARCODE_FIELD);
    }

    private MongoDatabase getDatabase() {
        String mongoDatabaseName = environmentReader
            .getMandatoryString(MONGO_DATABASE_NAME);

        return mongoClient.getDatabase(mongoDatabaseName);
    }
}
