package io.dockstore.webservice.helpers;

import com.auth0.jwt.exceptions.JWTVerificationException;

public class MissingScopesException extends JWTVerificationException {


    public MissingScopesException(String message) {
        super(message);
    }

    public MissingScopesException(String message, Throwable cause) {
        super(message, cause);
    }
}
