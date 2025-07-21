package koolfileindexer.common.utils;

import koolfileindexer.common.exceptions.InvalidFormatException;

@FunctionalInterface
public interface FromStr<T> {
    public T from(String source) throws InvalidFormatException;
}
