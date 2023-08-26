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

    public static final int DW_TAG_CLASS_TYPE = 0x02;
    public static final int DW_TAG_FORMAL_PARAMETER = 0x05;
    public static final int DW_TAG_MEMBER = 0x0d;
    public static final int DW_TAG_POINTER_TYPE = 0x0F;
    public static final int DW_TAG_COMPILE_UNIT = 0x11;
    public static final int DW_TAG_STRUCTURE_TYPE = 0x13;
    public static final int DW_TAG_INHERITANCE = 0x1C;
    public static final int DW_TAG_BASE_TYPE = 0x24;
    public static final int DW_TAG_SUBPROGRAM = 0x2E;
    public static final int DW_TAG_VARIABLE = 0x34;
    public static final int DW_TAG_NAMESPACE = 0x39;
    public static final int DW_TAG_UNSPECIFIED_TYPE = 0x3B;

    public static final int DW_AT_LOCATION = 0x02;
    public static final int DW_AT_NAME = 0x03;
    public static final int DW_AT_BYTE_SIZE = 0x0B;
    public static final int DW_AT_STMT_LIST = 0x10;
    public static final int DW_AT_LOW_PC = 0x11;
    public static final int DW_AT_HIGH_PC = 0x12;
    public static final int DW_AT_LANGUAGE = 0x13;
    public static final int DW_AT_CONTAINING_TYPE = 0x1D;
    public static final int DW_AT_PRODUCER = 0x25;
    public static final int DW_AT_ACCESSIBILITY = 0x32;
    public static final int DW_AT_CALLING_CONVENTION = 0x36;
    public static final int DW_AT_DATA_MEMBER_LOCATION = 0x38;
    public static final int DW_AT_DECLARATION = 0x3C;
    public static final int DW_AT_ENCODING = 0x3E;
    public static final int DW_AT_SPECIFICATION = 0x47;
    public static final int DW_AT_TYPE = 0x49;
    public static final int DW_AT_DATA_LOCATION = 0x50;
    public static final int DW_AT_LINKAGE_NAME = 0x6E;

    public static final int DW_ATE_ADDRESS = 0x01;
    public static final int DW_ATE_BOOLEAN = 0x02;
    public static final int DW_ATE_FLOAT = 0x04;
    public static final int DW_ATE_SIGNED = 0x05;
    public static final int DW_ATE_UNSIGNED = 0x07;
    public static final int DW_ATE_UNSIGNED_CHAR = 0x08;
    public static final int DW_ATE_UTF = 0x10;

    public static final int DW_LANG_JAVA = 0x0b;
    public static final int DW_LANG_C_PLUS_PLUS = 0x04;

    public static final int DW_CHILDREN_YES = 1;
    public static final int DW_CHILDREN_NO = 0;

    public static final int DW_LNCT_PATH = 0x1;
    public static final int DW_LNCT_DIRECTORY_INDEX = 0x2;

    public static final int DW_FORM_ADDR = 0x01;
    public static final int DW_FORM_DATA2 = 0x05;
    public static final int DW_FORM_DATA4 = 0x06;
    public static final int DW_FORM_DATA1 = 0x0B;
    public static final int DW_FORM_FLAG = 0x0C;
    public static final int DW_FORM_STRP = 0x0E;
    public static final int DW_FORM_REF4 = 0x13;
    public static final int DW_FORM_SEC_OFFSET = 0x17;
    public static final int DW_FORM_EXPRLOC = 0x18;
    public static final int DW_FORM_FLAG_PRESENT = 0x19;
    public static final int DW_FORM_LINE_STRP = 0x1F;

    public static final int DW_OP_ADDR = 0x03;
    public static final int DW_OP_DEREF = 0x06;
    public static final int DW_OP_SHL = 0x24;
    public static final int DW_OP_LIT0 = 0x30;
    public static final int DW_OP_LIT3 = 0x33;
    public static final int DW_OP_PUSH_OBJECT_ADDRESS = 0x97;
    public static final int DW_OP_STACK_VALUE = 0x9F;
    public static final int DW_OP_WASM_LOCATION = 0xED;

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

    public static final int DW_ACCESS_PUBLIC = 0x01;

    public static final int DW_CC_PASS_BY_REFERENCE = 0x04;

    private DwarfConstants() {
    }
}
