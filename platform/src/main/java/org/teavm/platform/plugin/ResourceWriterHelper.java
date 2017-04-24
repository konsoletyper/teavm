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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.backend.javascript.codegen.SourceWriter;

final class ResourceWriterHelper {
    private ResourceWriterHelper() {
    }

    public static void write(SourceWriter writer, Object resource) throws IOException {
        if (resource == null) {
            writer.append("null");
        } else {
            if (resource instanceof ResourceWriter) {
                ((ResourceWriter) resource).write(writer);
            } else if (resource instanceof Number) {
                writer.append(resource);
            } else if (resource instanceof Boolean) {
                writer.append(resource == Boolean.TRUE ? "true" : "false");
            } else if (resource instanceof String) {
                writeString(writer, (String) resource);
            } else {
                throw new RuntimeException("Error compiling resources. Value of illegal type found: "
                        + resource.getClass());
            }
        }
    }

    public static void writeIdentifier(SourceWriter writer, String id) throws IOException {
        if (id.isEmpty() || !isIdentifierStart(id.charAt(0))) {
            writeString(writer, id);
            return;
        }
        for (int i = 1; i < id.length(); ++i) {
            if (isIdentifierPart(id.charAt(i))) {
                writeString(writer, id);
                return;
            }
        }
        writer.append(id);
    }

    private static boolean isIdentifierStart(char c) {
        if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
            return true;
        }
        return c == '$' || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        if (isIdentifierStart(c)) {
            return true;
        }
        return c >= '0' && c <= '9';
    }

    public static void writeString(SourceWriter writer, String s) throws IOException {
        writer.append('"');
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\0':
                    writer.append("\\0");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        writer.append("\\u00").append(Character.forDigit(c / 16, 16))
                                .append(Character.forDigit(c % 16, 16));
                    } else {
                        writer.append(c);
                    }
                    break;
            }
        }
        writer.append('"');
    }
}
