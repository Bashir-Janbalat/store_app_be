package org.store.app.exception;
import org.springframework.security.core.AuthenticationException;

public class EmailNotVerifiedException extends AuthenticationException  {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}