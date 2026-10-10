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

/**
 * Reluctant quantifier ({@code *?}, {@code +?}) over a node that consumes a single code point, like {@code .} or
 * a character class, see {@link TCodePointSet}. Does what {@link TReluctantGroupQuantifierSet} does, which first
 * passes control to the next node and then recurses through the inner node, but in a loop, like
 * {@link TCodePointQuantifierSet}.
 */
class TReluctantCodePointQuantifierSet extends TCodePointQuantifierSet {
    public TReluctantCodePointQuantifierSet(TAbstractSet innerSet, TAbstractSet next, int type) {
        super(innerSet, next, type);
    }

    @Override
    public int matches(int stringIndex, CharSequence testString, TMatchResultImpl matchResult) {
        TCodePointSet codePoint = (TCodePointSet) innerSet;
        boolean singleWay = codePoint.ways() == 1;
        int start = stringIndex;
        int way = 0;
        int[] stack = null;
        int depth = 0;

        while (true) {
            if (way == 0) {
                int result = next.matches(stringIndex, testString, matchResult);
                if (result >= 0) {
                    return result;
                }
            }

            int step = codePoint.consume(way, stringIndex, testString, matchResult);
            if (step >= 0) {
                if (step != 1 && !singleWay) {
                    stack = push(stack, depth, stringIndex, step);
                    depth += 2;
                }
                stringIndex += step & 3;
                way = 0;
                continue;
            }

            // With a single way there is nothing left to try at the indexes we came through
            if (stringIndex == start || singleWay) {
                return -1;
            }

            // Go back to the index we came from and try its next way, see TCodePointQuantifierSet
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

    @Override
    void describe(TPatternWriter writer) {
        writer.create(this, TReluctantCodePointQuantifierSet.class, "reluctantCodePointQuantifierSet", innerSet,
                type);
    }
}
