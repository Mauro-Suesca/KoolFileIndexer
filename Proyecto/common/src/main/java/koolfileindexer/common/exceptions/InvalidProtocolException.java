package koolfileindexer.common.exceptions;

import java.io.IOException;

public class InvalidProtocolException extends Exception {
    public InvalidProtocolException() {
        super("InvalidProtocolError: Is this kfi v1?");
    }

    public InvalidProtocolException(String message) {
        super(message);
    }

    public InvalidProtocolException(IOException e) {
        super("InvalidProtocolError: Got unexpected IOError: " + e.getMessage());
    }
}
