package com.wasteflow.route;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SinglyLinkedListTest {

    @Test
    void supportsInsertFindUpdateDeleteAndOrderedTraversal() {
        SinglyLinkedList<String> values = new SinglyLinkedList<>();
        values.add("first");
        values.add("second");
        values.add("third");

        assertEquals("second", values.find("second"::equals).orElseThrow());
        assertTrue(values.update("second"::equals, ignored -> "updated"));
        assertTrue(values.remove("first"::equals));
        assertEquals(List.of("updated", "third"), values.toList());
        assertEquals(2, values.size());
        assertFalse(values.remove("missing"::equals));
    }
}