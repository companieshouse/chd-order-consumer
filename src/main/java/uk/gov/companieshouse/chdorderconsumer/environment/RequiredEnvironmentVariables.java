package uk.gov.companieshouse.chdorderconsumer.environment;

public enum RequiredEnvironmentVariables {

    CHS_API_KEY("CHS_API_KEY"),
    MONGO_CONNECTION_NAME("MONGO_CONNECTION_NAME"),
    MONGO_DATABASE_NAME("MONGO_DATABASE_NAME"),
    MONGO_COLLECTION("MONGO_COLLECTION"),
    MONGO_PORT_NUMBER("MONGO_PORT_NUMBER"),
    ENTITY_ID_FIELD("ENTITY_ID_FIELD");

    private final String name;

    RequiredEnvironmentVariables(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
