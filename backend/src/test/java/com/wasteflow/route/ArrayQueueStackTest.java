package com.wasteflow.route;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayQueueStackTest {

    @Test
    void queueProcessesValuesInFirstInFirstOutOrder() {
        ArrayQueue<String> queue = new ArrayQueue<>();
        queue.enqueue("first");
        queue.enqueue("second");

        assertEquals("first", queue.peek());
        assertEquals("first", queue.dequeue());
        assertEquals("second", queue.dequeue());
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::dequeue);
        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    void queuePreservesOrderWhenItWrapsAndGrows() {
        ArrayQueue<Integer> queue = new ArrayQueue<>();
        for (int value = 0; value < 8; value++) {
            queue.enqueue(value);
        }
        for (int value = 0; value < 5; value++) {
            assertEquals(value, queue.dequeue());
        }
        for (int value = 8; value < 14; value++) {
            queue.enqueue(value);
        }

        ArrayList<Integer> remaining = new ArrayList<>();
        while (!queue.isEmpty()) {
            remaining.add(queue.dequeue());
        }
        assertEquals(java.util.stream.IntStream.range(5, 14).boxed().toList(), remaining);
    }

    @Test
    void stackProcessesValuesInLastInFirstOutOrder() {
        ArrayStack<String> stack = new ArrayStack<>();
        stack.push("first");
        stack.push("second");

        assertEquals("second", stack.peek());
        assertEquals("second", stack.pop());
        assertEquals("first", stack.pop());
        assertEquals(0, stack.size());
        assertTrue(stack.isEmpty());
        assertThrows(NoSuchElementException.class, stack::pop);
        assertThrows(NoSuchElementException.class, stack::peek);
    }

    @Test
    void stackGrowsWithoutChangingLastInFirstOutOrder() {
        ArrayStack<Integer> stack = new ArrayStack<>();
        for (int value = 0; value < 20; value++) {
            stack.push(value);
        }

        for (int value = 19; value >= 0; value--) {
            assertEquals(value, stack.pop());
        }
        assertTrue(stack.isEmpty());
    }
}
