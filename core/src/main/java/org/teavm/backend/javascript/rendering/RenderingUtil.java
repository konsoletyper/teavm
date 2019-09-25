/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.backend.javascript.codegen.SourceWriter;

public final class RenderingUtil {
    public static final Set<String> KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("break", "case",
            "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export",
            "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "return",
            "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
            "NaN", "Map", "Set", "eval", "Math", "Date", "JSON", "Intl", "URL")));
    public static final String VARIABLE_START_CHARS = "abcdefghijklmnopqrstuvwxyz";
    public static final String VARIABLE_PART_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789$_";

    private RenderingUtil() {
    }

    public static String escapeName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
        return sb.toString();
    }

    public static void writeString(SourceWriter writer, String s) throws IOException {
        if (s.isEmpty()) {
            writer.append("\"\"");
            return;
        }
        for (int i = 0; i < s.length(); i += 512) {
            int next = Math.min(i + 512, s.length());
            if (i > 0) {
                writer.newLine().append("+").ws();
            }
            writer.append('"');
            for (int j = i; j < next; ++j) {
                char c = s.charAt(j);
                switch (c) {
                    case '\r':
                        writer.append("\\r");
                        break;
                    case '\n':
                        writer.append("\\n");
                        break;
                    case '\t':
                        writer.append("\\t");
                        break;
                    case '\'':
                        writer.append("\\'");
                        break;
                    case '\"':
                        writer.append("\\\"");
                        break;
                    case '\\':
                        writer.append("\\\\");
                        break;
                    default:
                        if (c < ' ') {
                            writer.append("\\u00").append(Character.forDigit(c / 16, 16))
                                    .append(Character.forDigit(c % 16, 16));
                        } else if (Character.isLowSurrogate(c) || Character.isHighSurrogate(c)
                                || !Character.isDefined(c)) {
                            writer.append("\\u")
                                    .append(Character.forDigit(c / 0x1000, 0x10))
                                    .append(Character.forDigit((c / 0x100) % 0x10, 0x10))
                                    .append(Character.forDigit((c / 0x10) % 0x10, 0x10))
                                    .append(Character.forDigit(c % 0x10, 0x10));
                        } else {
                            writer.append(c);
                        }
                        break;
                }
            }
            writer.append('"');
        }
    }

    public static String escapeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u00").append(Character.forDigit(c / 16, 16))
                                .append(Character.forDigit(c % 16, 16));
                    } else if (Character.isLowSurrogate(c) || Character.isHighSurrogate(c)
                            || !Character.isDefined(c)) {
                        sb.append("\\u")
                                .append(Character.forDigit(c / 0x1000, 0x10))
                                .append(Character.forDigit((c / 0x100) % 0x10, 0x10))
                                .append(Character.forDigit((c / 0x10) % 0x10, 0x10))
                                .append(Character.forDigit(c % 0x10, 0x10));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static String indexToId(int index, String startChars) {
        if (index >= startChars.length()) {
            index -= startChars.length() - VARIABLE_PART_CHARS.length();
            StringBuilder sb = new StringBuilder();
            while (index >= startChars.length()) {
                sb.append(VARIABLE_PART_CHARS.charAt(index % VARIABLE_PART_CHARS.length()));
                index /= VARIABLE_PART_CHARS.length();
            }
            return sb.append(startChars.charAt(index % startChars.length())).reverse().toString();
        } else {
            return String.valueOf(startChars.charAt(index));
        }
    }

    public static String indexToId(int index) {
        return indexToId(index, VARIABLE_START_CHARS);
    }

    public static boolean isSmallInteger(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return false;
        }

        Object constant = ((ConstantExpr) expr).getValue();
        if (!(constant instanceof Integer)) {
            return false;
        }

        int value = (Integer) constant;
        return Math.abs(value) < (1 << 18);
    }
}
