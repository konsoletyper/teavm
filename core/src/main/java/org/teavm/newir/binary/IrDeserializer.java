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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.teavm.newir.decl.IrReferenceType;
import org.teavm.newir.expr.IrArrayElementExpr;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCastExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrInstanceOfExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrOperation;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrProgram;
import org.teavm.newir.expr.IrSetArrayElementExpr;
import org.teavm.newir.expr.IrTupleComponentExpr;
import org.teavm.newir.expr.IrTupleExpr;
import org.teavm.newir.type.IrTupleType;
import org.teavm.newir.type.IrType;

public class IrDeserializer {
    private static OpcodeHandler[] opcodes = new OpcodeHandler[256];
    private IrPackedProgram packed;
    private int pointer;
    private Deque<IrExpr> stack = new ArrayDeque<>();
    private List<IrExpr> expressions = new ArrayList<>();
    private IrProgram program = new IrProgram();

    private ArrayList<IrBlockExpr> forwardBlocks = new ArrayList<>();
    private int currentBlockIndex;
    private ArrayList<IrLoopExpr> forwardLoops = new ArrayList<>();
    private int currentLoopIndex;
    
    static {
        operationOpcodes();
        arrayOpcodes();

        runtimeTypeCheckOpcodes();
        parameterOpcodes();
        tupleOpcodes();
        blockOpcodes();
        loopOpcodes();
    }
    
