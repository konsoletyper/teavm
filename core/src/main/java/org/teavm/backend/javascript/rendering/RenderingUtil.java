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

public final class RenderingUtil {
    private static final String variableNames = "abcdefghijkmnopqrstuvwxyz";
    private static final String variablePartNames = "abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

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
                    } else if (Character.isLowSurrogate(c) || Character.isHighSurrogate(c)) {
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

    public static String indexToId(int index) {
        StringBuilder sb = new StringBuilder();
        sb.append(variableNames.charAt(index % variableNames.length()));
        while (index > 0) {
            sb.append(variablePartNames.charAt(index % variablePartNames.length()));
            index /= variablePartNames.length();
        }
        return sb.toString();
    }
}
