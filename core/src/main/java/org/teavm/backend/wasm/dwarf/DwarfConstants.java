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
package org.teavm.backend.wasm.dwarf;

public final class DwarfConstants {
    public static final int DWARF_VERSION = 5;

    public static final int DW_UT_COMPILE = 0x01;

    public static final int DW_TAG_COMPILE_UNIT = 0x11;
    public static final int DW_TAG_SUBPROGRAM = 0x2E;

    public static final int DW_AT_NAME = 0x03;
    public static final int DW_AT_STMT_LIST = 0x10;
    public static final int DW_AT_LOW_PC = 0x11;
    public static final int DW_AT_HIGH_PC = 0x12;
    public static final int DW_AT_PRODUCER = 0x25;

    public static final int DW_CHILDREN_YES = 1;
    public static final int DW_CHILDREN_NO = 0;

    public static final int DW_LNCT_PATH = 0x1;
    public static final int DW_LNCT_DIRECTORY_INDEX = 0x2;

    public static final int DW_FORM_ADDR = 0x01;
    public static final int DW_FORM_DATA2 = 0x05;
    public static final int DW_FORM_DATA4 = 0x06;
    public static final int DW_FORM_STRP = 0x0E;
    public static final int DW_FORM_SEC_OFFSET = 0x17;
    public static final int DW_FORM_LINE_STRP = 0x1F;

    public static final int DW_LNS_COPY = 0x01;
    public static final int DW_LNS_ADVANCE_PC = 0x02;
    public static final int DW_LNS_ADVANCE_LINE = 0x03;
    public static final int DW_LNS_SET_FILE = 0x04;
    public static final int DW_LNS_SET_COLUMN = 0x05;
    public static final int DW_LNS_NEGATE_STMT = 0x06;
    public static final int DW_LNS_SET_BASIC_BLOCK = 0x07;
    public static final int DW_LNS_CONST_ADD_PC = 0x08;
    public static final int DW_LNS_FIXED_ADVANCE_PC = 0x09;
    public static final int DW_LNS_SET_PROLOGUE_END = 0x0A;
    public static final int DW_LNS_SET_EPILOGUE_BEGIN = 0x0B;
    public static final int DW_LNS_SET_ISA = 0x0C;

    public static final int DW_LNE_END_SEQUENCE = 0x01;

    private DwarfConstants() {
    }
}
