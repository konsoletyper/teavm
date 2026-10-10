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
import java.util.List;

/**
 * Matches a sequence of single char nodes and greedy quantifiers over them by running a deterministic
 * state machine, and passes control to the next node at positions where the machine is in accepting state,
 * starting from the rightmost one.
 *
 * <p>Chars are split into segments by sorted {@link #bounds}: segment of a char is the number of bounds
 * that are less than or equal to the char. States are numbered from 1, 0 means that no transition exists.
 * Transition from state {@code s} by a char from segment {@code g} is stored at
 * {@code (s - 1) * segmentCount + g}. States starting from {@link #firstAccepting} are accepting and
 * have transitions only to accepting states, so that accepting positions of a run are contiguous.
 *
 * <p>The machine is only built when there's no more than one way to match a string by the sequence of
 * nodes, except for the number of iterations of the trailing quantifier. Therefore trying positions
 * from right to left gives the same order as backtracking of the original nodes.
 *
 * <p>Nodes that match surrogate pairs as a single code point are not modelled by the machine. Instead,
 * when such node would read a surrogate char, transition is {@link #FALLBACK}, and the original sequence
 * of nodes, kept in {@link #fallback}, is matched instead.
 */
class TStateMachineSet extends TAbstractSet {
    private static final int MAX_TABLE_SIZE = 4096;
    private static final char FALLBACK = '\uFFFF';

    private char[] bounds;
    private char[] transitions;
    private int segmentCount;
    private int firstAccepting;
    private TAbstractSet fallback;
    private char[] asciiSegments = new char[128];

    TStateMachineSet(char[] bounds, char[] transitions, int firstAccepting, TAbstractSet fallback) {
        this.bounds = bounds;
        this.transitions = transitions;
        this.segmentCount = bounds.length + 1;
        this.firstAccepting = firstAccepting;
        this.fallback = fallback;
        for (int i = 0; i < asciiSegments.length; ++i) {
            asciiSegments[i] = (char) findSegment((char) i);
        }
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        int rightBound = matchResult.getRightBound();
        int start = stringIndex;
        int state = 1;
        int firstAccepted = -1;
        while (true) {
            if (firstAccepted < 0 && state >= firstAccepting) {
                firstAccepted = stringIndex;
            }
            if (stringIndex >= rightBound) {
                if (hasTransitions(state)) {
                    matchResult.hitEnd = true;
                }
                break;
            }
            int nextState = transitions[(state - 1) * segmentCount + segment(testString.charAt(stringIndex))];
            if (nextState == 0) {
                break;
            }
            if (nextState == FALLBACK) {
                return fallback.matches(start, testString, matchResult);
            }
            state = nextState;
            stringIndex++;
        }

        if (firstAccepted >= 0) {
            for (; stringIndex >= firstAccepted; --stringIndex) {
                int result = next.matches(stringIndex, testString, matchResult);
                if (result >= 0) {
                    return result;
                }
            }
        }
        return -1;
    }

