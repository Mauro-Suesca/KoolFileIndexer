package koolfileindexer.common.utils;

@FunctionalInterface
public interface ServerFunction<T, R> {
    public R apply(T t) throws Exception;
}
