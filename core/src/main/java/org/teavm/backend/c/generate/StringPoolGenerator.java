/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.generate;

import java.util.List;

public class StringPoolGenerator {
    private CodeWriter writer;

    public StringPoolGenerator(CodeWriter writer) {
        this.writer = writer;
    }

    public void generate(List<? extends String> strings) {
        writer.println("static JavaString stringPool[" + strings.size() + "] = {").indent();
        for (int i = 0; i < strings.size(); ++i) {
            String s = strings.get(i);
            boolean codes = hasBadCharacters(s);
            String macroName = codes ? "TEAVM_STRING_FROM_CODES" : "TEAVM_STRING";
            writer.print(macroName + "(" + s.length() + ", " + s.hashCode() + ",");
            if (codes) {
                generateNumericStringLiteral(s);
            } else {
                generateSimpleStringLiteral(s);
            }
            writer.print(")");

            writer.print(i < strings.size() - 1 ? "," : " ");
            writer.print(" // string #" + i);
            writer.println();
        }
        writer.outdent().println("};");
    }

    private boolean hasBadCharacters(String string) {
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == 0 || Character.isSurrogate(c)) {
                return true;
            }
        }
        return false;
    }

    private void generateSimpleStringLiteral(String string) {
        if (string.isEmpty()) {
            writer.print("u\"\"");
            return;
        }

        int chunkSize = 256;
        for (int i = 0; i < string.length(); i += chunkSize) {
            if (i > 0) {
                writer.println();
            }
            int last = Math.min(i + chunkSize, string.length());
            writer.print("u\"");

            for (int j = i; j < last; ++j) {
                char c = string.charAt(j);
                switch (c) {
                    case '\\':
                        writer.print("\\\\");
                        break;
                    case '"':
                        writer.print("\\\"");
                        break;
                    case '\r':
                        writer.print("\\r");
                        break;
                    case '\n':
                        writer.print("\\n");
                        break;
                    case '\t':
                        writer.print("\\t");
                        break;
                    default:
                        if (c < 32) {
                            writer.print("\\0" + Character.forDigit(c >> 3, 8) + Character.forDigit(c & 0x7, 8));
                        } else if (c > 127) {
                            writer.print("\\u"
                                    + Character.forDigit(c >> 12, 16)
                                    + Character.forDigit((c >> 8) & 15, 16)
                                    + Character.forDigit((c >> 4) & 15, 16)
                                    + Character.forDigit(c & 15, 16));
                        } else {
                            writer.print(String.valueOf(c));
                        }
                        break;
                }
            }

            writer.print("\"");
        }
    }

    private void generateNumericStringLiteral(String string) {
        for (int i = 0; i < string.length(); ++i) {
            if (i > 0) {
                writer.print(", ");
            }
            int c = string.charAt(i);
            writer.print(Integer.toString(c));
        }
    }
}
