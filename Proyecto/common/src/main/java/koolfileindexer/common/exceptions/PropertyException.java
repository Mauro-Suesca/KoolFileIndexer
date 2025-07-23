package koolfileindexer.common.exceptions;

public class PropertyException extends Exception {
    public PropertyException() {
        super("The property doesn't exist");
    }

    public PropertyException(String property) {
        super("The property " + property + " doesn't exist");
    }
}
