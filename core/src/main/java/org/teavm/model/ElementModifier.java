/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.model;

import java.util.Set;

/**
 * Represents flags for classes and class members.
 * @see ElementHolder
 * @see AccessLevel
 * @author Alexey Andreev
 */
public enum ElementModifier {
    ABSTRACT,
    ANNOTATION,
    BRIDGE,
    DEPRECATED,
    ENUM,
    FINAL,
    INTERFACE,
    NATIVE,
    STATIC,
    STRICT,
    SUPER,
    SYNCHRONIZED,
    SYNTHETIC,
    TRANSIENT,
    VARARGS,
    VOLATILE;

    public static int pack(Set<ElementModifier> elementModifiers) {
        ElementModifier[] knownModifiers = ElementModifier.values();
        int value = 0;
        int bit = 1;
        for (int i = 0; i < knownModifiers.length; ++i) {
            ElementModifier modifier = knownModifiers[i];
            if (elementModifiers.contains(modifier)) {
                value |= bit;
            }
            bit <<= 1;
        }
        return value;
    }
}
