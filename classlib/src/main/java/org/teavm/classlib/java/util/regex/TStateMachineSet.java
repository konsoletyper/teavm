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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches a sequence of single char nodes and quantifiers over them, or alternatives of such sequences,
 * by running a deterministic state machine, and passes control to the next node at positions where
 * the machine is in accepting state.
 *
 * <p>Chars are split into segments by sorted {@link #bounds}: segment of a char is the number of bounds
 * that are less than or equal to the char. States are numbered from 1, 0 means that no transition exists.
 * Transition from state {@code s} by a char from segment {@code g} is stored at
 * {@code (s - 1) * segmentCount + g}. States starting from {@link #firstAccepting} are accepting.
 *
 * <p>The machine is only built when there's no more than one way to match a string by the original nodes,
 * except for where the match ends. Order in which these ends are tried is defined by {@link #mode}:
 * {@link #GREEDY} tries them from right to left, {@link #RELUCTANT} tries them as soon as they are reached,
 * {@link #POSSESSIVE} tries only the rightmost one. In greedy and possessive modes accepting states only
 * have transitions to accepting states, so accepting positions of a run are contiguous.
 *
 * <p>Nodes that match surrogate pairs as a single code point are not modelled by the machine. Instead,
 * when such node would read a surrogate char, transition is {@link #FALLBACK}, and the original sequence
 * of nodes, kept in {@link #fallback}, is matched instead.
 */
class TStateMachineSet extends TAbstractSet {
    static final int GREEDY = 0;
    static final int RELUCTANT = 1;
    static final int POSSESSIVE = 2;

    private static final int MAX_TABLE_SIZE = 4096;
    private static final int MAX_STATES = 64;
    private static final int MAX_UNROLLED = 64;
    private static final char FALLBACK = '\uFFFF';
    private static final int ASCII_SHIFT = 7;

    private char[] bounds;
    private char[] transitions;
    private int segmentCount;
    private int startState;
    private int firstAccepting;
    private int mode;
    private TAbstractSet fallback;

    /**
     * Transitions by ASCII chars, i.e. by chars that are less than {@code 1 << ASCII_SHIFT}. Transition from
     * state {@code s} is stored at {@code ((s - 1) << ASCII_SHIFT) + c}. While running, machine keeps
     * current state as offset of its row in this table.
     */
    private char[] asciiTransitions;

    TStateMachineSet(char[] bounds, char[] transitions, int startState, int firstAccepting, int mode,
            TAbstractSet fallback) {
        this.bounds = bounds;
        this.transitions = transitions;
        this.segmentCount = bounds.length + 1;
        this.startState = startState;
        this.firstAccepting = firstAccepting;
        this.mode = mode;
        this.fallback = fallback;
        int stateCount = transitions.length / segmentCount;
        int asciiCount = 1 << ASCII_SHIFT;
        asciiTransitions = new char[stateCount << ASCII_SHIFT];
        for (int i = 0; i < asciiCount; ++i) {
            int segment = findSegment((char) i);
            for (int state = 0; state < stateCount; ++state) {
                asciiTransitions[(state << ASCII_SHIFT) + i] = transitions[state * segmentCount + segment];
            }
        }
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        if (mode == RELUCTANT) {
            return matchesReluctant(stringIndex, testString, matchResult);
        }
        int rightBound = matchResult.getRightBound();
        int acceptingOffset = (firstAccepting - 1) << ASCII_SHIFT;
        int offset = (startState - 1) << ASCII_SHIFT;
        int index = stringIndex;

        // In this mode accepting states only lead to accepting states, so first run until accepting state
        // is reached, then run without checking for accepting states
        while (offset < acceptingOffset) {
            if (index >= rightBound) {
                if (hasTransitions(offset)) {
                    matchResult.hitEnd = true;
                }
                return -1;
            }
            int nextState = transition(offset, testString.charAt(index));
            if (nextState == 0) {
                return -1;
            }
            if (nextState == FALLBACK) {
                return fallback.matches(stringIndex, testString, matchResult);
            }
            offset = (nextState - 1) << ASCII_SHIFT;
            index++;
        }
        int firstAccepted = index;
        while (index < rightBound) {
            int nextState = transition(offset, testString.charAt(index));
            if (nextState == 0) {
                break;
            }
            if (nextState == FALLBACK) {
                return fallback.matches(stringIndex, testString, matchResult);
            }
            offset = (nextState - 1) << ASCII_SHIFT;
            index++;
        }
        if (index >= rightBound && hasTransitions(offset)) {
            matchResult.hitEnd = true;
        }

        if (mode == POSSESSIVE) {
            return next.matches(index, testString, matchResult);
        }
        for (; index >= firstAccepted; --index) {
            int result = next.matches(index, testString, matchResult);
            if (result >= 0) {
                return result;
            }
        }
        return -1;
    }

    private int matchesReluctant(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int rightBound = matchResult.getRightBound();
        int acceptingOffset = (firstAccepting - 1) << ASCII_SHIFT;
        int offset = (startState - 1) << ASCII_SHIFT;
        int index = stringIndex;
        while (true) {
            if (offset >= acceptingOffset) {
                int result = next.matches(index, testString, matchResult);
                if (result >= 0) {
                    return result;
                }
            }
            if (index >= rightBound) {
                if (hasTransitions(offset)) {
                    matchResult.hitEnd = true;
                }
                return -1;
            }
            // Reluctant machines are never built with fallback
            int nextState = transition(offset, testString.charAt(index));
            if (nextState == 0) {
                return -1;
            }
            offset = (nextState - 1) << ASCII_SHIFT;
            index++;
        }
    }

    private int transition(int offset, char c) {
        if (c < 1 << ASCII_SHIFT) {
            return asciiTransitions[offset + c];
        }
        return transitions[(offset >> ASCII_SHIFT) * segmentCount + findSegment(c)];
    }

    private int findSegment(char c) {
        int low = 0;
        int high = bounds.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (bounds[mid] <= c) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private boolean hasTransitions(int offset) {
        int row = (offset >> ASCII_SHIFT) * segmentCount;
        for (int i = 0; i < segmentCount; ++i) {
            if (transitions[row + i] != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasConsumed(TMatchResultImpl matchResult) {
        return true;
    }

    @Override
    protected String getName() {
        return "stateMachine";
    }

    @Override
    void describe(TPatternWriter writer) {
        writer.create(this, TStateMachineSet.class, "stateMachineSet", new String(bounds),
                new String(transitions), startState, firstAccepting, mode, fallback);
    }

    /**
     * Tries to build state machine from a chain of nodes that starts at {@code first} and ends before
     * {@code end}. Returns {@code null} if there's nothing to gain, otherwise returns machine
     * which next node is the first node in the chain not covered by the machine.
     */
    static TStateMachineSet create(TAbstractSet first, TAbstractSet end) {
        // These nodes have their own fast find, keep them
        if (first.getClass() == TCharSet.class || first.getClass() == TSequenceSet.class
                || first.getClass() == TUnifiedQuantifierSet.class) {
            return null;
        }

        var items = new ArrayList<Item>();
        var itemCounts = new ArrayList<Integer>();
        var nextNodes = new ArrayList<TAbstractSet>();
        TAbstractSet node = first;
        while (node != end && addItems(node, items)) {
            node = node.getNext();
            itemCounts.add(items.size());
            nextNodes.add(node);
        }

        // Take as many nodes as possible
        for (int count = nextNodes.size(); count >= 2; --count) {
            var alternatives = List.<List<Item>>of(items.subList(0, itemCounts.get(count - 1)));
            var result = build(alternatives, first);
            if (result != null) {
                result.setNext(nextNodes.get(count - 1));
                return result;
            }
        }
        return null;
    }

    /**
     * Tries to build state machine that replaces all alternatives of a group. Each alternative
     * is a chain of nodes that ends with {@code end}. The machine's next node is {@code end}.
     */
    static TStateMachineSet createAlternatives(List<TAbstractSet> alternatives, TAbstractSet end) {
        if (alternatives.size() < 2) {
            return null;
        }
        var itemLists = new ArrayList<List<Item>>();
        for (var alternative : alternatives) {
            var items = new ArrayList<Item>();
            var node = alternative;
            while (node != end) {
                if (!addItems(node, items)) {
                    return null;
                }
                node = node.getNext();
            }
            for (var item : items) {
                if (item.codePoints) {
                    // No fallback for alternatives
                    return null;
                }
            }
            itemLists.add(items);
        }
        var result = build(itemLists, null);
        if (result != null) {
            result.setNext(end);
        }
        return result;
    }

    private static TStateMachineSet build(List<List<Item>> alternatives, TAbstractSet fallback) {
        var end = new Position(Position.END);
        var root = alternatives(alternatives, 0, end);

        // Enumerate states, i.e. positions where the machine can be before reading a char
        var states = new ArrayList<Position>();
        var closures = new HashMap<Position, List<Position>>();
        var stateIndexes = new HashMap<Position, Integer>();
        states.add(root);
        stateIndexes.put(root, 0);
        var boundSet = new BitSet();
        boolean codePoints = false;
        for (int i = 0; i < states.size(); ++i) {
            var closure = closure(states.get(i), closures);
            for (var position : closure) {
                if (position.kind == Position.END) {
                    continue;
                }
                var target = position.kind == Position.LOOP ? position : position.next;
                if (!stateIndexes.containsKey(target)) {
                    stateIndexes.put(target, states.size());
                    states.add(target);
                }
                var item = position.item;
                codePoints |= item.codePoints;
                var bits = item.chars;
                for (int j = bits.nextSetBit(0); j >= 0 && j <= Character.MAX_VALUE; j = bits.nextSetBit(j)) {
                    boundSet.set(j);
                    j = bits.nextClearBit(j);
                    boundSet.set(j);
                }
            }
        }
        if (codePoints) {
            boundSet.set(Character.MIN_SURROGATE);
            boundSet.set(Character.MAX_SURROGATE + 1);
        }
        boundSet.clear(0);
        boundSet.clear(Character.MAX_VALUE + 1, Integer.MAX_VALUE);
        int segmentCount = boundSet.cardinality() + 1;
        if (states.size() > MAX_STATES || states.size() * segmentCount > MAX_TABLE_SIZE) {
            return null;
        }
        var bounds = new char[segmentCount - 1];
        int index = 0;
        for (int i = boundSet.nextSetBit(0); i >= 0; i = boundSet.nextSetBit(i + 1)) {
            bounds[index++] = (char) i;
        }

        // Find out in which order to try accepting positions. Closure lists positions in the order in which
        // backtracking would try them, so the end must be either the first or the last in every closure.
        int mode = -1;
        boolean anyPossessive = false;
        boolean allPossessive = true;
        var accepting = new boolean[states.size()];
        for (int i = 0; i < states.size(); ++i) {
            var closure = closures.get(states.get(i));
            int first = closure.indexOf(end);
            if (first < 0) {
                continue;
            }
            accepting[i] = true;
            int last = closure.lastIndexOf(end);
            boolean endsLast = onlyEnds(closure, first, closure.size(), end);
            boolean endsFirst = onlyEnds(closure, 0, last + 1, end);
            int stateMode;
            if (endsLast && endsFirst) {
                continue;
            } else if (endsLast) {
                stateMode = GREEDY;
            } else if (endsFirst) {
                stateMode = RELUCTANT;
            } else {
                return null;
            }
            if (mode >= 0 && mode != stateMode) {
                return null;
            }
            mode = stateMode;
        }
        for (var state : states) {
            for (var position : closures.get(state)) {
                if ((position.kind == Position.OPT || position.kind == Position.LOOP)
                        && closure(position.skip, closures).contains(end)) {
                    if (position.item.mode == POSSESSIVE) {
                        anyPossessive = true;
                    } else {
                        allPossessive = false;
                    }
                }
            }
        }
        if (anyPossessive) {
            if (!allPossessive || mode == RELUCTANT || alternatives.size() > 1) {
                return null;
            }
            mode = POSSESSIVE;
        } else if (mode < 0) {
            mode = GREEDY;
        } else if (mode == RELUCTANT && codePoints) {
            // Reluctant mode tries next node before the run is over, it can't fall back after that
            return null;
        }

        // Accepting states get the highest numbers
        var numbers = new int[states.size()];
        int number = 1;
        for (int i = 0; i < states.size(); ++i) {
            if (!accepting[i]) {
                numbers[i] = number++;
            }
        }
        int firstAccepting = number;
        for (int i = 0; i < states.size(); ++i) {
            if (accepting[i]) {
                numbers[i] = number++;
            }
        }

        var transitions = new char[states.size() * segmentCount];
        for (int i = 0; i < states.size(); ++i) {
            var closure = closures.get(states.get(i));
            int offset = (numbers[i] - 1) * segmentCount;
            for (int segment = 0; segment < segmentCount; ++segment) {
                char c = segment == 0 ? 0 : bounds[segment - 1];
                Position candidate = null;
                boolean surrogateFallback = false;
                for (var position : closure) {
                    if (position.kind == Position.END) {
                        continue;
                    }
                    if (Character.isSurrogate(c) && position.item.codePoints) {
                        surrogateFallback = true;
                    } else if (position.item.contains(c)) {
                        if (candidate != null) {
                            // More than one way to match the char
                            return null;
                        }
                        candidate = position;
                    }
                }
                int target = 0;
                if (surrogateFallback) {
                    target = FALLBACK;
                } else if (candidate != null) {
                    var targetPosition = candidate.kind == Position.LOOP ? candidate : candidate.next;
                    int targetIndex = stateIndexes.get(targetPosition);
                    if (mode != RELUCTANT && accepting[i] && !accepting[targetIndex]) {
                        // Accepting positions of a run won't be contiguous
                        return null;
                    }
                    target = numbers[targetIndex];
                }
                transitions[offset + segment] = (char) target;
            }
        }

        return new TStateMachineSet(bounds, transitions, numbers[0], firstAccepting, mode,
                codePoints ? fallback : null);
    }

    private static boolean onlyEnds(List<Position> positions, int from, int to, Position end) {
        for (int i = from; i < to; ++i) {
            if (positions.get(i) != end) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds positions for alternatives, starting from given item. Alternatives that start with the same
     * single char item share position for this item.
     */
    private static Position alternatives(List<List<Item>> alternatives, int start, Position end) {
        var options = new ArrayList<Position>();
        var groups = new ArrayList<List<List<Item>>>();
        for (var alternative : alternatives) {
            if (alternative.size() == start) {
                options.add(end);
                groups.add(null);
                continue;
            }
            var item = alternative.get(start);
            boolean merged = false;
            if (item.isSingle()) {
                for (var group : groups) {
                    if (group != null && group.get(0).get(start).isSameSingle(item)) {
                        group.add(alternative);
                        merged = true;
                        break;
                    }
                }
            }
            if (!merged) {
                var group = new ArrayList<List<Item>>();
                group.add(alternative);
                groups.add(group);
                options.add(null);
            }
        }
        for (int i = 0; i < options.size(); ++i) {
            var group = groups.get(i);
            if (group == null) {
                continue;
            }
            if (group.size() == 1) {
                options.set(i, sequence(group.get(0), start, end));
            } else {
                var position = new Position(Position.ONE);
                position.item = group.get(0).get(start);
                position.next = alternatives(group, start + 1, end);
                options.set(i, position);
            }
        }
        if (options.size() == 1) {
            return options.get(0);
        }
        var branch = new Position(Position.BRANCH);
        branch.options = options;
        return branch;
    }

    private static Position sequence(List<Item> items, int start, Position end) {
        var current = end;
        for (int i = items.size() - 1; i >= start; --i) {
            var item = items.get(i);
            if (item.max == Integer.MAX_VALUE) {
                var loop = new Position(Position.LOOP);
                loop.item = item;
                loop.skip = current;
                current = loop;
            } else {
                // a{0,2} is matched like (?:a(?:a)?)?
                var skip = current;
                for (int j = item.min; j < item.max; ++j) {
                    var optional = new Position(Position.OPT);
                    optional.item = item;
                    optional.next = current;
                    optional.skip = skip;
                    current = optional;
                }
            }
            for (int j = 0; j < item.min; ++j) {
                var single = new Position(Position.ONE);
                single.item = item;
                single.next = current;
                current = single;
            }
        }
        return current;
    }

    /**
     * Lists positions that can read next char or end the match, in order in which backtracking would try them.
     */
    private static List<Position> closure(Position position, Map<Position, List<Position>> cache) {
        var result = cache.get(position);
        if (result == null) {
            result = new ArrayList<>();
            switch (position.kind) {
                case Position.BRANCH:
                    for (var option : position.options) {
                        result.addAll(closure(option, cache));
                    }
                    break;
                case Position.OPT:
                case Position.LOOP:
                    if (position.item.mode == RELUCTANT) {
                        result.addAll(closure(position.skip, cache));
                        result.add(position);
                    } else {
                        result.add(position);
                        result.addAll(closure(position.skip, cache));
                    }
                    break;
                default:
                    result.add(position);
                    break;
            }
            cache.put(position, result);
        }
        return result;
    }

    private static boolean addItems(TAbstractSet node, List<Item> items) {
        var nodeClass = node.getClass();
        int mode;
        int min = 0;
        int max = Integer.MAX_VALUE;
        if (nodeClass == TLeafQuantifierSet.class || nodeClass == TUnifiedQuantifierSet.class) {
            mode = GREEDY;
        } else if (nodeClass == TReluctantQuantifierSet.class) {
            mode = RELUCTANT;
        } else if (nodeClass == TPossessiveQuantifierSet.class) {
            mode = POSSESSIVE;
        } else if (nodeClass == TAltQuantifierSet.class) {
            mode = GREEDY;
            max = 1;
        } else if (nodeClass == TReluctantAltQuantifierSet.class) {
            mode = RELUCTANT;
            max = 1;
        } else if (nodeClass == TPossessiveAltQuantifierSet.class) {
            mode = POSSESSIVE;
            max = 1;
        } else if (nodeClass == TCompositeQuantifierSet.class) {
            mode = GREEDY;
        } else if (nodeClass == TReluctantCompositeQuantifierSet.class) {
            mode = RELUCTANT;
        } else if (nodeClass == TPossessiveCompositeQuantifierSet.class) {
            mode = POSSESSIVE;
        } else if (nodeClass == TSequenceSet.class) {
            var string = ((TSequenceSet) node).getString();
            for (int i = 0; i < string.length(); ++i) {
                items.add(new Item(string.charAt(i)));
            }
            return true;
        } else {
            var item = singleCharItem(node);
            if (item == null) {
                return false;
            }
            items.add(item);
            return true;
        }

        if (node instanceof TCompositeQuantifierSet) {
            var quantifier = ((TCompositeQuantifierSet) node).quantifier;
            min = quantifier.min();
            max = quantifier.max();
            if (max != Integer.MAX_VALUE && max > MAX_UNROLLED || min > MAX_UNROLLED) {
                return false;
            }
        }
        var item = singleCharItem(((TLeafQuantifierSet) node).getInnerSet());
        if (item == null) {
            return false;
        }
        item.min = min;
        item.max = max;
        item.mode = mode;
        items.add(item);
        return true;
    }

    private static Item singleCharItem(TAbstractSet node) {
        if (node.getClass() == TCharSet.class) {
            return new Item(((TCharSet) node).getChar());
        } else if (node.getClass() == TCompositeRangeSet.class) {
            // Composite range set differs from its part without surrogates only on surrogate chars
            return rangeItem(((TCompositeRangeSet) node).getWithoutSurrogates(), true);
        } else {
            return rangeItem(node, node.getClass() == TSupplRangeSet.class);
        }
    }

    private static Item rangeItem(TAbstractSet node, boolean codePoints) {
        TAbstractCharClass chars;
        if (node.getClass() == TRangeSet.class) {
            chars = ((TRangeSet) node).getChars();
        } else if (node.getClass() == TSupplRangeSet.class) {
            chars = ((TSupplRangeSet) node).getChars();
        } else {
            return null;
        }

        // Surrogate chars are not examined when code points are matched
        if (codePoints && chars.getClass() == TAbstractCharClass.WithoutSurrogatesCharClass.class) {
            chars = ((TAbstractCharClass.WithoutSurrogatesCharClass) chars).base;
        }
        Item item;
        if (chars.getClass() == TCharClass.BitSetCharClass.class) {
            item = new Item(((TCharClass.BitSetCharClass) chars).bs, chars.isNegative());
        } else if (chars.getClass() == TCharClass.class && ((TCharClass) chars).nonBitSet == null) {
            item = new Item(((TCharClass) chars).bits, chars.isNegative());
        } else {
            return null;
        }
        item.codePoints = codePoints;
        return item;
    }

    /**
     * Char class repeated from {@link #min} to {@link #max} times.
     */
    private static class Item {
        BitSet chars;
        boolean negative;
        boolean codePoints;
        int min = 1;
        int max = 1;
        int mode;

        Item(char c) {
            chars = new BitSet();
            chars.set(c);
        }

        Item(BitSet chars, boolean negative) {
            this.chars = chars;
            this.negative = negative;
        }

        boolean contains(int c) {
            return negative ^ chars.get(c);
        }

        boolean isSingle() {
            return min == 1 && max == 1;
        }

        boolean isSameSingle(Item other) {
            return isSingle() && other.isSingle() && negative == other.negative && codePoints == other.codePoints
                    && chars.equals(other.chars);
        }
    }

    /**
     * Position in a sequence of items. {@link #ONE} reads a char and proceeds to {@link #next}.
     * {@link #OPT} either does the same or proceeds to {@link #skip} without reading a char.
     * {@link #LOOP} either reads a char and stays or proceeds to {@link #skip}. {@link #BRANCH} proceeds to
     * one of {@link #options}. {@link #END} means that the match ends.
     */
    private static class Position {
        static final int ONE = 0;
        static final int OPT = 1;
        static final int LOOP = 2;
        static final int BRANCH = 3;
        static final int END = 4;

        int kind;
        Item item;
        Position next;
        Position skip;
        List<Position> options;

        Position(int kind) {
            this.kind = kind;
        }
    }
}
