package uk.gov.companieshouse.chdorderconsumer.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String connectionString;

    @Bean
    public MongoClient mongoClient() {
        MongoClientURI mongoClientUri = new MongoClientURI(connectionString);
        return new MongoClient(mongoClientUri);
    }
}