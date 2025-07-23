package koolfileindexer.common.utils;

import koolfileindexer.common.exceptions.PropertyException;

public class StringVisitor {
    public static String visitString(String line, String property) throws PropertyException {
        String[] result = line.split(": ");

        if (!result[0].equals(property)) {
            throw new PropertyException(property);
        }

        if (result.length < 2) {
            return "";
        }

        return result[1];
    }
}
