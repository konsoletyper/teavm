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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.newir.decl.IrClass;
import org.teavm.newir.decl.IrField;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.decl.IrGlobal;
import org.teavm.newir.decl.IrMethod;
import org.teavm.newir.decl.IrObjectType;
import org.teavm.newir.decl.IrReferenceArrayType;
import org.teavm.newir.decl.IrReferenceType;
import org.teavm.newir.decl.IrReferenceTypeKind;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrCastExpr;
import org.teavm.newir.expr.IrCaughtExceptionExpr;
import org.teavm.newir.expr.IrCaughtValueExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrDoubleConstantExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrExprTags;
import org.teavm.newir.expr.IrExprVisitor;
import org.teavm.newir.expr.IrFloatConstantExpr;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetGlobalExpr;
import org.teavm.newir.expr.IrGetVariableExpr;
import org.teavm.newir.expr.IrInstanceOfExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLongConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrLoopHeaderExpr;
import org.teavm.newir.expr.IrNewObjectExpr;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSetCaughtValueExpr;
import org.teavm.newir.expr.IrSetFieldExpr;
import org.teavm.newir.expr.IrSetGlobalExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrStringConstantExpr;
import org.teavm.newir.expr.IrThrowExpr;
import org.teavm.newir.expr.IrTryCatchExpr;
import org.teavm.newir.expr.IrTryCatchStartExpr;
import org.teavm.newir.expr.IrTupleComponentExpr;
import org.teavm.newir.expr.IrTupleExpr;
import org.teavm.newir.util.RecursiveIrExprVisitor;

public class IrSerializer extends RecursiveIrExprVisitor {
    private byte[] output = new byte[128];
    private int pointer;
    private int index;
    private int tryCatchLevel;
    private int blockLevel;
    private int loopLevel;
    private IrExprTags<ExprData> tags = new IrExprTags<>(expr -> new ExprData());
    private ObjectIntMap<String> stringIndexes = new ObjectIntHashMap<>();
    private List<String> strings = new ArrayList<>();
    private ObjectIntMap<IrFunction> functionIndexes = new ObjectIntHashMap<>();
    private List<IrFunction> functions = new ArrayList<>();
    private ObjectIntMap<IrMethod> methodIndexes = new ObjectIntHashMap<>();
    private List<IrMethod> methods = new ArrayList<>();
    private ObjectIntMap<IrField> fieldIndexes = new ObjectIntHashMap<>();
    private List<IrField> fields = new ArrayList<>();
    private ObjectIntMap<IrClass> classIndexes = new ObjectIntHashMap<>();
    private List<IrClass> classes = new ArrayList<>();
    private ObjectIntMap<IrGlobal> globalIndexes = new ObjectIntHashMap<>();
    private List<IrGlobal> globals = new ArrayList<>();

    public IrPackedTree serialize(IrExpr expr) {
        serializeExpr(expr);
        IrPackedTree packedProgram = new IrPackedTree(Arrays.copyOf(output, pointer), strings.toArray(new String[0]),
                functions.toArray(new IrFunction[0]), methods.toArray(new IrMethod[0]), fields.toArray(new IrField[0]),
                classes.toArray(new IrClass[0]), globals.toArray(new IrGlobal[0]));
        pointer = 0;
        stringIndexes.clear();
        functionIndexes.clear();
        methodIndexes.clear();
        fieldIndexes.clear();
        classIndexes.clear();
        globalIndexes.clear();
        strings.clear();
        functions.clear();
        methods.clear();
        fields.clear();
        classes.clear();
        globals.clear();
        tags.cleanup();
        return packedProgram;
    }

    private void serializeExpr(IrExpr expr) {
        if (trySerializeExisting(expr)) {
            return;
        }

        while (expr.getDependencyCount() > 0) {
            IrExpr prev = expr.getDependency(0);
            if (trySerializeExisting(prev)) {
                break;
            }
            ExprData data = tags.get(prev);
            data.next = expr;
            expr = prev;
        }

        expr.acceptVisitor(enterVisitor);
        while (true) {
            ExprData data = tags.get(expr);
            data.index = index;
            expr.acceptVisitor(this);
            index++;

            expr = data.next;
            if (expr == null) {
                break;
            }
            expr.acceptVisitor(enterVisitor);
            for (int i = 1; i < expr.getDependencyCount(); ++i) {
                serializeExpr(expr.getDependency(i));
            }
        }
    }

