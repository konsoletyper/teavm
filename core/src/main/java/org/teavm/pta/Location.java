/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.pta;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

public class Location {
    private final AnalysisState state;
    private final int id;
    private final ValueType type;
    private final String description;
    private final Object extension;
    private Map<FieldReference, Node> fieldNodes;
    private Node arrayItemNode;

    Location(AnalysisState state, int id, ValueType type, String description, Object extension) {
        this.state = state;
        this.id = id;
        this.type = type;
        this.description = description;
        this.extension = extension;
    }

    public int id() {
        return id;
    }

    @Override
    public String toString() {
        return description;
    }

    public ValueType type() {
        return type;
    }

    public Object extension() {
        return extension;
    }

    public Node fieldNode(FieldReference field) {
        if (fieldNodes == null) {
            fieldNodes = new HashMap<>();
        }
        return fieldNodes.computeIfAbsent(field, k -> state.createNode(this + "." + field.getFieldName()));
    }

    public Node arrayItemNode() {
        if (arrayItemNode == null) {
            arrayItemNode = state.createNode(this + ".elem");
        }
        return arrayItemNode;
    }
}
