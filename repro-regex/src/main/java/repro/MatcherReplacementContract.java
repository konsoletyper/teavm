/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MatcherReplacementContract {
    private static final Pattern WORD_BOUNDARY = Pattern.compile("([A-Z])([A-Z])([a-z])|([a-z])([A-Z])");
    private static final String REPLACEMENT = "$1$4 $2$3$5";

    private MatcherReplacementContract() {
    }

    public static void main(String[] args) {
        check("Keyboard Binding", WORD_BOUNDARY.matcher("KeyboardBinding").replaceAll(REPLACEMENT),
                "unmatched groups from the second alternative");
        check("A Bcfoo Bar", WORD_BOUNDARY.matcher("ABcfooBar").replaceAll(REPLACEMENT),
                "unmatched groups across alternating matches");
        check("123", WORD_BOUNDARY.matcher("123").replaceAll(REPLACEMENT), "input without matches");

        Matcher matcher = WORD_BOUNDARY.matcher("KeyboardBinding");
        if (!matcher.find()) {
            throw new AssertionError("expected a keyboard binding boundary");
        }
        StringBuffer buffer = new StringBuffer();
        matcher.appendReplacement(buffer, REPLACEMENT);
        matcher.appendTail(buffer);
        check("Keyboard Binding", buffer.toString(), "appendReplacement unmatched groups");

        System.out.println("matcher replacement contract passed");
    }

    private static void check(String expected, String actual, String description) {
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
