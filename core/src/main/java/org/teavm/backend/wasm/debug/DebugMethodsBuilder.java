/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import org.teavm.model.MethodReference;

public class DebugMethodsBuilder extends DebugSectionBuilder implements DebugMethods {
    private DebugClasses classes;
    private DebugStrings strings;
    private ObjectIntMap<MethodReference> methods = new ObjectIntHashMap<>();

    public DebugMethodsBuilder(DebugClasses classes, DebugStrings strings) {
        super(DebugConstants.SECTION_METHODS);
        this.classes = classes;
        this.strings = strings;
    }

    @Override
    public int methodPtr(MethodReference method) {
        var result = methods.getOrDefault(method, -1);
        if (result < 0) {
            result = methods.size();
            methods.put(method, result);
            blob.writeLEB(classes.classPtr(method.getClassName()));
            blob.writeLEB(strings.stringPtr(method.getName()));
        }
        return result;
    }
}
