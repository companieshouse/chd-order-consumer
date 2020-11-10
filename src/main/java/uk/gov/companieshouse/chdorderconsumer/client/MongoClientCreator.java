package uk.gov.companieshouse.chdorderconsumer.client;

import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.environment.EnvironmentReader;

@Component
public class MongoClientCreator {

    @Autowired
    private EnvironmentReader environmentReader;

    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";

    public MongoClient createMongoClient() {
        int mongoPortNumber = environmentReader
            .getMandatoryInteger(MONGO_PORT_NUMBER);
        String mongoConnectionName = environmentReader
            .getMandatoryString(MONGO_CONNECTION_NAME);
        return new MongoClient(mongoConnectionName, mongoPortNumber);
    }
}
