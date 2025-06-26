package uk.gov.companieshouse.chdorderconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.chdorderconsumer.environment.RequiredEnvironmentVariables;
import uk.gov.companieshouse.chdorderconsumer.logging.LoggingUtils;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.exception.EnvironmentVariableException;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;

@SpringBootApplication
public class ChdOrderConsumerApplication {

    private static final Logger LOGGER = LoggingUtils.getLogger();

    public static void main(String[] args) {
        if (checkEnvironmentVariables()) {
            SpringApplication.run(ChdOrderConsumerApplication.class, args);
        }
    }

    public static boolean checkEnvironmentVariables() {
        boolean allVariablesPresent = true;
        LOGGER.info("Checking all environment variables present");

        for (RequiredEnvironmentVariables param : RequiredEnvironmentVariables.values()) {
            String value = System.getenv(param.getName());
            if (value == null) {
                value = System.getProperty(param.getName()); // fallback for tests
            }

            if (value == null || value.isBlank()) {
                allVariablesPresent = false;
                LOGGER.error(String.format("Required config item %s missing", param.getName()));
            }
        }

        return allVariablesPresent;
    }

}
