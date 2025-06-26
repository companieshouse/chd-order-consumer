package uk.gov.companieshouse.chdorderconsumer;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
class ChdOrderConsumerApplicationTests {

    private static final String CHS_API_KEY = "CHS_API_KEY";
    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";

    @BeforeEach
    void init() {
        System.setProperty(CHS_API_KEY, CHS_API_KEY);
        System.setProperty(MONGO_CONNECTION_NAME, MONGO_CONNECTION_NAME);
        System.setProperty(MONGO_DATABASE_NAME, MONGO_DATABASE_NAME);
        System.setProperty(MONGO_COLLECTION, MONGO_COLLECTION);
        System.setProperty(MONGO_PORT_NUMBER, MONGO_PORT_NUMBER);
        System.setProperty(ENTITY_ID_FIELD, ENTITY_ID_FIELD);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(CHS_API_KEY);
        System.clearProperty(MONGO_CONNECTION_NAME);
        System.clearProperty(MONGO_DATABASE_NAME);
        System.clearProperty(MONGO_COLLECTION);
        System.clearProperty(MONGO_PORT_NUMBER);
        System.clearProperty(ENTITY_ID_FIELD);
    }


    @Test
    void checkEnvironmentVariablesAllPresentReturnsTrue() {
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertTrue(isPresent);
    }

    @Test
    void checkEnvironmentVariableMongoConnectionNameReturnsFalse() {
        System.clearProperty(MONGO_CONNECTION_NAME);
        assertFalse(ChdOrderConsumerApplication.checkEnvironmentVariables());
    }

    @Test
    void checkEnvironmentVariableMongoDatabaseNameReturnsFalse() {
        System.clearProperty(MONGO_DATABASE_NAME);
        assertFalse(ChdOrderConsumerApplication.checkEnvironmentVariables());
    }

    @Test
    void checkEnvironmentVariableMongoCollectionReturnsFalse() {
        System.clearProperty(MONGO_COLLECTION);
        assertFalse(ChdOrderConsumerApplication.checkEnvironmentVariables());
    }

    @Test
    void checkEnvironmentVariableMongoPortNumberReturnsFalse() {
        System.clearProperty(MONGO_PORT_NUMBER);
        assertFalse(ChdOrderConsumerApplication.checkEnvironmentVariables());
    }

    @Test
    void checkEnvironmentVariableEntityIdReturnsFalse() {
        System.clearProperty(ENTITY_ID_FIELD);
        assertFalse(ChdOrderConsumerApplication.checkEnvironmentVariables());
    }


    @Test
    void contextLoads() {}

}
