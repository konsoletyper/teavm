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

public final class DebugConstants {
    private DebugConstants() {
    }

    public static final int LOC_START = 0;
    public static final int LOC_END = 1;
    public static final int LOC_LINE = 2;
    public static final int LOC_FILE = 3;
    public static final int LOC_PTR = 4;
    public static final int LOC_USER = 10;

    public static final int CLASS_ROOT = 0;
    public static final int CLASS_CLASS = 1;
    public static final int CLASS_INTERFACE = 2;
    public static final int CLASS_ARRAY = 3;
    public static final int CLASS_BOOLEAN = 4;
    public static final int CLASS_BYTE = 5;
    public static final int CLASS_SHORT = 6;
    public static final int CLASS_CHAR = 7;
    public static final int CLASS_INT = 8;
    public static final int CLASS_LONG = 9;
    public static final int CLASS_FLOAT = 10;
    public static final int CLASS_DOUBLE = 11;
    public static final int CLASS_UNKNOWN = 12;

    public static final int FIELD_END = 0;
    public static final int FIELD_END_SEQUENCE = 1;
    public static final int FIELD_BOOLEAN = 2;
    public static final int FIELD_BYTE = 3;
    public static final int FIELD_SHORT = 4;
    public static final int FIELD_CHAR = 5;
    public static final int FIELD_INT = 6;
    public static final int FIELD_LONG = 7;
    public static final int FIELD_FLOAT = 8;
    public static final int FIELD_DOUBLE = 9;
    public static final int FIELD_OBJECT = 10;
    public static final int FIELD_ADDRESS = 11;
    public static final int FIELD_UNDEFINED = 12;

    public static final String SECTION_STRINGS = "teavm_str";
    public static final String SECTION_FILES = "teavm_file";
    public static final String SECTION_PACKAGES = "teavm_pkg";
    public static final String SECTION_CLASSES = "teavm_cls";
    public static final String SECTION_CLASS_LAYOUT = "teavm_cll";
    public static final String SECTION_METHODS = "teavm_mtd";
    public static final String SECTION_LINES = "teavm_line";
    public static final String SECTION_VARIABLES = "teavm_var";
}
