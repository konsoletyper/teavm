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

    public static final String SECTION_STRINGS = "teavm_str";
    public static final String SECTION_FILES = "teavm_file";
    public static final String SECTION_PACKAGES = "teavm_pkg";
    public static final String SECTION_CLASSES = "teavm_cls";
    public static final String SECTION_METHODS = "teavm_mtd";
    public static final String SECTION_LINES = "teavm_line";
    public static final String SECTION_VARIABLES = "teavm_var";
}