    private static void operationOpcodes() {
        operationOpcode(Opcodes.VOID, IrOperation.VOID);
        operationOpcode(Opcodes.START, IrOperation.START);
        operationOpcode(Opcodes.NULL, IrOperation.NULL);
        operationOpcode(Opcodes.UNREACHABLE, IrOperation.UNREACHABLE);
        operationOpcode(Opcodes.THROW_NPE, IrOperation.THROW_NPE);
        operationOpcode(Opcodes.THROW_AIIOBE, IrOperation.THROW_AIIOBE);
        operationOpcode(Opcodes.THROW_CCE, IrOperation.THROW_CCE);

        operationOpcode(Opcodes.BOOLEAN_TO_INT, IrOperation.BOOLEAN_TO_INT);
        operationOpcode(Opcodes.BYTE_TO_INT, IrOperation.BYTE_TO_INT);
        operationOpcode(Opcodes.SHORT_TO_INT, IrOperation.SHORT_TO_INT);
        operationOpcode(Opcodes.CHAR_TO_INT, IrOperation.CHAR_TO_INT);
        operationOpcode(Opcodes.INT_TO_BOOLEAN, IrOperation.INT_TO_BOOLEAN);
        operationOpcode(Opcodes.INT_TO_BYTE, IrOperation.INT_TO_BYTE);
        operationOpcode(Opcodes.INT_TO_SHORT, IrOperation.INT_TO_SHORT);
        operationOpcode(Opcodes.INT_TO_CHAR, IrOperation.INT_TO_CHAR);
        operationOpcode(Opcodes.INT_TO_LONG, IrOperation.INT_TO_LONG);
        operationOpcode(Opcodes.INT_TO_FLOAT, IrOperation.INT_TO_FLOAT);
        operationOpcode(Opcodes.INT_TO_DOUBLE, IrOperation.INT_TO_DOUBLE);
        operationOpcode(Opcodes.LONG_TO_INT, IrOperation.LONG_TO_INT);
        operationOpcode(Opcodes.LONG_TO_FLOAT, IrOperation.LONG_TO_FLOAT);
        operationOpcode(Opcodes.LONG_TO_DOUBLE, IrOperation.LONG_TO_DOUBLE);
        operationOpcode(Opcodes.FLOAT_TO_INT, IrOperation.FLOAT_TO_INT);
        operationOpcode(Opcodes.FLOAT_TO_LONG, IrOperation.FLOAT_TO_LONG);
        operationOpcode(Opcodes.FLOAT_TO_DOUBLE, IrOperation.FLOAT_TO_DOUBLE);
        operationOpcode(Opcodes.DOUBLE_TO_INT, IrOperation.DOUBLE_TO_INT);
        operationOpcode(Opcodes.DOUBLE_TO_LONG, IrOperation.DOUBLE_TO_LONG);
        operationOpcode(Opcodes.DOUBLE_TO_FLOAT, IrOperation.DOUBLE_TO_FLOAT);

        operationOpcode(Opcodes.ARRAY_LENGTH, IrOperation.ARRAY_LENGTH);

        operationOpcode(Opcodes.IGNORE, IrOperation.IGNORE);

        operationOpcode(Opcodes.NULL_CHECK, IrOperation.NULL_CHECK);
        operationOpcode(Opcodes.NOT, IrOperation.NOT);

        operationOpcode(Opcodes.IINV, IrOperation.IINV);
        operationOpcode(Opcodes.LINV, IrOperation.LINV);

        operationOpcode(Opcodes.INEG, IrOperation.INEG);
        operationOpcode(Opcodes.LNEG, IrOperation.LNEG);
        operationOpcode(Opcodes.FNEG, IrOperation.FNEG);
        operationOpcode(Opcodes.DNEG, IrOperation.DNEG);

        operationOpcode(Opcodes.IADD, IrOperation.IADD);
        operationOpcode(Opcodes.ISUB, IrOperation.ISUB);
        operationOpcode(Opcodes.IMUL, IrOperation.IMUL);
        operationOpcode(Opcodes.IDIV, IrOperation.IDIV);
        operationOpcode(Opcodes.IREM, IrOperation.IREM);

        operationOpcode(Opcodes.LADD, IrOperation.LADD);
        operationOpcode(Opcodes.LSUB, IrOperation.LSUB);
        operationOpcode(Opcodes.LMUL, IrOperation.LMUL);
        operationOpcode(Opcodes.LDIV, IrOperation.LDIV);
        operationOpcode(Opcodes.LREM, IrOperation.LREM);
        operationOpcode(Opcodes.LCMP, IrOperation.LCMP);

        operationOpcode(Opcodes.FADD, IrOperation.FADD);
        operationOpcode(Opcodes.FSUB, IrOperation.FSUB);
        operationOpcode(Opcodes.FMUL, IrOperation.FMUL);
        operationOpcode(Opcodes.FDIV, IrOperation.FDIV);
        operationOpcode(Opcodes.FREM, IrOperation.FREM);
        operationOpcode(Opcodes.FCMP, IrOperation.FCMP);

        operationOpcode(Opcodes.DADD, IrOperation.DADD);
        operationOpcode(Opcodes.DSUB, IrOperation.DSUB);
        operationOpcode(Opcodes.DMUL, IrOperation.DMUL);
        operationOpcode(Opcodes.DDIV, IrOperation.DDIV);
        operationOpcode(Opcodes.DREM, IrOperation.DREM);
        operationOpcode(Opcodes.DCMP, IrOperation.DCMP);

        operationOpcode(Opcodes.IEQ, IrOperation.IEQ);
        operationOpcode(Opcodes.INE, IrOperation.INE);
        operationOpcode(Opcodes.ILT, IrOperation.ILT);
        operationOpcode(Opcodes.ILE, IrOperation.ILE);
        operationOpcode(Opcodes.IGT, IrOperation.IGT);
        operationOpcode(Opcodes.IGE, IrOperation.IGE);

        operationOpcode(Opcodes.REF_EQ, IrOperation.REF_EQ);
        operationOpcode(Opcodes.REF_NE, IrOperation.REF_NE);

        operationOpcode(Opcodes.IAND, IrOperation.IAND);
        operationOpcode(Opcodes.IOR, IrOperation.IOR);
        operationOpcode(Opcodes.IXOR, IrOperation.IXOR);
        operationOpcode(Opcodes.ISHL, IrOperation.ISHL);
        operationOpcode(Opcodes.ISHR, IrOperation.ISHR);
        operationOpcode(Opcodes.ISHRU, IrOperation.ISHRU);

        operationOpcode(Opcodes.LAND, IrOperation.LAND);
        operationOpcode(Opcodes.LOR, IrOperation.LOR);
        operationOpcode(Opcodes.LXOR, IrOperation.LXOR);
        operationOpcode(Opcodes.LSHL, IrOperation.LSHL);
        operationOpcode(Opcodes.LSHR, IrOperation.LSHR);
        operationOpcode(Opcodes.LSHRU, IrOperation.LSHRU);

        operationOpcode(Opcodes.LOGICAL_AND, IrOperation.LOGICAL_AND);
        operationOpcode(Opcodes.LOGICAL_OR, IrOperation.LOGICAL_OR);

        operationOpcode(Opcodes.UPPER_BOUND_CHECK, IrOperation.UPPER_BOUND_CHECK);
        operationOpcode(Opcodes.LOWER_BOUND_CHECK, IrOperation.LOWER_BOUND_CHECK);
    }

    private static void operationOpcode(int opcode, IrOperation operation) {
        setOpcode(opcode, deserializer -> deserializer.push(operation));
    }

