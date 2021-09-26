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
package org.teavm.newir.expr;

public enum IrBinaryOperation {
    IADD,
    ISUB,
    IMUL,
    IDIV,
    IDIV_SAFE,
    IREM,
    IREM_SAFE,

    LADD,
    LSUB,
    LMUL,
    LDIV,
    LDIV_SAFE,
    LREM,
    LREM_SAFE,
    LCMP,

    FADD,
    FSUB,
    FMUL,
    FDIV,
    FREM,
    FCMP,

    DADD,
    DSUB,
    DMUL,
    DDIV,
    DREM,
    DCMP,

    IEQ,
    INE,
    ILT,
    ILE,
    IGT,
    IGE,

    REF_EQ,
    REF_NE,

    IAND,
    IOR,
    IXOR,
    ISHL,
    ISHR,
    ISHRU,

    LAND,
    LOR,
    LXOR,
    LSHL,
    LSHR,
    LSHRU,

    LOGICAL_AND,
    LOGICAL_OR
}
