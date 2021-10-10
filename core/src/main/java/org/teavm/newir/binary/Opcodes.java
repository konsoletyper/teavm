/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.binary;

final class Opcodes {
    static final byte VOID = 0;
    static final byte NULL = 1;
    static final byte UNREACHABLE = 2;
    static final byte THROW_NPE = 3;
    static final byte THROW_AIIOBE = 4;
    static final byte THROW_CCE = 5;

    static final byte BOOLEAN_TO_INT = 6;
    static final byte BYTE_TO_INT = 7;
    static final byte SHORT_TO_INT = 8;
    static final byte CHAR_TO_INT = 9;
    static final byte INT_TO_BOOLEAN = 10;
    static final byte INT_TO_BYTE = 11;
    static final byte INT_TO_SHORT = 12;
    static final byte INT_TO_CHAR = 13;
    static final byte INT_TO_LONG = 14;
    static final byte INT_TO_FLOAT = 15;
    static final byte INT_TO_DOUBLE = 16;
    static final byte LONG_TO_INT = 17;
    static final byte LONG_TO_FLOAT = 18;
    static final byte LONG_TO_DOUBLE = 19;
    static final byte FLOAT_TO_INT = 20;
    static final byte FLOAT_TO_LONG = 21;
    static final byte FLOAT_TO_DOUBLE = 22;
    static final byte DOUBLE_TO_INT = 23;
    static final byte DOUBLE_TO_LONG = 24;
    static final byte DOUBLE_TO_FLOAT = 25;

    static final byte ARRAY_LENGTH = 26;

    static final byte IGNORE = 27;

    static final byte NULL_CHECK = 28;
    static final byte NOT = 29;

    static final byte IINV = 30;
    static final byte LINV = 31;

    static final byte INEG = 32;
    static final byte LNEG = 33;
    static final byte FNEG = 34;
    static final byte DNEG = 35;

    static final byte IADD = 36;
    static final byte ISUB = 37;
    static final byte IMUL = 38;
    static final byte IDIV = 39;
    static final byte IREM = 40;

    static final byte LADD = 41;
    static final byte LSUB = 42;
    static final byte LMUL = 43;
    static final byte LDIV = 44;
    static final byte LREM = 45;
    static final byte LCMP = 46;

    static final byte FADD = 47;
    static final byte FSUB = 48;
    static final byte FMUL = 49;
    static final byte FDIV = 50;
    static final byte FREM = 51;
    static final byte FCMP = 52;

    static final byte DADD = 53;
    static final byte DSUB = 54;
    static final byte DMUL = 55;
    static final byte DDIV = 56;
    static final byte DREM = 57;
    static final byte DCMP = 58;

    static final byte IEQ = 59;
    static final byte INE = 60;
    static final byte ILT = 61;
    static final byte ILE = 62;
    static final byte IGT = 64;
    static final byte IGE = 65;

    static final byte REF_EQ = 66;
    static final byte REF_NE = 67;

    static final byte IAND = 68;
    static final byte IOR = 69;
    static final byte IXOR = 70;
    static final byte ISHL = 71;
    static final byte ISHR = 72;
    static final byte ISHRU = 73;

    static final byte LAND = 74;
    static final byte LOR = 75;
    static final byte LXOR = 76;
    static final byte LSHL = 77;
    static final byte LSHR = 78;
    static final byte LSHRU = 79;

    static final byte LOGICAL_AND = 80;
    static final byte LOGICAL_OR = 81;

    static final byte UPPER_BOUND_CHECK = 82;
    static final byte LOWER_BOUND_CHECK = 83;

    static final byte ARRAYGET_BOOLEAN = 84;
    static final byte ARRAYGET_BYTE = 85;
    static final byte ARRAYGET_SHORT = 86;
    static final byte ARRAYGET_CHAR = 87;
    static final byte ARRAYGET_INT = 88;
    static final byte ARRAYGET_LONG = 89;
    static final byte ARRAYGET_FLOAT = 90;
    static final byte ARRAYGET_DOUBLE = 91;
    static final byte ARRAYGET_OBJECT = 92;

