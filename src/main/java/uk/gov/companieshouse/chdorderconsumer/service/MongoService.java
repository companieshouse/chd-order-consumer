package uk.gov.companieshouse.chdorderconsumer.service;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.chdorderconsumer.client.MongoClientCreator;
import uk.gov.companieshouse.environment.EnvironmentReader;

@Service
public class MongoService {

    private MongoClient mongoClient;
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";

    @Autowired
    private EnvironmentReader environmentReader;

    @Autowired
    private MongoClientCreator mongoClientCreator;

    public String getEntityId(String transactionId) {
        String mongoDatabaseName = environmentReader
            .getMandatoryString(MONGO_DATABASE_NAME);
        String mongoCollection = environmentReader
            .getMandatoryString(MONGO_COLLECTION);
        String entityIdField = environmentReader
            .getMandatoryString(ENTITY_ID_FIELD);

        if (mongoClient == null) {
            mongoClient = mongoClientCreator.createMongoClient();
        }

        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        FindIterable<Document> documents = database.getCollection(mongoCollection)
            .find(Filters.eq(transactionId))
            .projection(Projections.include(entityIdField));
        Document document = documents.first();
        String entityIdValue = (String) document.get(entityIdField);
        mongoClient.close();
        return entityIdValue;
    }
}
