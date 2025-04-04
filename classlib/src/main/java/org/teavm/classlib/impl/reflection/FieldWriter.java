/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import org.teavm.platform.Platform;

public interface FieldWriter {
    void write(Object instance, Object value);

    static FieldWriter forJs(JSFieldSetter setter) {
        if (setter == null) {
            return null;
        }
        return (instance, value) -> setter.set(Platform.getPlatformObject(instance), Converter.fromJava(value));
    }
}
