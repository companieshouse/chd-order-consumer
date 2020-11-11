package uk.gov.companieshouse.chdorderconsumer.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.mongodb.MongoClient;
@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String connectionString;

    @Value("${spring.data.mongodb.port}")
    private int connectionPort;

    @Bean
    public MongoClient mongoClient() {
        MongoClient mongoClient = new MongoClient(connectionString, connectionPort);
        return mongoClient;
    }
}