    private static void arrayOpcodes() {
        arrayGetOpcode(Opcodes.ARRAYGET_BOOLEAN, IrType.BOOLEAN);
        arrayGetOpcode(Opcodes.ARRAYGET_BYTE, IrType.BYTE);
        arrayGetOpcode(Opcodes.ARRAYGET_SHORT, IrType.SHORT);
        arrayGetOpcode(Opcodes.ARRAYGET_CHAR, IrType.CHAR);
        arrayGetOpcode(Opcodes.ARRAYGET_INT, IrType.INT);
        arrayGetOpcode(Opcodes.ARRAYGET_LONG, IrType.LONG);
        arrayGetOpcode(Opcodes.ARRAYGET_FLOAT, IrType.FLOAT);
        arrayGetOpcode(Opcodes.ARRAYGET_DOUBLE, IrType.DOUBLE);
        arrayGetOpcode(Opcodes.ARRAYGET_OBJECT, IrType.OBJECT);

        arraySetOpcode(Opcodes.ARRAYSET_BOOLEAN, IrType.BOOLEAN);
        arraySetOpcode(Opcodes.ARRAYSET_BYTE, IrType.BYTE);
        arraySetOpcode(Opcodes.ARRAYSET_SHORT, IrType.SHORT);
        arraySetOpcode(Opcodes.ARRAYSET_CHAR, IrType.CHAR);
        arraySetOpcode(Opcodes.ARRAYSET_INT, IrType.INT);
        arraySetOpcode(Opcodes.ARRAYSET_LONG, IrType.LONG);
        arraySetOpcode(Opcodes.ARRAYSET_FLOAT, IrType.FLOAT);
        arraySetOpcode(Opcodes.ARRAYSET_DOUBLE, IrType.DOUBLE);
        arraySetOpcode(Opcodes.ARRAYSET_OBJECT, IrType.OBJECT);
    }

    private static void arrayGetOpcode(int opcode, IrType type) {
        setOpcode(opcode, deserializer -> deserializer.pushArrayGet(type));
    }

    private static void arraySetOpcode(int opcode, IrType type) {
        setOpcode(opcode, deserializer -> deserializer.pushArraySet(type));
    }

    private static void runtimeTypeCheckOpcodes() {
        setOpcode(Opcodes.INSTANCEOF, IrDeserializer::pushInstanceOf);
        setOpcode(Opcodes.CAST, IrDeserializer::pushCast);
    }

    private static void parameterOpcodes() {
        for (int i = Opcodes.PARAMETER_0; i < Opcodes.PARAMETER; ++i) {
            int index = i;
            setOpcode(i, deserializer -> deserializer.pushParameter(index - Opcodes.PARAMETER_0));
        }
        setOpcode(Opcodes.PARAMETER, deserializer -> deserializer.pushParameter(deserializer.readUnsignedInt()));
    }

    private static void tupleOpcodes() {
        for (int i = Opcodes.TUPLE_COMPONENT_0; i < Opcodes.TUPLE_COMPONENT; ++i) {
            int index = i - Opcodes.TUPLE_COMPONENT;
            setOpcode(i, deserializer -> deserializer.pushTupleComponent(index));
        }
        setOpcode(Opcodes.TUPLE_COMPONENT, deserializer -> deserializer.pushTupleComponent(
                deserializer.readUnsignedInt()));

        for (int i = Opcodes.TUPLE_2; i < Opcodes.TUPLE; ++i) {
            int size = i - Opcodes.TUPLE_2 + 2;
            setOpcode(i, deserializer -> deserializer.pushTuple(size));
        }
        setOpcode(Opcodes.TUPLE, deserializer -> deserializer.pushTuple(deserializer.readUnsignedInt()));
    }

    private static void blockOpcodes() {
        setOpcode(Opcodes.BLOCK, IrDeserializer::pushBlock);

        for (int i = Opcodes.EXIT_BLOCK_0; i < Opcodes.EXIT_BLOCK; ++i) {
            int target = i - Opcodes.EXIT_BLOCK_0;
            setOpcode(i, deserializer -> deserializer.pushExitBlock(target));
        }
        setOpcode(Opcodes.EXIT_BLOCK, deserializer -> deserializer.pushExitBlock(deserializer.readUnsignedInt()));
    }

