package io.dockstore.webservice.helpers;

public class OidcClientException extends RuntimeException {

    public OidcClientException(String message) {
        super(message);
    }

    public OidcClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
