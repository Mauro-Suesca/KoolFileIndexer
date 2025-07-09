package koolfileindexer.controller;

public class UninitializedServerException extends Exception {

    public UninitializedServerException(Integer port) {
        super("Server is not listening on port: " + port);
    }
}
