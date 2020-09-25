package uk.gov.companieshouse.chdorderconsumer.logging;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class LoggingUtils {

    private LoggingUtils() {}

    public static final String APPLICATION_NAMESPACE = "chd-order-consumer";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public static Logger getLogger() {
        return LOGGER;
    }
}
