package com.wasteflow.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class SinglyLinkedList<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void add(T value) {
        Node<T> node = new Node<>(Objects.requireNonNull(value));
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    public Optional<T> find(Predicate<? super T> condition) {
        for (Node<T> node = head; node != null; node = node.next) {
            if (condition.test(node.value)) {
                return Optional.of(node.value);
            }
        }
        return Optional.empty();
    }

    public boolean update(Predicate<? super T> condition, UnaryOperator<T> updater) {
        for (Node<T> node = head; node != null; node = node.next) {
            if (condition.test(node.value)) {
                node.value = Objects.requireNonNull(updater.apply(node.value));
                return true;
            }
        }
        return false;
    }

    public boolean remove(Predicate<? super T> condition) {
        Node<T> previous = null;
        Node<T> current = head;
        while (current != null) {
            if (condition.test(current.value)) {
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                if (current == tail) {
                    tail = previous;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    public List<T> toList() {
        List<T> values = new ArrayList<>(size);
        for (Node<T> node = head; node != null; node = node.next) {
            values.add(node.value);
        }
        return values;
    }

    public int size() {
        return size;
    }

    private static final class Node<T> {
        private T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }
}