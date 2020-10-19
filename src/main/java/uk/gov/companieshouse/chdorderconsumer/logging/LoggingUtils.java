package uk.gov.companieshouse.chdorderconsumer.logging;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.HashMap;
import java.util.Map;

public class LoggingUtils {

    private LoggingUtils() {}

    public static final String APPLICATION_NAMESPACE = "chd-order-consumer";
    public static final String TOPIC = "topic";
    public static final String OFFSET = "offset";
    public static final String KEY = "key";
    public static final String PARTITION = "partition";
    public static final String ORDER_URI = "order_uri";
    public static final String CURRENT_TOPIC = "current_topic";
    public static final String NEXT_TOPIC = "next_topic";
    public static final String MESSAGE = "message";
    public static final String RETRY_ATTEMPT = "retry_attempt";
    public static final String CHD_ITEM_ORDERED_GROUP_ERROR = "chd_item_ordered_error";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Map<String, Object> createLogMap() {
        return new HashMap<>();
    }

    public static Map<String, Object> getMessageHeadersAsMap(
            org.springframework.messaging.Message<OrderReceived> message) {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        MessageHeaders messageHeaders = message.getHeaders();

        logIfNotNull(logMap, KEY, messageHeaders.get(KafkaHeaders.RECEIVED_MESSAGE_KEY));
        logIfNotNull(logMap, TOPIC, messageHeaders.get(KafkaHeaders.RECEIVED_TOPIC));
        logIfNotNull(logMap, OFFSET, messageHeaders.get(KafkaHeaders.OFFSET));
        logIfNotNull(logMap, PARTITION, messageHeaders.get(KafkaHeaders.RECEIVED_PARTITION_ID));

        return logMap;
    }

    public static Map<String, Object> createLogMapWithKafkaMessage(Message message) {
        Map<String, Object> logMap = createLogMap();
        logIfNotNull(logMap, TOPIC, message.getTopic());
        logIfNotNull(logMap, PARTITION, message.getPartition());
        logIfNotNull(logMap, OFFSET, message.getOffset());
        return logMap;
    }

    public static void logIfNotNull(Map<String, Object> logMap, String key, Object loggingObject) {
        if (loggingObject != null) {
            logMap.put(key, loggingObject);
        }
    }
}
