package koolfileindexer.common.utils;

public sealed interface Result<T, E> permits Result.Success, Result.Error {
    record Success<T, E>(T value) implements Result<T, E> {
    }

    record Error<T, E>(E error) implements Result<T, E> {
    }

    static <T, E> Result<T, E> success(T value) {
        return new Success<T, E>(value);
    }

    static <T, E> Result<T, E> error(E error) {
        return new Error<T, E>(error);
    }
}