    private static void loopOpcodes() {
        setOpcode(Opcodes.LOOP, IrDeserializer::pushLoop);

        for (int i = Opcodes.LOOP_CONTINUE_0; i < Opcodes.LOOP_CONTINUE; ++i) {
            int target = i - Opcodes.LOOP_CONTINUE_0;
            setOpcode(i, deserializer -> deserializer.pushContinueLoop(target));
        }
        setOpcode(Opcodes.LOOP_CONTINUE, deserializer -> deserializer.pushContinueLoop(deserializer.readUnsignedInt()));

        for (int i = Opcodes.LOOP_HEADER_0; i < Opcodes.LOOP_HEADER; ++i) {
            int target = i - Opcodes.LOOP_HEADER_0;
            setOpcode(i, deserializer -> deserializer.pushLoopHeader(target));
        }
        setOpcode(Opcodes.LOOP_HEADER, deserializer -> deserializer.pushLoopHeader(deserializer.readUnsignedInt()));
    }

    private static void setOpcode(int opcode, OpcodeHandler handler) {
        assert opcodes[opcode] == null;
        opcodes[opcode] = handler;
    }

    public IrProgram deserialize(IrPackedProgram packed) {
        this.packed = packed;
        int parameterCount = readUnsignedInt();
        IrType[] parameterTypes = new IrType[parameterCount];
        for (int i = 0; i < parameterCount; ++i) {
            parameterTypes[i] = readType();
        }
        program = new IrProgram(parameterTypes);
        int variableCount = readUnsignedInt();
        for (int i = 0; i < variableCount; ++i) {
            program.addVariable(readType());
        }

        while (pointer < packed.data.length) {
            readSingle();
        }
        program.setBody(stack.pop());
        assert stack.isEmpty();
        IrProgram result = program;
        program = null;

        forwardBlocks.clear();
        currentBlockIndex = 0;
        forwardLoops.clear();
        currentLoopIndex = 0;
        pointer = 0;
        expressions.clear();
        this.packed = null;

        return result;
    }
    
    private void readSingle() {
        int opcode = packed.data[pointer++];
        opcodes[opcode].accept(this);
    }
    
    private void push(IrOperation operation) {
        IrOperationExpr expr;
        switch (operation.getOperandCount()) {
            case 0:
                expr = IrOperationExpr.of(operation);
                break;
            case 1:
                expr = IrOperationExpr.of(operation, stack.pop());
                break;
            default: {
                IrExpr second = stack.pop();
                IrExpr first = stack.pop();
                expr = IrOperationExpr.of(operation, first, second);
                break;
            }
        }
        push(expr);
    }

    private void pushArrayGet(IrType type) {
        IrExpr index = stack.pop();
        IrExpr array = stack.pop();
        push(new IrArrayElementExpr(type, array, index));
    }

    private void pushArraySet(IrType type) {
        IrExpr value = stack.pop();
        IrExpr index = stack.pop();
        IrExpr array = stack.pop();
        push(new IrSetArrayElementExpr(type, array, index, value));
    }

    private void pushInstanceOf() {
        push(new IrInstanceOfExpr(stack.pop(), readReferenceType()));
    }

    private void pushCast() {
        push(new IrCastExpr(stack.pop(), readReferenceType()));
    }

    private void pushParameter(int index) {
        push(new IrParameterExpr(program.getParameter(index)));
    }

    private void pushTupleComponent(int index) {
        push(new IrTupleComponentExpr(stack.pop(), index));
    }

    private void pushTuple(int count) {
        IrExpr[] components = new IrExpr[count];
        for (int i = components.length - 1; i >= 0; --i) {
            components[i] = stack.pop();
        }
        push(IrTupleExpr.of(components));
    }

    private void pushBlock() {
        IrBlockExpr block = forwardBlocks.get(currentBlockIndex++);
        if (block == null) {
            block = new IrBlockExpr();
        }
        block.setBody(stack.pop());
        block.setPrevious(stack.pop());
        push(block);
    }

    private void pushExitBlock(int target) {
        target += currentBlockIndex;
        if (target >= forwardBlocks.size()) {
            forwardBlocks.addAll(Collections.nCopies(target - forwardBlocks.size() + 1, null));
        }
        IrBlockExpr block = forwardBlocks.get(target);
        if (block == null) {
            block = new IrBlockExpr();
            forwardBlocks.set(target, block);
        }
        IrExitBlockExpr result = new IrExitBlockExpr(stack.pop(), block);
        result.setPrevious(stack.pop());
        push(result);
    }

    private void pushLoop() {
        IrLoopExpr loop = forwardLoops.get(currentLoopIndex++);
        if (loop == null) {
            loop = new IrLoopExpr();
        }
        loop.setBody(stack.pop());
        loop.setPreheader(stack.pop());
        push(loop);
    }

    private void pushContinueLoop(int target) {
        IrContinueLoopExpr continueLoop = new IrContinueLoopExpr(stack.pop(), getLoop(target));
        continueLoop.setPrevious(stack.pop());
        push(continueLoop);
    }

