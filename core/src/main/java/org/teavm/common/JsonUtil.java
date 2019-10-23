/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.common;

import java.io.IOException;
import java.io.Writer;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static void writeEscapedString(Writer output, String str) throws IOException {
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    output.write("\\n");
                    break;
                case '\r':
                    output.write("\\r");
                    break;
                case '\t':
                    output.write("\\t");
                    break;
                case '\b':
                    output.write("\\b");
                    break;
                case '\\':
                    output.write("\\\\");
                    break;
                case '"':
                    output.write("\\\"");
                    break;
                default:
                    output.write(c);
                    break;
            }
        }
    }
}
