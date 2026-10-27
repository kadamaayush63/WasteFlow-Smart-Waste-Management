package com.wasteflow.route;

import java.util.NoSuchElementException;
import java.util.Objects;

public class ArrayStack<T> {

    private Object[] values = new Object[8];
    private int size;

    public void push(T value) {
        if (size == values.length) {
            Object[] expanded = new Object[values.length * 2];
            System.arraycopy(values, 0, expanded, 0, size);
            values = expanded;
        }
        values[size++] = Objects.requireNonNull(value);
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot pop from an empty stack");
        }
        int top = --size;
        T value = (T) values[top];
        values[top] = null;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek at an empty stack");
        }
        return (T) values[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}