    private void pushLoopHeader(int target) {
        push(getLoop(target).getHeader());
    }

    private IrLoopExpr getLoop(int target) {
        target += currentLoopIndex;
        if (target >= forwardLoops.size()) {
            forwardLoops.addAll(Collections.nCopies(target - forwardLoops.size() + 1, null));
        }
        IrLoopExpr loop = forwardLoops.get(target);
        if (loop == null) {
            loop = new IrLoopExpr();
            forwardLoops.set(target, loop);
        }
        return loop;
    }

    private IrReferenceType readReferenceType() {
        int tag = readUnsignedInt();
        int degree = tag >>= 4;
        tag &= 15;
        IrReferenceType type;
        switch (tag) {
            case 0:
                type = IrReferenceType.BOOLEAN_ARRAY;
                break;
            case 1:
                type = IrReferenceType.BYTE_ARRAY;
                break;
            case 2:
                type = IrReferenceType.SHORT_ARRAY;
                break;
            case 3:
                type = IrReferenceType.CHAR_ARRAY;
                break;
            case 4:
                type = IrReferenceType.INT_ARRAY;
                break;
            case 5:
                type = IrReferenceType.LONG_ARRAY;
                break;
            case 6:
                type = IrReferenceType.FLOAT_ARRAY;
                break;
            case 7:
                type = IrReferenceType.DOUBLE_ARRAY;
                break;
            case 8: {
                int classIndex = readUnsignedInt();
                type = packed.classes[classIndex].asType();
                break;
            }
            default:
                throw new IllegalStateException();
        }
        while (degree-- > 0) {
            type = type.arrayType();
        }
        return type;
    }

    private IrType readType() {
        int c = readUnsignedInt();
        switch (c) {
            case Opcodes.TYPE_BOOLEAN:
                return IrType.BOOLEAN;
            case Opcodes.TYPE_BYTE:
                return IrType.BYTE;
            case Opcodes.TYPE_SHORT:
                return IrType.SHORT;
            case Opcodes.TYPE_CHAR:
                return IrType.CHAR;
            case Opcodes.TYPE_INT:
                return IrType.INT;
            case Opcodes.TYPE_LONG:
                return IrType.LONG;
            case Opcodes.TYPE_FLOAT:
                return IrType.FLOAT;
            case Opcodes.TYPE_DOUBLE:
                return IrType.DOUBLE;
            case Opcodes.TYPE_OBJECT:
                return IrType.OBJECT;
            case Opcodes.TYPE_VOID:
                return IrType.VOID;
            case Opcodes.TYPE_ANY:
                return IrType.ANY;
            case Opcodes.TYPE_TUPLE_1:
                return IrTupleType.of(readType());
            case Opcodes.TYPE_TUPLE_2:
                return IrTupleType.of(readType(), readType());
            case Opcodes.TYPE_TUPLE_3:
                return IrTupleType.of(readType(), readType(), readType());
            case Opcodes.TYPE_TUPLE_4:
                return IrTupleType.of(readType(), readType(), readType(), readType());
            case Opcodes.TYPE_TUPLE_5:
                return IrTupleType.of(readType(), readType(), readType(), readType(), readType());
            case Opcodes.TYPE_TUPLE_6:
                return IrTupleType.of(readType(), readType(), readType(), readType(), readType(), readType());
            case Opcodes.TYPE_TUPLE_7:
                return IrTupleType.of(readType(), readType(), readType(), readType(), readType(), readType(),
                        readType());
            case Opcodes.TYPE_TUPLE_8:
                return IrTupleType.of(readType(), readType(), readType(), readType(), readType(), readType(),
                        readType(), readType());
            case Opcodes.TYPE_TUPLE: {
                int componentCount = readUnsignedInt();
                IrType[] components = new IrType[componentCount];
                for (int i = 0; i < componentCount; ++i) {
                    components[i] = readType();
                }
                return IrTupleType.of(components);
            }
            default:
                throw new IllegalStateException();
        }
    }

    private int readUnsignedInt() {
        int result = 0;
        int shift = 0;
        while (true) {
            int c = packed.data[pointer++] & 255;
            result |= (c & 127) << shift;
            if ((c & 128) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    private void push(IrExpr expr) {
        if (expr.needsOrdering()) {
            expr.setPrevious(stack.pop());
        }
        stack.push(expr);
        expressions.add(expr);
    }
    
    interface OpcodeHandler {
        void accept(IrDeserializer deserializer);
    }
}
