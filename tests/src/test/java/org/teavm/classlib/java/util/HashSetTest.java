/*
 *  Copyright 2026 Ethan Resnick.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class HashSetTest {
    // HashSet.clone: "Returns a shallow copy of this HashSet instance". Object.clone: by convention,
    // "x.clone().getClass() == x.getClass()". So a LinkedHashSet's clone is a LinkedHashSet, in the same order.
    @Test
    @SuppressWarnings("unchecked")
    public void linkedHashSetCloneIsLinked() {
        var set = new LinkedHashSet<>(List.of("b", "a"));
        var clone = (LinkedHashSet<String>) set.clone();
        assertEquals(LinkedHashSet.class, clone.getClass());
        assertEquals(List.of("b", "a"), new ArrayList<>(clone));
    }

    // Object.clone(): "x.clone() != x" and the clone is independent of the original. Iterates both sets to their
    // ends, since new ArrayList<>(set) copies only size() elements and would hide a shared linked list.
    @Test
    @SuppressWarnings("unchecked")
    public void linkedHashSetCloneIsIndependent() {
        var set = new LinkedHashSet<>(List.of("b", "a"));
        var clone = (LinkedHashSet<String>) set.clone();
        clone.add("c");
        set.add("d");
        assertEquals(List.of("b", "a", "d"), iterate(set));
        assertEquals(List.of("b", "a", "c"), iterate(clone));
        clone.remove("b");
        assertEquals(List.of("b", "a", "d"), iterate(set));
        assertEquals(List.of("a", "c"), iterate(clone));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void hashSetCloneIsIndependent() {
        var set = new HashSet<>(List.of("b", "a"));
        var clone = (HashSet<String>) set.clone();
        assertEquals(HashSet.class, clone.getClass());
        clone.add("c");
        set.remove("a");
        // The iteration order of a HashSet is unspecified, so the elements are compared as sets.
        assertEquals(List.of("b"), iterate(set));
        var cloneElements = iterate(clone);
        assertEquals(3, cloneElements.size());
        assertEquals(Set.of("a", "b", "c"), new HashSet<>(cloneElements));
    }

    static class CustomSet extends HashSet<String> {
    }

    @Test
    public void subclassCloneHasSubclass() {
        var set = new CustomSet();
        set.add("a");
        var clone = set.clone();
        assertEquals(CustomSet.class, clone.getClass());
        assertEquals(set, clone);
    }

    private static <T> List<T> iterate(Iterable<T> iterable) {
        var result = new ArrayList<T>();
        for (var element : iterable) {
            result.add(element);
        }
        return result;
    }
}
