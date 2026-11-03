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

import org.teavm.jso.JSObject;

/**
 * Value of annotation attribute.
 */
public interface BridgeValue extends JSObject {
    /**
     * One of {@code BridgeKinds.VALUE_*} constants.
     */
    int kind();

    /**
     * Value of boolean (1 or 0), byte, short, char, int, float or double.
     */
    double number();

    /**
     * Value of string, or decimal representation of long value.
     */
    String string();

    /**
     * Class (for class value), field (for enum value) or annotation (for annotation value).
     */
    BridgeElement element();

    /**
     * Kind of array items, one of {@code BridgeKinds.VALUE_*} constants.
     */
    int itemKind();

    BridgeValue[] items();
}
