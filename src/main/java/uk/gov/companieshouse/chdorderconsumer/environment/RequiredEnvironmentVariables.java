package uk.gov.companieshouse.chdorderconsumer.environment;

public enum RequiredEnvironmentVariables {

    CHS_API_KEY("CHS_API_KEY");

    private String name;

    RequiredEnvironmentVariables(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
