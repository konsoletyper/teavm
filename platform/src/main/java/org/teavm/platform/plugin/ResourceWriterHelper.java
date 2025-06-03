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

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

final class ResourceWriterHelper {
    private ResourceWriterHelper() {
    }

    public static void write(SourceWriter writer, ResourceBuilder resource) {
        if (resource instanceof ResourceArrayBuilder<?>) {
            var data = (ResourceArrayBuilder<?>) resource;
            writer.append('[').tokenBoundary();
            for (int i = 0; i < data.values.size(); ++i) {
                if (i > 0) {
                    writer.append(',').ws();
                }
                write(writer, data.values.get(i));
            }
            writer.append(']').tokenBoundary();
        } else if (resource instanceof ResourceMapBuilder<?>) {
            var data = (ResourceMapBuilder<?>) resource;
            writer.append('{');
            boolean first = true;
            for (var entry : data.values.entrySet()) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                RenderingUtil.writeString(writer, entry.getKey());
                writer.append(':').ws();
                write(writer, entry.getValue());
            }
            writer.append('}').tokenBoundary();
        } else if (resource instanceof ObjectResourceBuilder) {
            var data = (ObjectResourceBuilder) resource;
            writer.append('{');
            var fieldNames = data.fieldNames();
            for (int i = 0; i < fieldNames.length; ++i) {
                if (i > 0) {
                    writer.append(',').ws();
                }
                writeIdentifier(writer, fieldNames[i]);
                writer.ws().append(':').ws();
                writeValue(writer, data.getValue(i));
            }
            writer.append('}').tokenBoundary();
        } else {
            throw new RuntimeException("Error compiling resources. Value of illegal type found: "
                    + resource.getClass());
        }
    }

    public static void writeValue(SourceWriter writer, Object value) {
        if (value instanceof ResourceBuilder) {
            write(writer, (ResourceBuilder) value);
        } else if (value instanceof Number) {
            writer.append(value.toString());
        } else if (value instanceof Boolean) {
            writer.append(value == Boolean.TRUE ? "true" : "false");
        } else if (value instanceof String) {
            RenderingUtil.writeString(writer, (String) value);
        } else if (value == null) {
            writer.append("null");
        }
    }

    public static void writeIdentifier(SourceWriter writer, String id) {
        if (id.isEmpty() || !isIdentifierStart(id.charAt(0))) {
            RenderingUtil.writeString(writer, id);
            return;
        }
        for (int i = 1; i < id.length(); ++i) {
            if (isIdentifierPart(id.charAt(i))) {
                RenderingUtil.writeString(writer, id);
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
}
