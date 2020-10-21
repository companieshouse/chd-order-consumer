package uk.gov.companieshouse.chdorderconsumer;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
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

    @Test
    void checkEnvironmentVariablesAllPresentReturnsTrue() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertTrue(isPresent);

        environmentVariables.clear(CHS_API_KEY);
    }

    @Test
    void checkEnvironmentVariableCHSAPIKeytReturnsTrue() {
        boolean isPresent = ChdOrderConsumerApplication.checkEnvironmentVariables();
        assertFalse(isPresent);
    }

    @Test
    void contextLoads() {}

}