    private boolean trySerializeExisting(IrExpr expr) {
        ExprData data = tags.get(expr);
        if (data.index >= 0 && !data.noBackref) {
            int diff = pointer - data.index;
            switch (diff) {
                case 1:
                    writeSingleByte(Opcodes.BACK_REF_1);
                    break;
                case 2:
                    writeSingleByte(Opcodes.BACK_REF_2);
                    break;
                case 3:
                    writeSingleByte(Opcodes.BACK_REF_3);
                    break;
                default:
                    writeSingleByte(Opcodes.BACK_REF);
                    writeUnsignedInt(diff - 1);
                    break;
            }
            data.index = index++;
            return true;
        }
        return false;
    }

    @Override
    public void visit(IrOperationExpr expr) {
        ExprData data = tags.get(expr);
        switch (expr.getOperation()) {
            case VOID:
                writeSingleByte(Opcodes.VOID);
                data.noBackref = true;
                break;
            case NULL:
                writeSingleByte(Opcodes.NULL);
                break;
            case UNREACHABLE:
                writeSingleByte(Opcodes.UNREACHABLE);
                break;
            case THROW_NPE:
                writeSingleByte(Opcodes.THROW_NPE);
                break;
            case THROW_CCE:
                writeSingleByte(Opcodes.THROW_CCE);
                break;
            case THROW_AIIOBE:
                writeSingleByte(Opcodes.THROW_AIIOBE);
                break;

            case BOOLEAN_TO_INT:
                writeSingleByte(Opcodes.BOOLEAN_TO_INT);
                break;
            case BYTE_TO_INT:
                writeSingleByte(Opcodes.BYTE_TO_INT);
                break;
            case SHORT_TO_INT:
                writeSingleByte(Opcodes.SHORT_TO_INT);
                break;
            case CHAR_TO_INT:
                writeSingleByte(Opcodes.CHAR_TO_INT);
                break;
            case INT_TO_BOOLEAN:
                writeSingleByte(Opcodes.INT_TO_BOOLEAN);
                break;
            case INT_TO_BYTE:
                writeSingleByte(Opcodes.INT_TO_BYTE);
                break;
            case INT_TO_SHORT:
                writeSingleByte(Opcodes.INT_TO_SHORT);
                break;
            case INT_TO_CHAR:
                writeSingleByte(Opcodes.INT_TO_CHAR);
                break;
            case INT_TO_LONG:
                writeSingleByte(Opcodes.INT_TO_LONG);
                break;
            case INT_TO_FLOAT:
                writeSingleByte(Opcodes.INT_TO_FLOAT);
                break;
            case INT_TO_DOUBLE:
                writeSingleByte(Opcodes.INT_TO_DOUBLE);
                break;
            case LONG_TO_INT:
                writeSingleByte(Opcodes.LONG_TO_INT);
                break;
            case LONG_TO_FLOAT:
                writeSingleByte(Opcodes.LONG_TO_FLOAT);
                break;
            case LONG_TO_DOUBLE:
                writeSingleByte(Opcodes.LONG_TO_DOUBLE);
                break;
            case FLOAT_TO_INT:
                writeSingleByte(Opcodes.FLOAT_TO_INT);
                break;
            case FLOAT_TO_LONG:
                writeSingleByte(Opcodes.FLOAT_TO_LONG);
                break;
            case FLOAT_TO_DOUBLE:
                writeSingleByte(Opcodes.FLOAT_TO_DOUBLE);
                break;
            case DOUBLE_TO_INT:
                writeSingleByte(Opcodes.DOUBLE_TO_INT);
                break;
            case DOUBLE_TO_LONG:
                writeSingleByte(Opcodes.DOUBLE_TO_LONG);
                break;
            case DOUBLE_TO_FLOAT:
                writeSingleByte(Opcodes.DOUBLE_TO_FLOAT);
                break;

            case ARRAY_LENGTH:
                writeSingleByte(Opcodes.ARRAY_LENGTH);
                break;
            case IGNORE:
                writeSingleByte(Opcodes.IGNORE);
                break;
            case NULL_CHECK:
                writeSingleByte(Opcodes.NULL_CHECK);
                break;
            case NOT:
                writeSingleByte(Opcodes.NOT);
                break;
            case IINV:
                writeSingleByte(Opcodes.IINV);
                break;
            case LINV:
                writeSingleByte(Opcodes.LINV);
                break;
            case INEG:
                writeSingleByte(Opcodes.INEG);
                break;
            case LNEG:
                writeSingleByte(Opcodes.LNEG);
                break;
            case FNEG:
                writeSingleByte(Opcodes.FNEG);
                break;
            case DNEG:
                writeSingleByte(Opcodes.DNEG);
                break;

            case IADD:
                writeSingleByte(Opcodes.IADD);
                break;
            case ISUB:
                writeSingleByte(Opcodes.ISUB);
                break;
            case IMUL:
                writeSingleByte(Opcodes.IMUL);
                break;
            case IDIV:
                writeSingleByte(Opcodes.IDIV);
                break;
            case IREM:
                writeSingleByte(Opcodes.IREM);
                break;

            case LADD:
                writeSingleByte(Opcodes.LADD);
                break;
            case LSUB:
                writeSingleByte(Opcodes.LSUB);
                break;
            case LMUL:
                writeSingleByte(Opcodes.LMUL);
                break;
            case LDIV:
                writeSingleByte(Opcodes.LDIV);
                break;
            case LREM:
                writeSingleByte(Opcodes.LREM);
                break;
            case LCMP:
                writeSingleByte(Opcodes.LCMP);
                break;

            case FADD:
                writeSingleByte(Opcodes.FADD);
                break;
            case FSUB:
                writeSingleByte(Opcodes.FSUB);
                break;
            case FMUL:
                writeSingleByte(Opcodes.FMUL);
                break;
            case FDIV:
                writeSingleByte(Opcodes.FDIV);
                break;
            case FREM:
                writeSingleByte(Opcodes.FREM);
                break;
            case FCMP:
                writeSingleByte(Opcodes.FCMP);
                break;

            case DADD:
                writeSingleByte(Opcodes.DADD);
                break;
            case DSUB:
                writeSingleByte(Opcodes.DSUB);
                break;
            case DMUL:
                writeSingleByte(Opcodes.DMUL);
                break;
            case DDIV:
                writeSingleByte(Opcodes.DDIV);
                break;
            case DREM:
                writeSingleByte(Opcodes.DREM);
                break;
            case DCMP:
                writeSingleByte(Opcodes.DCMP);
                break;

            case IEQ:
                writeSingleByte(Opcodes.IEQ);
                break;
            case INE:
                writeSingleByte(Opcodes.INE);
                break;
            case ILT:
                writeSingleByte(Opcodes.ILT);
                break;
            case ILE:
                writeSingleByte(Opcodes.ILE);
                break;
            case IGT:
                writeSingleByte(Opcodes.IGT);
                break;
            case IGE:
                writeSingleByte(Opcodes.IGE);
                break;

            case REF_EQ:
                writeSingleByte(Opcodes.REF_EQ);
                break;
            case REF_NE:
                writeSingleByte(Opcodes.REF_NE);
                break;

            case IAND:
                writeSingleByte(Opcodes.IAND);
                break;
            case IOR:
                writeSingleByte(Opcodes.IOR);
                break;
            case IXOR:
                writeSingleByte(Opcodes.IXOR);
                break;
            case ISHL:
                writeSingleByte(Opcodes.ISHL);
                break;
            case ISHR:
                writeSingleByte(Opcodes.ISHR);
                break;
            case ISHRU:
                writeSingleByte(Opcodes.ISHRU);
                break;

            case LAND:
                writeSingleByte(Opcodes.LAND);
                break;
            case LOR:
                writeSingleByte(Opcodes.LOR);
                break;
            case LXOR:
                writeSingleByte(Opcodes.LXOR);
                break;
            case LSHL:
                writeSingleByte(Opcodes.LSHL);
                break;
            case LSHR:
                writeSingleByte(Opcodes.LSHR);
                break;
            case LSHRU:
                writeSingleByte(Opcodes.LSHRU);
                break;

            case LOGICAL_AND:
                writeSingleByte(Opcodes.LOGICAL_AND);
                break;
            case LOGICAL_OR:
                writeSingleByte(Opcodes.LOGICAL_OR);
                break;

            case UPPER_BOUND_CHECK:
                writeSingleByte(Opcodes.UPPER_BOUND_CHECK);
                break;
            case LOWER_BOUND_CHECK:
                writeSingleByte(Opcodes.LOWER_BOUND_CHECK);
                break;
        }
    }

