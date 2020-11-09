package uk.gov.companieshouse.chdorderconsumer.service;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.environment.EnvironmentReader;

@Service
public class MongoService {

    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";

    @Autowired
    private EnvironmentReader environmentReader;

    public String getEntityId(String transactionId) {

        String mongoConnectionName = environmentReader
            .getMandatoryString(MONGO_CONNECTION_NAME);
        String mongoDatabaseName = environmentReader
            .getMandatoryString(MONGO_DATABASE_NAME);
        String mongoCollection = environmentReader
            .getMandatoryString(MONGO_COLLECTION);
        int mongoPortNumber = environmentReader
            .getMandatoryInteger(MONGO_PORT_NUMBER);
        String entityIdField = environmentReader
            .getMandatoryString(ENTITY_ID_FIELD);

        MongoClient mongoClient = new MongoClient(mongoConnectionName, mongoPortNumber);
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        FindIterable<Document> documents = database.getCollection(mongoCollection).find(Filters.eq(transactionId))
            .projection(Projections.include(entityIdField));

        Document document = documents.first();

        String enitityIdValue = (String)document.get(entityIdField);

        mongoClient.close();

        return enitityIdValue;
    }
}
