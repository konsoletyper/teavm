/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.classlib.java.util.regex;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.teavm.classlib.impl.regex.PatternConstructionListener;

/**
 * Used in build time only. Describes a compiled pattern as a sequence of calls to {@link TPatternFactory}
 * methods. Each node of the pattern describes itself, and nodes that don't know how to describe
 * themselves make the whole pattern unsupported.
 */
public final class TPatternWriter {
    private PatternConstructionListener listener;
    private Set<Object> created = Collections.newSetFromMap(new IdentityHashMap<>());
    private Set<Object> inProgress = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<Object[]> deferredCalls = new ArrayList<>();

    private TPatternWriter(PatternConstructionListener listener) {
        this.listener = listener;
    }

    /**
     * Describes pattern to given listener.
     *
     * @return {@code false} if pattern can't be described, in which case listener receives incomplete
     * sequence of calls which should be discarded.
     */
    public static boolean write(TPattern pattern, PatternConstructionListener listener) {
        var writer = new TPatternWriter(listener);
        try {
            writer.node(pattern);
            for (var i = 0; i < writer.deferredCalls.size(); ++i) {
                var call = writer.deferredCalls.get(i);
                writer.call((String) call[0], (Object[]) call[1]);
            }
        } catch (UnsupportedPatternException e) {
            return false;
        }
        return true;
    }

    static RuntimeException unsupported() {
        return new UnsupportedPatternException();
    }

    /**
     * Creates node by calling factory method. Arguments that are nodes are created first. Throws exception
     * if exact class of node is not {@code nodeClass}, which prevents subclasses from inheriting
     * description of their superclass.
     */
    void create(Object node, Class<?> nodeClass, String factoryMethod, Object... arguments) {
        if (node.getClass() != nodeClass || created.contains(node)) {
            throw unsupported();
        }
        ensureCreated(arguments);
        listener.create(node, factoryMethod, arguments);
        created.add(node);
    }

    /**
     * Calls factory method. Nodes passed as arguments are created first.
     */
    void call(String factoryMethod, Object... arguments) {
        ensureCreated(arguments);
        listener.call(factoryMethod, arguments);
    }

    /**
     * Calls factory method after all nodes reachable from pattern are created. This allows to describe
     * cyclic references between nodes.
     */
    void callLater(String factoryMethod, Object... arguments) {
        deferredCalls.add(new Object[] { factoryMethod, arguments });
    }

    /**
     * Describes content of a bit set as a sequence of calls to factory method that takes node and
     * a range of bits.
     */
    void describeBits(Object node, String factoryMethod, BitSet bits) {
        int start = bits.nextSetBit(0);
        while (start >= 0) {
            int end = bits.nextClearBit(start);
            call(factoryMethod, node, start, end);
            start = bits.nextSetBit(end);
        }
    }

    private void ensureCreated(Object[] arguments) {
        for (var argument : arguments) {
            if (argument != null && !(argument instanceof String) && !(argument instanceof Integer)
                    && !(argument instanceof Boolean) && !(argument instanceof Character)) {
                node(argument);
            }
        }
    }

    private void node(Object node) {
        if (created.contains(node)) {
            return;
        }
        if (!inProgress.add(node)) {
            throw unsupported();
        }
        if (node instanceof TAbstractSet set) {
            set.describe(this);
            if (set.next != null) {
                callLater("link", set, set.next);
            }
        } else if (node instanceof TAbstractCharClass) {
            ((TAbstractCharClass) node).describe(this);
        } else if (node instanceof TQuantifier) {
            ((TQuantifier) node).describe(this);
        } else if (node instanceof TAbstractLineTerminator) {
            ((TAbstractLineTerminator) node).describe(this);
        } else if (node instanceof TPattern) {
            ((TPattern) node).describe(this);
        }
        inProgress.remove(node);
        if (!created.contains(node)) {
            throw unsupported();
        }
    }

    private static class UnsupportedPatternException extends RuntimeException {
        UnsupportedPatternException() {
            super(null, null, false, false);
        }
    }
}