    @Override
    public void visit(IrConditionalExpr expr) {
        writeSingleByte(Opcodes.CONDITIONAL);
    }

    @Override
    public void visit(IrTryCatchExpr expr) {
        writeSingleByte(Opcodes.TRY_CATCH);
        --tryCatchLevel;

        writeUnsignedInt(expr.getExceptionTypesCount());
        for (int i = 0; i < expr.getExceptionTypesCount(); ++i) {
            writeUnsignedInt(getClassIndex(expr.getExceptionType(i)));
        }

        writeUnsignedInt(expr.getCaughtValuesCount());
    }

    @Override
    public void visit(IrTryCatchStartExpr expr) {
        int diff = tryCatchLevel - tags.get(expr.getTryCatch()).index;
        if (diff == 0) {
            writeSingleByte(Opcodes.TRY_CATCH_START_0);
        } else {
            writeSingleByte(Opcodes.TRY_CATCH_START);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrCaughtValueExpr expr) {
        int diff = tryCatchLevel - tags.get(expr.getTryCatch()).index;
        if (diff == 0) {
            writeSingleByte(Opcodes.CAUGHT_VALUE_0);
        } else {
            writeSingleByte(Opcodes.CAUGHT_VALUE);
            writeUnsignedInt(diff);
        }
        writeUnsignedInt(expr.getIndex());
    }

    @Override
    public void visit(IrSetCaughtValueExpr expr) {
        int diff = tryCatchLevel - tags.get(expr.getTarget().getTryCatch()).index;
        if (diff == 0) {
            writeSingleByte(Opcodes.SET_CAUGHT_VALUE_0);
        } else {
            writeSingleByte(Opcodes.SET_CAUGHT_VALUE);
            writeUnsignedInt(diff);
        }
        writeUnsignedInt(expr.getTarget().getIndex());
    }

    @Override
    public void visit(IrCaughtExceptionExpr expr) {
        int diff = tryCatchLevel - tags.get(expr.getTryCatch()).index;
        if (diff == 0) {
            writeSingleByte(Opcodes.CAUGHT_EXCEPTION_0);
        } else {
            writeSingleByte(Opcodes.CAUGHT_EXCEPTION);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrThrowExpr expr) {
        writeSingleByte(Opcodes.THROW);
    }

    @Override
    public void visit(IrBlockExpr expr) {
        writeSingleByte(Opcodes.BLOCK);
        --blockLevel;
    }

    @Override
    public void visit(IrExitBlockExpr expr) {
        int diff = blockLevel - tags.get(expr.getBlock()).level;
        if (diff == 0) {
            writeSingleByte(Opcodes.EXIT_BLOCK_0);
        } else {
            writeSingleByte(Opcodes.EXIT_BLOCK);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrLoopExpr expr) {
        writeSingleByte(Opcodes.LOOP);
        --loopLevel;
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
        int diff = loopLevel - tags.get(expr.getLoop()).level;
        if (diff == 0) {
            writeSingleByte(Opcodes.LOOP_HEADER_0);
        } else {
            writeSingleByte(Opcodes.LOOP_HEADER);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        int diff = loopLevel - tags.get(expr.getLoop()).level;
        if (diff == 0) {
            writeSingleByte(Opcodes.LOOP_EXIT_0);
        } else {
            writeSingleByte(Opcodes.LOOP_EXIT);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrContinueLoopExpr expr) {
        int diff = loopLevel - tags.get(expr.getLoop()).level;
        if (diff == 0) {
            writeSingleByte(Opcodes.LOOP_CONTINUE_0);
        } else {
            writeSingleByte(Opcodes.LOOP_CONTINUE);
            writeUnsignedInt(diff);
        }
    }

    @Override
    public void visit(IrTupleExpr expr) {
        if (expr.getInputCount() >= 2 && expr.getInputCount() < 8) {
            writeSingleByte((byte) (Opcodes.TUPLE_2 + expr.getInputCount() - 2));
        } else {
            writeSingleByte(Opcodes.TUPLE);
            writeUnsignedInt(expr.getInputCount());
        }
    }

    @Override
    public void visit(IrTupleComponentExpr expr) {
        if (expr.getComponent() < 8) {
            writeSingleByte((byte) (Opcodes.TUPLE_COMPONENT_0 + expr.getComponent()));
        } else {
            writeSingleByte(Opcodes.TUPLE_COMPONENT);
            writeUnsignedInt(expr.getComponent());
        }
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
        switch (expr.getValue()) {
            case -1:
                writeSingleByte(Opcodes.INT_CONST_M1);
                break;
            case 0:
                writeSingleByte(Opcodes.INT_CONST_0);
                break;
            case 1:
                writeSingleByte(Opcodes.INT_CONST_1);
                break;
            case 2:
                writeSingleByte(Opcodes.INT_CONST_2);
                break;
            default:
                writeSingleByte(Opcodes.INT_CONST);
                writeSignedInt(expr.getValue());
                break;
        }
    }

    @Override
    public void visit(IrLongConstantExpr expr) {
        if ((int) expr.getValue() == expr.getValue()) {
           switch ((int) expr.getValue()) {
               case -1:
                   writeSingleByte(Opcodes.LONG_CONST_M1);
                   return;
               case 0:
                   writeSingleByte(Opcodes.LONG_CONST_0);
                   return;
               case 1:
                   writeSingleByte(Opcodes.LONG_CONST_1);
                   return;
               case 2:
                   writeSingleByte(Opcodes.LONG_CONST_2);
                   return;
           }
        }
        writeSingleByte(Opcodes.LONG_CONST);
        writeSignedLong(expr.getValue());
    }

    @Override
    public void visit(IrFloatConstantExpr expr) {
        if (expr.getValue() == 0) {
            writeSingleByte(Opcodes.FLOAT_CONST_0);
        } else {
            writeSingleByte(Opcodes.FLOAT_CONST);
            writeUnsignedInt(Integer.reverse(Float.floatToIntBits(expr.getValue())));
        }
    }

    @Override
    public void visit(IrDoubleConstantExpr expr) {
        if (expr.getValue() == 0) {
            writeSingleByte(Opcodes.DOUBLE_CONST_0);
        } else {
            writeSingleByte(Opcodes.DOUBLE_CONST);
            writeUnsignedLong(Long.reverse(Double.doubleToLongBits(expr.getValue())));
        }
    }

    @Override
    public void visit(IrStringConstantExpr expr) {
        writeSingleByte(Opcodes.STRING_CONST);
        writeUnsignedInt(getStringIndex(expr.getValue()));
    }

    @Override
    public void visit(IrCallExpr expr) {
        switch (expr.getTarget().getType()) {
            case FUNCTION:
                writeSingleByte(Opcodes.CALL_FUNCTION);
                writeUnsignedInt(getFunctionIndex((IrFunction) expr.getTarget().getCallable()));
                break;
            case METHOD:
                writeSingleByte(Opcodes.CALL_METHOD);
                writeUnsignedInt(getMethodIndex((IrMethod) expr.getTarget().getCallable()));
                break;
        }
    }

    @Override
    public void visit(IrGetVariableExpr expr) {
        writeSingleByte(Opcodes.GET_VAR);
        writeUnsignedInt(expr.getVariable().getIndex());
    }

    @Override
    public void visit(IrSetVariableExpr expr) {
        writeSingleByte(Opcodes.SET_VAR);
        writeUnsignedInt(expr.getVariable().getIndex());
    }

    @Override
    public void visit(IrGetGlobalExpr expr) {
        writeSingleByte(Opcodes.GET_GLOBAL);
        writeUnsignedInt(getGlobalIndex(expr.getGlobal()));
    }

    @Override
    public void visit(IrSetGlobalExpr expr) {
        writeSingleByte(Opcodes.SET_GLOBAL);
        writeUnsignedInt(getGlobalIndex(expr.getGlobal()));
    }

    @Override
    public void visit(IrGetFieldExpr expr) {
        writeSingleByte(Opcodes.GET_FIELD);
        writeUnsignedInt(getFieldIndex(expr.getField()));
    }

    @Override
    public void visit(IrSetFieldExpr expr) {
        writeSingleByte(Opcodes.SET_FIELD);
        writeUnsignedInt(getFieldIndex(expr.getField()));
    }

    @Override
    public void visit(IrCastExpr expr) {
        writeSingleByte(Opcodes.CAST);
        writeReferenceType(expr.getTargetType());
    }

    @Override
    public void visit(IrInstanceOfExpr expr) {
        writeSingleByte(Opcodes.INSTANCEOF);
        writeReferenceType(expr.getCheckedType());
    }

    @Override
    public void visit(IrParameterExpr expr) {
        if (expr.getParameter().getIndex() < 4) {
            writeSingleByte((byte) (Opcodes.PARAMETER_0 + expr.getParameter().getIndex()));
        } else {
            writeSingleByte(Opcodes.PARAMETER);
            writeUnsignedInt(expr.getParameter().getIndex());
        }
    }

    @Override
    public void visit(IrNewObjectExpr expr) {
        writeSingleByte(Opcodes.NEW);
        writeUnsignedInt(getClassIndex(expr.getObjectType()));
    }

    private IrExprVisitor enterVisitor = new RecursiveIrExprVisitor() {
        @Override
        protected void visitDefault(IrExpr expr) {
        }

        @Override
        public void visit(IrTryCatchExpr expr) {
            tags.get(expr).level = ++tryCatchLevel;
        }

        @Override
        public void visit(IrBlockExpr expr) {
            tags.get(expr).level = ++blockLevel;
        }

        @Override
        public void visit(IrLoopExpr expr) {
            tags.get(expr).level = ++loopLevel;
        }
    };

    private int getClassIndex(IrClass cls) {
        int index = classIndexes.getOrDefault(cls, -1);
        if (index < 0) {
            index = classes.size();
            classes.add(cls);
            classIndexes.put(cls, index);
        }
        return index;
    }

    private int getStringIndex(String string) {
        int index = stringIndexes.getOrDefault(string, -1);
        if (index < 0) {
            index = strings.size();
            strings.add(string);
            stringIndexes.put(string, index);
        }
        return index;
    }

    private int getFunctionIndex(IrFunction function) {
        int index = functionIndexes.getOrDefault(function, -1);
        if (index < 0) {
            index = functions.size();
            functions.add(function);
            functionIndexes.put(function, index);
        }
        return index;
    }

    private int getMethodIndex(IrMethod method) {
        int index = methodIndexes.getOrDefault(method, -1);
        if (index < 0) {
            index = methods.size();
            methods.add(method);
            methodIndexes.put(method, index);
        }
        return index;
    }

    private int getFieldIndex(IrField field) {
        int index = fieldIndexes.getOrDefault(field, -1);
        if (index < 0) {
            index = fields.size();
            fields.add(field);
            fieldIndexes.put(field, index);
        }
        return index;
    }

    private int getGlobalIndex(IrGlobal global) {
        int index = globalIndexes.getOrDefault(global, -1);
        if (index < 0) {
            index = globals.size();
            globals.add(global);
            globalIndexes.put(global, index);
        }
        return index;
    }

    private void writeReferenceType(IrReferenceType type) {
        int degree = 0;
        while (type.getKind() == IrReferenceTypeKind.ARRAY) {
            type = ((IrReferenceArrayType) type).getElementType();
            ++degree;
        }

        int tag;
        switch (type.getKind()) {
            case BOOLEAN_ARRAY:
                tag = 0;
                break;
            case BYTE_ARRAY:
                tag = 1;
                break;
            case SHORT_ARRAY:
                tag = 2;
                break;
            case CHAR_ARRAY:
                tag = 3;
                break;
            case INT_ARRAY:
                tag = 4;
                break;
            case LONG_ARRAY:
                tag = 5;
                break;
            case FLOAT_ARRAY:
                tag = 6;
                break;
            case DOUBLE_ARRAY:
                tag = 7;
                break;
            case OBJECT:
                tag = 8;
                break;
            default:
                throw new IllegalStateException();
        }

        writeSingleByte((byte) (tag | (degree << 4)));
        if (type.getKind() == IrReferenceTypeKind.OBJECT) {
            IrClass cls = ((IrObjectType) type).getObjectClass();
            writeUnsignedInt(getClassIndex(cls));
        }
    }

    private void writeSignedInt(int value) {
        if (value > 0) {
            writeUnsignedInt(value << 1);
        } else {
            writeUnsignedInt(1 | (~value << 1));
        }
    }

    private void writeUnsignedInt(int value) {
        do {
            writeSingleByte((byte) (value > 127 ? value & 0x127 : 0x80 | (value & 0x7F)));
            value >>= 7;
        } while (value != 0);
    }

    private void writeSignedLong(long value) {
        if (value > 0) {
            writeUnsignedLong(value << 1);
        } else {
            writeUnsignedLong(1 | (~value << 1));
        }
    }

    private void writeUnsignedLong(long value) {
        do {
            writeSingleByte((byte) (value > 127 ? value & 0x127 : 0x80 | (value & 0x7F)));
            value >>= 7;
        } while (value != 0);
    }

    private void writeSingleByte(byte b) {
        grow(pointer + 1);
        output[pointer++] = b;
    }

    private void grow(int desiredSize) {
        int newSize = output.length;
        while (newSize < desiredSize) {
            newSize <<= 1;
        }
        if (newSize != output.length) {
            output = Arrays.copyOf(output, newSize);
        }
    }

    static class ExprData {
        int index = -1;
        int level;
        boolean noBackref;
        IrExpr next;
    }
}
