package uk.gov.companieshouse.chdorderconsumer.config;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String connectionString;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(connectionString);
    }
}