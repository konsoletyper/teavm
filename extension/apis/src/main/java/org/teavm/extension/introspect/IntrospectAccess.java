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
package org.teavm.extension.introspect;

import java.lang.reflect.Modifier;

public enum IntrospectAccess {
    PUBLIC,
    PROTECTED,
    PRIVATE,
    PACKAGE_PRIVATE;

    static IntrospectAccess fromModifiers(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return PUBLIC;
        } else if (Modifier.isProtected(modifiers)) {
            return PROTECTED;
        } else if (Modifier.isPrivate(modifiers)) {
            return PRIVATE;
        } else {
            return PACKAGE_PRIVATE;
        }
    }
}
