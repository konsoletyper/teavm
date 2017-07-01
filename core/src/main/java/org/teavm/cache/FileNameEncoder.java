/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cache;

public final class FileNameEncoder {
    private FileNameEncoder() {
    }

    public static String encodeFileName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            switch (c) {
                case '/':
                    sb.append("$s");
                    break;
                case '\\':
                    sb.append("$b");
                    break;
                case '?':
                    sb.append("$q");
                    break;
                case '%':
                    sb.append("$p");
                    break;
                case '*':
                    sb.append("$a");
                    break;
                case ':':
                    sb.append("$c");
                    break;
                case '|':
                    sb.append("$v");
                    break;
                case '$':
                    sb.append("$$");
                    break;
                case '"':
                    sb.append("$Q");
                    break;
                case '<':
                    sb.append("$l");
                    break;
                case '>':
                    sb.append("$g");
                    break;
                case '.':
                    sb.append("$d");
                    break;
                case ' ':
                    sb.append("$w");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        String str = sb.toString();
        sb.setLength(0);
        for (int i = 0; i < str.length(); i += 100) {
            if (i > 0) {
                sb.append('/');
            }
            int j = Math.min(i + 100, str.length());
            sb.append(str.substring(i, j));
        }
        return sb.toString();
    }
}
