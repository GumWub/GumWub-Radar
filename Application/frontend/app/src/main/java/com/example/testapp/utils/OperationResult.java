package com.example.testapp.utils;

public final class OperationResult<T> {
    private final T value;
    private final String errorMessage;

    private OperationResult(T value, String errorMessage) {
        this.value = value;
        this.errorMessage = errorMessage;
    }

    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(value, null);
    }

    public static <T> OperationResult<T> failure(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("An error message is required");
        }
        return new OperationResult<>(null, errorMessage);
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }

    public T getValue() {
        if (!isSuccess()) {
            throw new IllegalStateException("Cannot read a failed result");
        }
        return value;
    }

    public String getErrorMessage() {
        if (isSuccess()) {
            throw new IllegalStateException("A successful result has no error");
        }
        return errorMessage;
    }
}
