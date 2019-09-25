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

import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class VirtualTableEntry {
    private MethodDescriptor method;
    MethodReference implementor;
    private int index;

    VirtualTableEntry(MethodDescriptor method, MethodReference implementor, int index) {
        this.method = method;
        this.implementor = implementor;
        this.index = index;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public MethodReference getImplementor() {
        return implementor;
    }

    public int getIndex() {
        return index;
    }
}
