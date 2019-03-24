/*
 *  Copyright 2019 konsoletyper.
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

public class GenericTypeParameter {
    private String name;
    private GenericValueType.Reference classBound;
    private GenericValueType.Reference[] interfaceBounds;

    public GenericTypeParameter(String name, GenericValueType.Reference classBound,
            GenericValueType.Reference[] interfaceBounds) {
        this.name = name;
        this.classBound = classBound;
        this.interfaceBounds = interfaceBounds.clone();
    }

    public String getName() {
        return name;
    }

    public GenericValueType.Reference getClassBound() {
        return classBound;
    }

    public GenericValueType.Reference[] getInterfaceBounds() {
        return interfaceBounds.clone();
    }
}
