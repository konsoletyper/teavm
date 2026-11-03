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
package org.teavm.extension.bridge;

public interface BridgeClass extends BridgeGenericDeclaration, BridgeType {
    String name();

    String simpleName();

    int modifiers();

    /**
     * Combination of {@code BridgeKinds.CLASS_*} flags.
     */
    int flags();

    BridgeClass componentType();

    BridgeClass arrayType();

    BridgeClass superclass();

    BridgeClass[] interfaces();

    BridgeField[] enumConstants();

    boolean isAssignableFrom(BridgeClass cls);

    BridgeMethod[] declaredMethods();

    BridgeMethod[] methods();

    BridgeMethod declaredMethod(String name, BridgeClass[] parameterTypes);

    BridgeMethod method(String name, BridgeClass[] parameterTypes);

    BridgeField[] declaredFields();

    BridgeField[] fields();

    BridgeField declaredField(String name);

    BridgeField field(String name);
}
