package uk.gov.companieshouse.chdorderconsumer.exception;

public class DuplicateErrorException extends RuntimeException {

    public DuplicateErrorException(String message) {
        super(message);
    }

}
