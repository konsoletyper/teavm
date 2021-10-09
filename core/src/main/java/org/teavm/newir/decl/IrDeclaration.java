/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.decl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class IrDeclaration<D extends IrDeclaration<D>> {
    private Map<IrAttribute<?, ?>, Object> attributes;
    private String nameHint;

    @SuppressWarnings("unchecked")
    public <V> V getAttribute(IrAttribute<V, D> attribute) {
        if (attributes == null) {
            return attribute.defaultValue;
        }
        Object result = attributes.get(attribute);
        if (result == null) {
            return attribute.defaultValue;
        }
        return (V) result;
    }

    public <V> void setAttribute(IrAttribute<V, D> attribute, V value) {
        if (Objects.equals(attribute.defaultValue, value)) {
            if (attributes != null) {
                attributes.remove(attribute);
                if (attributes.isEmpty()) {
                    attributes = null;
                }
            }
        } else {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(attribute, value);
        }
    }

    public String getNameHint() {
        return nameHint;
    }

    public void setNameHint(String nameHint) {
        this.nameHint = nameHint;
    }
}
