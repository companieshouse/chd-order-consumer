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

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private static final String CHS_API_KEY = "CHS_API_KEY";
    private static final String MONGO_CONNECTION_NAME = "MONGO_CONNECTION_NAME";
    private static final String MONGO_DATABASE_NAME = "MONGO_DATABASE_NAME";
    private static final String MONGO_COLLECTION = "MONGO_COLLECTION";
    private static final String MONGO_PORT_NUMBER = "MONGO_PORT_NUMBER";
    private static final String ENTITY_ID_FIELD = "ENTITY_ID_FIELD";

    @BeforeEach
    void init() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(MONGO_CONNECTION_NAME, MONGO_CONNECTION_NAME);
        environmentVariables.set(MONGO_DATABASE_NAME, MONGO_DATABASE_NAME);
        environmentVariables.set(MONGO_COLLECTION, MONGO_COLLECTION);
        environmentVariables.set(MONGO_PORT_NUMBER, MONGO_PORT_NUMBER);
        environmentVariables.set(ENTITY_ID_FIELD, ENTITY_ID_FIELD);
    }

    @AfterEach
    void tearDown() {
        environmentVariables.clear(CHS_API_KEY);
        environmentVariables.clear(MONGO_CONNECTION_NAME);
        environmentVariables.clear(MONGO_DATABASE_NAME);
        environmentVariables.clear(MONGO_COLLECTION);
        environmentVariables.clear(MONGO_PORT_NUMBER);
        environmentVariables.clear(ENTITY_ID_FIELD);
    }

    @Test
    void checkEnvironmentVariablesAllPresentReturnsTrue() {
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertTrue(isPresent);
    }

    @Test
    void checkEnvironmentVariableCHSAPIKeyReturnsFalse() {
        environmentVariables.clear(CHS_API_KEY);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void checkEnvironmentVariableMongoConnectionNameReturnsFalse() {
        environmentVariables.clear(MONGO_CONNECTION_NAME);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void checkEnvironmentVariableMongoDatabaseNameReturnsFalse() {
        environmentVariables.clear(MONGO_DATABASE_NAME);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void checkEnvironmentVariableMongoCollectionReturnsFalse() {
        environmentVariables.clear(MONGO_COLLECTION);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void checkEnvironmentVariableMongoPortNumberReturnsFalse() {
        environmentVariables.clear(MONGO_PORT_NUMBER);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void checkEnvironmentVariableEntityIdReturnsFalse() {
        environmentVariables.clear(ENTITY_ID_FIELD);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void contextLoads() {}

}
