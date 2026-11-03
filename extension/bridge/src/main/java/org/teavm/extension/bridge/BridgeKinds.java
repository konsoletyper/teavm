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

/**
 * Constants used in bridge protocol to describe kinds of elements and values.
 */
public final class BridgeKinds {
    public static final int CLASS = 0;
    public static final int FIELD = 1;
    public static final int METHOD = 2;
    public static final int PARAMETER = 3;
    public static final int ANNOTATION = 4;
    public static final int TYPE_VARIABLE = 5;
    public static final int PARAMETERIZED_TYPE = 6;
    public static final int GENERIC_ARRAY = 7;
    public static final int TYPE_ARGUMENT = 8;

    public static final int CLASS_PRIMITIVE = 1;
    public static final int CLASS_INTERFACE = 2;
    public static final int CLASS_ARRAY = 4;
    public static final int CLASS_ANNOTATION = 8;
    public static final int CLASS_ENUM = 16;
    public static final int CLASS_RECORD = 32;

    public static final int VALUE_BOOLEAN = 0;
    public static final int VALUE_BYTE = 1;
    public static final int VALUE_SHORT = 2;
    public static final int VALUE_CHAR = 3;
    public static final int VALUE_INT = 4;
    public static final int VALUE_LONG = 5;
    public static final int VALUE_FLOAT = 6;
    public static final int VALUE_DOUBLE = 7;
    public static final int VALUE_STRING = 8;
    public static final int VALUE_CLASS = 9;
    public static final int VALUE_ENUM = 10;
    public static final int VALUE_ANNOTATION = 11;
    public static final int VALUE_ARRAY = 12;

    public static final int PROJECTION_EXACT = 0;
    public static final int PROJECTION_EXTENDS = 1;
    public static final int PROJECTION_SUPER = 2;

    private BridgeKinds() {
    }
}
