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

import java.util.Arrays;
import org.teavm.newir.type.IrType;

public enum IrOperation {
    NULL(false, IrType.OBJECT),
    VOID(false, IrType.VOID),
    START(false, IrType.VOID),
    UNREACHABLE(true, IrType.UNREACHABLE),
    THROW_NPE(true, IrType.UNREACHABLE),
    THROW_AIIOBE(true, IrType.UNREACHABLE),
    THROW_CCE(true, IrType.UNREACHABLE),

    BOOLEAN_TO_INT(false, IrType.INT, IrType.BOOLEAN),
    BYTE_TO_INT(false, IrType.INT, IrType.BYTE),
    SHORT_TO_INT(false, IrType.INT, IrType.SHORT),
    CHAR_TO_INT(false, IrType.INT, IrType.CHAR),
    INT_TO_BOOLEAN(false, IrType.BOOLEAN, IrType.INT),
    INT_TO_BYTE(false, IrType.BYTE, IrType.INT),
    INT_TO_SHORT(false, IrType.SHORT, IrType.INT),
    INT_TO_CHAR(false, IrType.CHAR, IrType.INT),
    INT_TO_LONG(false, IrType.LONG, IrType.INT),
    INT_TO_FLOAT(false, IrType.FLOAT, IrType.INT),
    INT_TO_DOUBLE(false, IrType.DOUBLE, IrType.INT),
    LONG_TO_INT(false, IrType.INT, IrType.LONG),
    LONG_TO_FLOAT(false, IrType.FLOAT, IrType.LONG),
    LONG_TO_DOUBLE(false, IrType.DOUBLE, IrType.LONG),
    FLOAT_TO_INT(false, IrType.INT, IrType.FLOAT),
    FLOAT_TO_LONG(false, IrType.LONG, IrType.FLOAT),
    FLOAT_TO_DOUBLE(false, IrType.DOUBLE, IrType.FLOAT),
    DOUBLE_TO_INT(false, IrType.INT, IrType.DOUBLE),
    DOUBLE_TO_LONG(false, IrType.LONG, IrType.DOUBLE),
    DOUBLE_TO_FLOAT(false, IrType.FLOAT, IrType.DOUBLE),

    ARRAY_LENGTH(false, IrType.INT, IrType.OBJECT),

    IGNORE(false, IrType.VOID, IrType.ANY),

    NULL_CHECK(true, IrType.OBJECT, IrType.OBJECT),
    NOT(false, IrType.BOOLEAN, IrType.BOOLEAN),

    IINV(IrType.INT, 1),
    LINV(IrType.LONG, 1),

    INEG(IrType.INT, 1),
    LNEG(IrType.LONG, 1),
    FNEG(IrType.FLOAT, 1),
    DNEG(IrType.DOUBLE, 1),

    IADD(IrType.INT, 2),
    ISUB(IrType.INT, 2),
    IMUL(IrType.INT, 2),
    IDIV(true, IrType.INT, IrType.INT, IrType.INT),
    IREM(true, IrType.INT, IrType.INT, IrType.INT),

    LADD(IrType.LONG, 2),
    LSUB(IrType.LONG, 2),
    LMUL(IrType.LONG, 2),
    LDIV(true, IrType.LONG, IrType.LONG, IrType.LONG),
    LREM(true, IrType.LONG, IrType.LONG, IrType.LONG),
    LCMP(false, IrType.INT, IrType.LONG, IrType.LONG),

    FADD(IrType.FLOAT, 2),
    FSUB(IrType.FLOAT, 2),
    FMUL(IrType.FLOAT, 2),
    FDIV(IrType.FLOAT, 2),
    FREM(IrType.FLOAT, 2),
    FCMP(false, IrType.INT, IrType.FLOAT, IrType.FLOAT),

    DADD(IrType.DOUBLE, 2),
    DSUB(IrType.DOUBLE, 2),
    DMUL(IrType.DOUBLE, 2),
    DDIV(IrType.DOUBLE, 2),
    DREM(IrType.DOUBLE, 2),
    DCMP(false, IrType.INT, IrType.DOUBLE, IrType.DOUBLE),

    IEQ(false, IrType.BOOLEAN, IrType.INT, IrType.INT),
    INE(false, IrType.BOOLEAN, IrType.INT, IrType.INT),
    ILT(false, IrType.BOOLEAN, IrType.INT, IrType.INT),
    ILE(false, IrType.BOOLEAN, IrType.INT, IrType.INT),
    IGT(false, IrType.BOOLEAN, IrType.INT, IrType.INT),
    IGE(false, IrType.BOOLEAN, IrType.INT, IrType.INT),

    REF_EQ(false, IrType.BOOLEAN, IrType.OBJECT, IrType.OBJECT),
    REF_NE(false, IrType.BOOLEAN, IrType.OBJECT, IrType.OBJECT),

    IAND(IrType.INT, 2),
    IOR(IrType.INT, 2),
    IXOR(IrType.INT, 2),
    ISHL(IrType.INT, 2),
    ISHR(IrType.INT, 2),
    ISHRU(IrType.INT, 2),

    LAND(IrType.LONG, 2),
    LOR(IrType.LONG, 2),
    LXOR(IrType.LONG, 2),
    LSHL(false, IrType.LONG, IrType.LONG, IrType.INT),
    LSHR(false, IrType.LONG, IrType.LONG, IrType.INT),
    LSHRU(false, IrType.LONG, IrType.LONG, IrType.INT),

    LOGICAL_AND(IrType.BOOLEAN, 2),
    LOGICAL_OR(IrType.BOOLEAN, 2),

    UPPER_BOUND_CHECK(true, IrType.OBJECT, IrType.OBJECT, IrType.INT),
    LOWER_BOUND_CHECK(true, IrType.OBJECT, IrType.OBJECT);

    private final boolean needsOrdering;
    private final IrType type;
    private final IrType[] operandTypes;

    IrOperation(boolean needsOrdering, IrType type, IrType... operandTypes) {
        this.needsOrdering = needsOrdering;
        this.type = type;
        this.operandTypes = operandTypes;
    }

    IrOperation(IrType type, int operands) {
        this(false, type, repeat(type, operands));
    }

    public boolean needsOrdering() {
        return needsOrdering;
    }

    public IrType getType() {
        return type;
    }

    public int getOperandCount() {
        return operandTypes.length;
    }

    public IrType getOperandType(int index) {
        return operandTypes[index];
    }

    private static IrType[] repeat(IrType type, int count) {
        IrType[] result = new IrType[count];
        Arrays.fill(result, type);
        return result;
    }
}
