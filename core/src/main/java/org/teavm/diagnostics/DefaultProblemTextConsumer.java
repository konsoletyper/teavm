/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.diagnostics;

import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class DefaultProblemTextConsumer implements ProblemTextConsumer {
    private StringBuilder sb = new StringBuilder();

    public void clear() {
        sb.setLength(0);
    }

    public String getText() {
        return sb.toString();
    }

    @Override
    public void append(String text) {
        sb.append(text);
    }

    @Override
    public void appendClass(String className) {
        sb.append(className);
    }

    @Override
    public void appendMethod(MethodReference method) {
        sb.append(method);
    }

    @Override
    public void appendField(FieldReference field) {
        sb.append(field);
    }

    @Override
    public void appendLocation(TextLocation location) {
        sb.append(location);
    }

    @Override
    public void appendType(ValueType type) {
        sb.append(type);
    }
}
