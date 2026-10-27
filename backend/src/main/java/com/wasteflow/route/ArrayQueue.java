package com.wasteflow.route;

import java.util.NoSuchElementException;
import java.util.Objects;

public class ArrayQueue<T> {

    private Object[] values = new Object[8];
    private int head;
    private int size;

    public void enqueue(T value) {
        if (size == values.length) {
            grow();
        }
        values[(head + size) % values.length] = Objects.requireNonNull(value);
        size++;
    }

    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot dequeue from an empty queue");
        }
        T value = (T) values[head];
        values[head] = null;
        head = (head + 1) % values.length;
        size--;
        if (size == 0) {
            head = 0;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot peek at an empty queue");
        }
        return (T) values[head];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    private void grow() {
        Object[] expanded = new Object[values.length * 2];
        for (int index = 0; index < size; index++) {
            expanded[index] = values[(head + index) % values.length];
        }
        values = expanded;
        head = 0;
    }
}