    private int segment(char c) {
        return c < asciiSegments.length ? asciiSegments[c] : findSegment(c);
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

    private boolean hasTransitions(int state) {
        int offset = (state - 1) * segmentCount;
        for (int i = 0; i < segmentCount; ++i) {
            if (transitions[offset + i] != 0) {
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
                new String(transitions), firstAccepting, fallback);
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

        var steps = new ArrayList<Step>();
        int nodeCount = 0;
        TAbstractSet node = first;
        while (node != end) {
            int stepCount = steps.size();
            if (!addSteps(node, steps)) {
                break;
            }
            if (stepCount > 0 && steps.get(stepCount - 1).loop) {
                // A loop must be followed by a single char from a disjoint set, so that
                // the loop can only be matched in one way
                var step = steps.get(stepCount);
                if (step.loop || intersects(steps.get(stepCount - 1), step)) {
                    while (steps.size() > stepCount) {
                        steps.remove(steps.size() - 1);
                    }
                    break;
                }
            }
            nodeCount++;
            node = node.getNext();
        }
        if (nodeCount < 2) {
            return null;
        }

        var boundSet = new BitSet();
        for (var step : steps) {
            var bits = step.chars;
            for (int i = bits.nextSetBit(0); i >= 0 && i <= Character.MAX_VALUE; i = bits.nextSetBit(i)) {
                boundSet.set(i);
                i = bits.nextClearBit(i);
                boundSet.set(i);
            }
        }
        boolean codePoints = false;
        for (var step : steps) {
            codePoints |= step.codePoints;
        }
        if (codePoints) {
            boundSet.set(Character.MIN_SURROGATE);
            boundSet.set(Character.MAX_SURROGATE + 1);
        }
        boundSet.clear(0);
        boundSet.clear(Character.MAX_VALUE + 1, Integer.MAX_VALUE);
        int segmentCount = boundSet.cardinality() + 1;
        int stateCount = steps.size() + 1;
        if (stateCount * segmentCount > MAX_TABLE_SIZE) {
            return null;
        }
        var bounds = new char[segmentCount - 1];
        int index = 0;
        for (int i = boundSet.nextSetBit(0); i >= 0; i = boundSet.nextSetBit(i + 1)) {
            bounds[index++] = (char) i;
        }

        // State i + 1 means that step i is the next to match, state steps.size() + 1 means that all steps
        // are matched. Loop may be either skipped or matched once more, but due to restrictions above
        // there's at most one transition for every char.
        var transitions = new char[stateCount * segmentCount];
        for (int i = 0; i < steps.size(); ++i) {
            var step = steps.get(i);
            var nextStep = step.loop && i + 1 < steps.size() ? steps.get(i + 1) : null;
            for (int segment = 0; segment < segmentCount; ++segment) {
                int c = segment == 0 ? 0 : bounds[segment - 1];
                int target = 0;
                if (Character.isSurrogate((char) c) && (step.codePoints
                        || nextStep != null && nextStep.codePoints)) {
                    target = FALLBACK;
                } else if (step.contains(c)) {
                    target = step.loop ? i + 1 : i + 2;
                } else if (nextStep != null && nextStep.contains(c)) {
                    target = i + 3;
                }
                transitions[i * segmentCount + segment] = (char) target;
            }
        }
        int firstAccepting = steps.get(steps.size() - 1).loop ? stateCount - 1 : stateCount;

        var result = new TStateMachineSet(bounds, transitions, firstAccepting, codePoints ? first : null);
        result.setNext(node);
        return result;
    }

    private static boolean addSteps(TAbstractSet node, List<Step> steps) {
        if (node.getClass() == TLeafQuantifierSet.class || node.getClass() == TUnifiedQuantifierSet.class) {
            var leaf = ((TLeafQuantifierSet) node).getInnerSet();
            if (leaf.getClass() == TSequenceSet.class || !addSteps(leaf, steps)) {
                return false;
            }
            steps.get(steps.size() - 1).loop = true;
            return true;
        } else if (node.getClass() == TCharSet.class) {
            steps.add(new Step(((TCharSet) node).getChar()));
            return true;
        } else if (node.getClass() == TSequenceSet.class) {
            var string = ((TSequenceSet) node).getString();
            for (int i = 0; i < string.length(); ++i) {
                steps.add(new Step(string.charAt(i)));
            }
            return true;
        }
        Step step;
        if (node.getClass() == TCompositeRangeSet.class) {
            // Composite range set differs from its part without surrogates only on surrogate chars
            step = rangeStep(((TCompositeRangeSet) node).getWithoutSurrogates(), true);
        } else {
            step = rangeStep(node, node.getClass() == TSupplRangeSet.class);
        }
        if (step == null) {
            return false;
        }
        steps.add(step);
        return true;
    }

    private static Step rangeStep(TAbstractSet node, boolean codePoints) {
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
        Step step;
        if (chars.getClass() == TCharClass.BitSetCharClass.class) {
            step = new Step(((TCharClass.BitSetCharClass) chars).bs, chars.isNegative());
        } else if (chars.getClass() == TCharClass.class && ((TCharClass) chars).nonBitSet == null) {
            step = new Step(((TCharClass) chars).bits, chars.isNegative());
        } else {
            return null;
        }
        step.codePoints = codePoints;
        return step;
    }

    private static boolean intersects(Step a, Step b) {
        if (a.negative && b.negative) {
            return true;
        }
        if (a.negative) {
            var tmp = a;
            a = b;
            b = tmp;
        }
        var bits = (BitSet) a.chars.clone();
        if (b.negative) {
            bits.andNot(b.chars);
        } else {
            bits.and(b.chars);
        }
        return !bits.isEmpty();
    }

    private static class Step {
        BitSet chars;
        boolean negative;
        boolean loop;
        boolean codePoints;

        Step(char c) {
            chars = new BitSet();
            chars.set(c);
        }

        Step(BitSet chars, boolean negative) {
            this.chars = chars;
            this.negative = negative;
        }

        boolean contains(int c) {
            return negative ^ chars.get(c);
        }
    }
}
