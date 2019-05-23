/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.model.classes;

import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.model.MethodReference;

public class VirtualTableProvider {
    Map<String, VirtualTable> virtualTables = new LinkedHashMap<>();

    VirtualTableProvider() {
    }

    public VirtualTableEntry lookup(MethodReference method) {
        VirtualTable vtable = virtualTables.get(method.getClassName());
        if (vtable == null) {
            return null;
        }
        return vtable.getEntry(method.getDescriptor());
    }

    public VirtualTable lookup(String className) {
        return virtualTables.get(className);
    }
}