    static final byte ARRAYSET_BOOLEAN = 93;
    static final byte ARRAYSET_BYTE = 94;
    static final byte ARRAYSET_SHORT = 95;
    static final byte ARRAYSET_CHAR = 96;
    static final byte ARRAYSET_INT = 97;
    static final byte ARRAYSET_LONG = 98;
    static final byte ARRAYSET_FLOAT = 99;
    static final byte ARRAYSET_DOUBLE = 100;
    static final byte ARRAYSET_OBJECT = 101;
    
    static final byte INSTANCEOF = 102;
    static final byte CAST = 103;
    static final byte PARAMETER_0 = (byte) 170;
    static final byte PARAMETER = (byte) 174;
    static final byte TUPLE_COMPONENT = 104;
    static final byte TUPLE = 105;
    static final byte TUPLE_2 = (byte) 156;
    static final byte TUPLE_COMPONENT_0 = (byte) 162;
    static final byte BLOCK = 106;
    static final byte EXIT_BLOCK_0 = 107;
    static final byte EXIT_BLOCK = (byte) 151;
    static final byte LOOP = 108;
    static final byte LOOP_EXIT_0 = 109;
    static final byte LOOP_EXIT = (byte) 153;
    static final byte LOOP_CONTINUE_0 = 110;
    static final byte LOOP_CONTINUE = (byte) 154;
    static final byte LOOP_HEADER_0 = 111;
    static final byte LOOP_HEADER = (byte) 155;
    static final byte CONDITIONAL = 112;
    static final byte SWITCH = 113;
    static final byte TRY_CATCH = 114;
    static final byte TRY_CATCH_START_0 = 115;
    static final byte TRY_CATCH_START = (byte) 148;
    static final byte CAUGHT_VALUE_0 = 116;
    static final byte CAUGHT_VALUE = (byte) 149;
    static final byte SET_CAUGHT_VALUE_0 = (byte) 146;
    static final byte SET_CAUGHT_VALUE = (byte) 147;
    static final byte CAUGHT_EXCEPTION_0 = 117;
    static final byte CAUGHT_EXCEPTION = (byte) 150;
    static final byte THROW = 118;
    static final byte GET_VAR = 119;
    static final byte SET_VAR = 120;
    static final byte GET_GLOBAL = 121;
    static final byte SET_GLOBAL = 122;
    static final byte GET_FIELD = 123;
    static final byte SET_FIELD = 124;
    static final byte CALL_FUNCTION = 125;
    static final byte CALL_METHOD = 126;
    static final byte NEW = 127;
    static final byte NEW_ARRAY = (byte) 128;
    static final byte NEW_ARRAY_MULTI = (byte) 129;
    
    static final byte INT_CONST = (byte) 130;
    static final byte LONG_CONST = (byte) 131;
    static final byte FLOAT_CONST = (byte) 132;
    static final byte DOUBLE_CONST = (byte) 133;
    static final byte STRING_CONST = (byte) 134;
    static final byte TYPE_CONST = (byte) 135;
    
    static final byte INT_CONST_0 = (byte) 136;
    static final byte INT_CONST_1 = (byte) 137;
    static final byte INT_CONST_2 = (byte) 138;
    static final byte INT_CONST_M1 = (byte) 139;
    static final byte LONG_CONST_0 = (byte) 136;
    static final byte LONG_CONST_1 = (byte) 137;
    static final byte LONG_CONST_2 = (byte) 138;
    static final byte LONG_CONST_M1 = (byte) 139;
    static final byte FLOAT_CONST_0 = (byte) 140;
    static final byte DOUBLE_CONST_0 = (byte) 141;
    
    static final byte BACK_REF = (byte) 142;
    static final byte BACK_REF_1 = (byte) 143;
    static final byte BACK_REF_2 = (byte) 144;
    static final byte BACK_REF_3 = (byte) 145;

    static final byte TYPE_BOOLEAN = 0;
    static final byte TYPE_BYTE = 1;
    static final byte TYPE_SHORT = 2;
    static final byte TYPE_CHAR = 3;
    static final byte TYPE_INT = 4;
    static final byte TYPE_LONG = 5;
    static final byte TYPE_FLOAT = 6;
    static final byte TYPE_DOUBLE = 7;
    static final byte TYPE_OBJECT = 8;
    static final byte TYPE_VOID = 9;
    static final byte TYPE_UNREACHABLE = 10;
    static final byte TYPE_ANY = 11;
    static final byte TYPE_TUPLE_1 = 12;
    static final byte TYPE_TUPLE = 20;

    private Opcodes() {
    } 
}
