package uk.gov.companieshouse.chdorderconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
class ChdOrderConsumerApiApplicationTests {

    @Test
    void contextLoads() {}

}
