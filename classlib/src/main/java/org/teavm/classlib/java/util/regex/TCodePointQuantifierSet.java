/*
 *  Copyright 2026 lemonlion.
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

import java.util.Arrays;

/**
 * Greedy quantifier ({@code *}, {@code +}) over a node that consumes a single code point, like a character class,
 * see {@link TCodePointSet}.
 *
 * <p>{@link TGroupQuantifierSet} would match such node by recursion: its {@code matches(i)} calls
 * {@code innerSet.matches(i)}, which tries each way of consuming input at {@code i}, calling {@code matches(i + n)}
 * after each, where {@code n} is the number of chars the way consumes, until one succeeds; if none does,
 * {@code matches(i)} passes control to the next node at {@code i}. That nests a few calls per consumed code point,
 * so long input overflows the stack. This node walks the same tree depth first in a loop: it tries the same ways in
 * the same order and passes control to the next node at the same indexes in the same order.
 */
class TCodePointQuantifierSet extends TQuantifierSet {
    public TCodePointQuantifierSet(TAbstractSet innerSet, TAbstractSet next, int type) {
        super(innerSet, next, type);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        TCodePointSet codePoint = (TCodePointSet) innerSet;
        int start = stringIndex;
        int way = 0;
        int[] stack = null;
        int depth = 0;

        while (true) {
            int step = codePoint.consume(way, stringIndex, testString, matchResult);
            if (step >= 0) {
                if (step != 1) {
                    stack = push(stack, depth, stringIndex, step);
                    depth += 2;
                }
                stringIndex += step & 3;
                way = 0;
                continue;
            }

            // No way of consuming input is left here, so pass control to the next node. It returns either a negative
            // value or an index not less than stringIndex, so a non-negative result is accepted by every way that
            // led here (even by the first way of TSupplRangeSet, which needs a result > 0) and is the result of
            // this node.
            int result = next.matches(stringIndex, testString, matchResult);
            if (result >= 0 || stringIndex == start) {
                return result;
            }

            // Go back to the index we came from and try its next way. If the step that led here is not on the stack,
            // it was the first way consuming one char.
            if (depth > 0 && stack[depth - 2] + (stack[depth - 1] & 3) == stringIndex) {
                depth -= 2;
                stringIndex = stack[depth];
                way = (stack[depth + 1] >> 2) + 1;
            } else {
                stringIndex--;
                way = 1;
            }
        }
    }

    /**
     * Records the step the quantifier made from {@code index}, as {@link TCodePointSet#consume} returned it. Not
     * needed for the common step, the first way consuming one char, which is what going back assumes when the stack
     * does not say otherwise, so matching text without surrogates does not allocate.
     */
    static int[] push(int[] stack, int depth, int index, int step) {
        if (stack == null) {
            stack = new int[16];
        } else if (depth == stack.length) {
            stack = Arrays.copyOf(stack, depth * 2);
        }
        stack[depth] = index;
        stack[depth + 1] = step;
        return stack;
    }

    @Override
    protected String getName() {
        return "<CodePointQuant>";
    }

    @Override
    void describe(TPatternWriter writer) {
        writer.create(this, TCodePointQuantifierSet.class, "codePointQuantifierSet", innerSet, type);
    }
}
