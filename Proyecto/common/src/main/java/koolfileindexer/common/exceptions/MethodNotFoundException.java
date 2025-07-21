package koolfileindexer.common.exceptions;

public class MethodNotFoundException extends Exception {
    public MethodNotFoundException(String method) {
        super("MethodNotFoundException: Method " + method + " not found");
    }
}
