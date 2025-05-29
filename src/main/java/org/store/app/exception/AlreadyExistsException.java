package org.store.app.exception;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String message) {
        super(message);
    }

    public AlreadyExistsException(String entityName, String fieldName, String fieldValue) {
        super(String.format("%s with %s '%s' already exists.", entityName, fieldName, fieldValue));
    }
}
