package koolfileindexer.common.exceptions;

public class InvalidFormatException extends Exception {
    public InvalidFormatException() {
        super("InvalidFormatException");
    }

    public InvalidFormatException(String className) {
        super("InvalidFormatException: String is not " + className + " format.");
    }

    public InvalidFormatException(Class<?> classObject) {
        super("InvalidFormatException: String is not " + classObject.getName() + " format.");
    }
}
