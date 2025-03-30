/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc.classes;

public final class WasmGCClassFlags {
    public static final int ABSTRACT = 1;
    public static final int INTERFACE = 1 << 1;
    public static final int FINAL = 1 << 2;
    public static final int ENUM = 1 << 3;
    public static final int ANNOTATION = 1 << 4;
    public static final int SYNTHETIC = 1 << 5;
    public static final int BRIDGE = 1 << 6;
    public static final int DEPRECATED = 1 << 7;
    public static final int NATIVE = 1 << 8;
    public static final int STATIC = 1 << 9;
    public static final int STRICT = 1 << 10;
    public static final int SYNCHRONIZED = 1 << 11;
    public static final int TRANSIENT = 1 << 12;
    public static final int VARARGS = 1 << 13;
    public static final int VOLATILE = 1 << 14;
    public static final int PRIMITIVE = 1 << 15;
    public static final int ARRAY = 1 << 16;
    public static final int INHERITED_ANNOTATIONS = 1 << 17;

    public static final int PRIMITIVE_KIND_SHIFT = 18;
    public static final int PRIMITIVE_BOOLEAN = 0;
    public static final int PRIMITIVE_BYTE = 1;
    public static final int PRIMITIVE_SHORT = 2;
    public static final int PRIMITIVE_CHAR = 3;
    public static final int PRIMITIVE_INT = 4;
    public static final int PRIMITIVE_LONG = 5;
    public static final int PRIMITIVE_FLOAT = 6;
    public static final int PRIMITIVE_DOUBLE = 7;
    public static final int PRIMITIVE_VOID = 8;

    private WasmGCClassFlags() {
    }
}
