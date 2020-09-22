package uk.gov.companieshouse.chdorderconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChdOrderConsumerApplication {

	public static final String APPLICATION_NAMESPACE = "chd-order-consumer";

	public static void main(String[] args) {
		SpringApplication.run(ChdOrderConsumerApplication.class, args);
	}

}
