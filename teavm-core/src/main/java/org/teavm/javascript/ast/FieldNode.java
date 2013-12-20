/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.javascript.ast;

import java.util.EnumSet;
import java.util.Set;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class FieldNode {
    private String name;
    private ValueType type;
    private Set<NodeModifier> modifiers = EnumSet.noneOf(NodeModifier.class);
    private Object initialValue;

    public FieldNode(String name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Set<NodeModifier> getModifiers() {
        return modifiers;
    }

    public ValueType getType() {
        return type;
    }

    public Object getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
    }
}
