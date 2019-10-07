/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.metaprogramming.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.impl.reflect.ReflectClassImpl;
import org.teavm.metaprogramming.impl.reflect.ReflectFieldImpl;
import org.teavm.metaprogramming.impl.reflect.ReflectMethodImpl;
import org.teavm.metaprogramming.reflect.ReflectField;
import org.teavm.metaprogramming.reflect.ReflectMethod;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.IncomingReader;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.SwitchTableEntryReader;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class CompositeMethodGenerator {
    private Diagnostics diagnostics;
    Program program;
    TextLocation location;
    TextLocation forcedLocation;
    int blockIndex;
    int returnBlockIndex;
    private Variable resultVar;
    private Phi resultPhi;
    private Map<BasicBlock, BasicBlock> phiBlockMap = new HashMap<>();
    VariableContext varContext;

    CompositeMethodGenerator(VariableContext varContext) {
        this(varContext, new Program());
        program.createBasicBlock();
    }

    CompositeMethodGenerator(VariableContext varContext, Program program) {
        this.diagnostics = MetaprogrammingImpl.agent.getDiagnostics();
        this.program = program;
        this.varContext = varContext;
    }

    public void addProgram(ProgramReader template, List<CapturedValue> capturedValues) {
        location = null;
        resultVar = null;
        resultPhi = null;
        int startBlock = blockIndex;
        AliasFinder aliasFinder = new AliasFinder();
        aliasFinder.findAliases(template);

        CapturedValue[] capturedValueArray = new CapturedValue[template.variableCount()];
        for (int i = 0; i < capturedValues.size(); ++i) {
            capturedValueArray[i + 1] = capturedValues.get(i);
        }

        TemplateSubstitutor substitutor = new TemplateSubstitutor(capturedValueArray, aliasFinder.getAliases(),
                aliasFinder.getArrayElements(), program.basicBlockCount() - 1,
                program.variableCount() - capturedValues.size());

        // Pre-create blocks and variables
        for (int i = 0; i < template.basicBlockCount(); ++i) {
            program.createBasicBlock();
        }
        returnBlockIndex = program.basicBlockCount() - 1;

        for (int i = capturedValues.size(); i < template.variableCount(); ++i) {
            VariableReader variable = template.variableAt(i);
            Variable variableCopy = program.createVariable();
            variableCopy.setDebugName(variable.getDebugName());
            variableCopy.setLabel(variable.getLabel());
        }

        // Pre-create phis
        // We need this since phi input variables can be captured as well. In thi case
        // we must perform capturing in input block rather that phi owner.
        List<List<Incoming>> outgoings = Stream.<List<Incoming>>generate(ArrayList::new)
                .limit(template.basicBlockCount())
                .collect(Collectors.toList());
        List<List<VariableReader>> outgoingVars = Stream.<List<VariableReader>>generate(ArrayList::new)
                .limit(template.basicBlockCount())
                .collect(Collectors.toList());
        for (int i = 0; i < template.basicBlockCount(); ++i) {
            BasicBlockReader templateBlock = template.basicBlockAt(i);
            blockIndex = i == 0 ? startBlock : substitutor.blockOffset + i;
            BasicBlock targetBlock = program.basicBlockAt(blockIndex);
            for (PhiReader templatePhi : templateBlock.readPhis()) {
                Phi phi = new Phi();
                for (IncomingReader templateIncoming : templatePhi.readIncomings()) {
                    Incoming incoming = new Incoming();
                    incoming.setSource(substitutor.block(templateIncoming.getSource()));
                    phi.getIncomings().add(incoming);
                    int sourceIndex = templateIncoming.getSource().getIndex();
                    outgoings.get(sourceIndex).add(incoming);
                    outgoingVars.get(sourceIndex).add(templateIncoming.getValue());
                }
                phi.setReceiver(substitutor.var(templatePhi.getReceiver()));
                targetBlock.getPhis().add(phi);
            }
        }

        // Copy program
        for (int i = 0; i < template.basicBlockCount(); ++i) {
            BasicBlockReader templateBlock = template.basicBlockAt(i);
            blockIndex = i == 0 ? startBlock : substitutor.blockOffset + i;
            BasicBlock targetBlock = program.basicBlockAt(blockIndex);
            if (templateBlock.getExceptionVariable() != null) {
                targetBlock.setExceptionVariable(substitutor.var(templateBlock.getExceptionVariable()));
            }

            for (TryCatchBlockReader templateTryCatch : templateBlock.readTryCatchBlocks()) {
                TryCatchBlock tryCatch = new TryCatchBlock();
                tryCatch.setExceptionType(templateTryCatch.getExceptionType());
                tryCatch.setHandler(substitutor.block(templateTryCatch.getHandler()));
                targetBlock.getTryCatchBlocks().add(tryCatch);
            }

            templateBlock.readAllInstructions(substitutor);

            // Capture phi inputs of successor blocks
            Instruction lastInsn = targetBlock.getLastInstruction();
            lastInsn.delete();
            List<Incoming> blockOutgoings = outgoings.get(i);
            for (int j = 0; j < blockOutgoings.size(); ++j) {
                VariableReader outgoingVar = outgoingVars.get(i).get(j);
                blockOutgoings.get(j).setValue(substitutor.var(outgoingVar));
            }
            targetBlock.add(lastInsn);

            phiBlockMap.put(targetBlock, currentBlock());
        }

        // Fix phi input blocks
        for (int i = 0; i < template.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i == 0 ? startBlock : substitutor.blockOffset + i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    BasicBlock mappedBlock = phiBlockMap.get(incoming.getSource());
                    if (mappedBlock != null) {
                        incoming.setSource(mappedBlock);
                    }
                }
            }
        }

        phiBlockMap.clear();
        blockIndex = substitutor.blockOffset + template.basicBlockCount();
    }

    public Variable getResultVar() {
        return resultVar;
    }

    public BasicBlock currentBlock() {
        return program.basicBlockAt(blockIndex);
    }

    void add(Instruction insn) {
        insn.setLocation(forcedLocation != null ? forcedLocation : location);
        program.basicBlockAt(blockIndex).add(insn);
    }

    Variable captureValue(CapturedValue captured) {
        Object value = captured.obj;
        if (value == null) {
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Integer) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Integer) value);
            add(insn);
            Variable result = insn.getReceiver();
            if (!captured.primitive) {
                result = box(result, ValueType.INTEGER);
            }
            return result;
        } else if (value instanceof Long) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Long) value);
            add(insn);
            Variable result = insn.getReceiver();
            if (!captured.primitive) {
                result = box(result, ValueType.LONG);
            }
            return result;
        } else if (value instanceof Float) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Float) value);
            add(insn);
            Variable result = insn.getReceiver();
            if (!captured.primitive) {
                result = box(result, ValueType.FLOAT);
            }
            return result;
        } else if (value instanceof Double) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((Double) value);
            add(insn);
            Variable result = insn.getReceiver();
            if (!captured.primitive) {
                result = box(result, ValueType.DOUBLE);
            }
            return result;
        } else if (value instanceof String) {
            StringConstantInstruction insn = new StringConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((String) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof ValueType) {
            ClassConstantInstruction insn = new ClassConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant((ValueType) value);
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof Class<?>) {
            ClassConstantInstruction insn = new ClassConstantInstruction();
            insn.setReceiver(program.createVariable());
            insn.setConstant(ValueType.parse((Class<?>) value));
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof ValueImpl) {
            Variable result = varContext.emitVariable((ValueImpl<?>) value,
                    new CallLocation(MetaprogrammingImpl.templateMethod, location));
            return coalesce(result);
        } else if (value instanceof LazyValueImpl) {
            return coalesce(lazy((LazyValueImpl<?>) value));
        } else if (value instanceof ReflectFieldImpl) {
            ReflectFieldImpl reflectField = (ReflectFieldImpl) value;
            diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                    "Can't reference this ReflectField {{f0}} directly except for calling special "
                    + "methods on it", reflectField.field.getReference());
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        } else if (value instanceof ReflectMethodImpl) {
            ReflectMethodImpl reflectMethod = (ReflectMethodImpl) value;
            diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                    "Can't reference this ReflectMethod {{m0}} directly except for calling special methods on it",
                    reflectMethod.method.getReference());
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        } else if (value.getClass().getComponentType() != null) {
            diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                    "Can't reference this array directly except for fetching by constant index");
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        } else {
            diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location), "Wrong captured value");
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            add(insn);
            return insn.getReceiver();
        }
    }

    Variable lazy(LazyValueImpl<?> lazyImpl) {
        CompositeMethodGenerator nestedGenerator = new CompositeMethodGenerator(varContext, program);
        nestedGenerator.blockIndex = blockIndex;
        nestedGenerator.location = location;
        nestedGenerator.forcedLocation = lazyImpl.forcedLocation;
        MetaprogrammingImpl.generator = nestedGenerator;
        Value<?> result = lazyImpl.computation.compute();
        blockIndex = nestedGenerator.blockIndex;
        MetaprogrammingImpl.generator = this;

        if (result instanceof ValueImpl) {
            return ((ValueImpl<?>) result).innerValue;
        } else if (result instanceof LazyValueImpl) {
            return lazy((LazyValueImpl<?>) result);
        } else if (result != null) {
            throw new IllegalStateException("Unknown value type: " + result.getClass().getName());
        } else {
            return null;
        }
    }

    private Variable coalesce(Variable var) {
        if (var == null) {
            NullConstantInstruction nullInsn = new NullConstantInstruction();
            nullInsn.setReceiver(program.createVariable());
            var = nullInsn.getReceiver();
            add(nullInsn);
        }
        return var;
    }

    Variable box(Variable var, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return box(var, boolean.class, Boolean.class);
                case BYTE:
                    return box(var, byte.class, Byte.class);
                case SHORT:
                    return box(var, short.class, Short.class);
                case CHARACTER:
                    return box(var, char.class, Character.class);
                case INTEGER:
                    return box(var, int.class, Integer.class);
                case LONG:
                    return box(var, long.class, Long.class);
                case FLOAT:
                    return box(var, float.class, Float.class);
                case DOUBLE:
                    return box(var, double.class, Double.class);
            }
        }
        return var;
    }

    private Variable box(Variable var, Class<?> primitive, Class<?> wrapper) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(wrapper, "valueOf", primitive, wrapper));
        insn.setType(InvocationType.SPECIAL);
        insn.setArguments(var);
        var = program.createVariable();
        insn.setReceiver(var);
        add(insn);
        return var;
    }

    Variable unbox(Variable var, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unbox(var, boolean.class, Boolean.class);
                case BYTE:
                    return unbox(var, byte.class, Byte.class);
                case SHORT:
                    return unbox(var, short.class, Short.class);
                case CHARACTER:
                    return unbox(var, char.class, Character.class);
                case INTEGER:
                    return unbox(var, int.class, Integer.class);
                case LONG:
                    return unbox(var, long.class, Long.class);
                case FLOAT:
                    return unbox(var, float.class, Float.class);
                case DOUBLE:
                    return unbox(var, double.class, Double.class);
            }
        } else if (!type.isObject(Object.class)) {
            CastInstruction castInsn = new CastInstruction();
            castInsn.setValue(var);
            castInsn.setReceiver(program.createVariable());
            castInsn.setTargetType(type);
            var = castInsn.getReceiver();
            add(castInsn);
        }
        return var;
    }

    private Variable unbox(Variable var, Class<?> primitive, Class<?> wrapper) {
        CastInstruction castInsn = new CastInstruction();
        castInsn.setValue(var);
        castInsn.setReceiver(program.createVariable());
        castInsn.setTargetType(ValueType.parse(wrapper));
        add(castInsn);

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(wrapper, primitive.getName() + "Value", primitive));
        insn.setType(InvocationType.VIRTUAL);
        insn.setInstance(castInsn.getReceiver());
        var = program.createVariable();
        insn.setReceiver(var);
        add(insn);
        return var;
    }

    public Program getProgram() {
        return program;
    }

    private class TemplateSubstitutor implements InstructionReader {
        private int blockOffset;
        private int variableOffset;
        int[] variableMapping;
        CapturedValue[] capturedValues;
        AliasFinder.ArrayElement[] arrayElements;

        TemplateSubstitutor(CapturedValue[] capturedValues, int[] variableMapping,
                AliasFinder.ArrayElement[] arrayElements, int blockOffset, int variableOffset) {
            this.capturedValues = capturedValues;
            this.variableMapping = variableMapping;
            this.arrayElements = arrayElements;
            this.blockOffset = blockOffset;
            this.variableOffset = variableOffset;
        }

        @Override
        public void location(TextLocation location) {
            CompositeMethodGenerator.this.location = location;
        }

        @Override
        public void nop() {
        }

        public Variable var(VariableReader variable) {
            if (variable == null) {
                return null;
            }
            int index = variableMapping[variable.getIndex()];
            if (capturedValues[index] != null) {
                return captureValue(capturedValues[index]);
            }
            AliasFinder.ArrayElement elem = arrayElements[index];
            if (elem != null) {
                int arrayVar = variableMapping[elem.array];
                if (capturedValues[arrayVar] != null) {
                    Object capturedArray = capturedValues[arrayVar].obj;
                    boolean primitive = capturedArray.getClass().getComponentType().isPrimitive();
                    return captureValue(new CapturedValue(Array.get(capturedArray, elem.index), primitive));
                }
            }
            return program.variableAt(variableOffset + variable.getIndex());
        }

        public BasicBlock block(BasicBlockReader block) {
            if (block == null) {
                return null;
            }
            return program.basicBlockAt(blockOffset + block.getIndex());
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            ClassConstantInstruction insn = new ClassConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void nullConstant(VariableReader receiver) {
            NullConstantInstruction insn = new NullConstantInstruction();
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            IntegerConstantInstruction insn = new IntegerConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            LongConstantInstruction insn = new LongConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            FloatConstantInstruction insn = new FloatConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            DoubleConstantInstruction insn = new DoubleConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            StringConstantInstruction insn = new StringConstantInstruction();
            insn.setConstant(cst);
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
            BinaryInstruction insn = new BinaryInstruction(op, type);
            insn.setReceiver(var(receiver));
            insn.setFirstOperand(var(first));
            insn.setSecondOperand(var(second));
            add(insn);
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
            NegateInstruction insn = new NegateInstruction(type);
            insn.setReceiver(var(receiver));
            insn.setOperand(var(operand));
            add(insn);
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            int index = variableMapping[assignee.getIndex()];
            if (capturedValues[index] != null) {
                return;
            }

            AssignInstruction insn = new AssignInstruction();
            insn.setReceiver(var(receiver));
            insn.setAssignee(var(assignee));
            if (insn.getReceiver() != insn.getAssignee()) {
                add(insn);
            }
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            CastInstruction insn = new CastInstruction();
            insn.setTargetType(targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
            CastNumberInstruction insn = new CastNumberInstruction(sourceType, targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
            CastIntegerInstruction insn = new CastIntegerInstruction(type, targetType);
            insn.setValue(var(value));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            BranchingInstruction insn = new BranchingInstruction(cond);
            insn.setOperand(var(operand));
            insn.setConsequent(block(consequent));
            insn.setAlternative(block(alternative));
            add(insn);
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            BinaryBranchingInstruction insn = new BinaryBranchingInstruction(cond);
            insn.setFirstOperand(var(first));
            insn.setSecondOperand(var(second));
            insn.setConsequent(block(consequent));
            insn.setAlternative(block(alternative));
            add(insn);
        }

        @Override
        public void jump(BasicBlockReader target) {
            JumpInstruction insn = new JumpInstruction();
            insn.setTarget(block(target));
            add(insn);
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            SwitchInstruction insn = new SwitchInstruction();
            insn.setCondition(var(condition));
            insn.setDefaultTarget(block(defaultTarget));
            for (SwitchTableEntryReader entry : table) {
                SwitchTableEntry insnEntry = new SwitchTableEntry();
                insnEntry.setCondition(entry.getCondition());
                insnEntry.setTarget(block(entry.getTarget()));
                insn.getEntries().add(insnEntry);
            }
            add(insn);
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            BasicBlock target = program.basicBlockAt(returnBlockIndex);

            if (valueToReturn != null) {
                Variable valueToReturnResolved = var(valueToReturn);
                if (resultVar == null) {
                    resultVar = program.createVariable();
                    resultPhi = new Phi();
                    resultPhi.setReceiver(resultVar);
                    target.getPhis().add(resultPhi);
                }
                Incoming incoming = new Incoming();
                incoming.setSource(program.basicBlockAt(blockIndex));
                incoming.setValue(valueToReturnResolved);
                resultPhi.getIncomings().add(incoming);
            }

            JumpInstruction insn = new JumpInstruction();
            insn.setTarget(target);
            add(insn);
        }

        @Override
        public void raise(VariableReader exception) {
            RaiseInstruction insn = new RaiseInstruction();
            insn.setException(var(exception));
            add(insn);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            ConstructArrayInstruction insn = new ConstructArrayInstruction();
            insn.setReceiver(var(receiver));
            insn.setItemType(itemType);
            insn.setSize(var(size));
            add(insn);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
            insn.setReceiver(var(receiver));
            insn.setItemType(itemType);
            insn.getDimensions().addAll(dimensions.stream().map(this::var).collect(Collectors.toList()));
            add(insn);
        }

        @Override
        public void create(VariableReader receiver, String type) {
            ConstructInstruction insn = new ConstructInstruction();
            insn.setReceiver(var(receiver));
            insn.setType(type);
            add(insn);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            GetFieldInstruction insn = new GetFieldInstruction();
            insn.setField(field);
            insn.setFieldType(fieldType);
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value,
                ValueType fieldType) {
            PutFieldInstruction insn = new PutFieldInstruction();
            insn.setField(field);
            insn.setFieldType(fieldType);
            insn.setInstance(var(instance));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            ArrayLengthInstruction insn = new ArrayLengthInstruction();
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            CloneArrayInstruction insn = new CloneArrayInstruction();
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            int arrayIndex = variableMapping[array.getIndex()];
            if (capturedValues[arrayIndex] != null) {
                return;
            }

            UnwrapArrayInstruction insn = new UnwrapArrayInstruction(elementType);
            insn.setArray(var(array));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            int arrayIndex = variableMapping[array.getIndex()];

            AliasFinder.ArrayElement elem = arrayElements[receiver.getIndex()];
            if (elem != null && capturedValues[arrayIndex] != null) {
                AssignInstruction insn = new AssignInstruction();
                insn.setAssignee(var(receiver));
                insn.setReceiver(program.variableAt(variableOffset + receiver.getIndex()));
                add(insn);
                return;
            }

            GetElementInstruction insn = new GetElementInstruction(type);
            insn.setArray(var(array));
            insn.setIndex(var(index));
            insn.setReceiver(var(receiver));
            add(insn);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType type) {
            PutElementInstruction insn = new PutElementInstruction(type);
            insn.setArray(var(array));
            insn.setIndex(var(index));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (type == InvocationType.VIRTUAL && instance != null) {
                if (method.getClassName().equals(Value.class.getName())) {
                    if (method.getName().equals("get")) {
                        if (receiver != null) {
                            AssignInstruction insn = new AssignInstruction();
                            insn.setReceiver(var(receiver));
                            insn.setAssignee(var(instance));
                            add(insn);
                        } else {
                            var(instance);
                        }
                        return;
                    } else {
                        diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                                "Can't call method {{m0}} in runtime domain", method);
                    }
                } else if (method.getClassName().equals(ReflectField.class.getName())) {
                    if (replaceFieldGetSet(receiver, instance, method, arguments)) {
                        return;
                    }
                } else if (method.getClassName().equals(ReflectMethod.class.getName())) {
                    if (replaceMethodInvocation(receiver, instance, method, arguments)) {
                        return;
                    }
                } else if (method.getClassName().equals(ReflectClass.class.getName())) {
                    if (replaceClassInvocation(receiver, instance, method, arguments)) {
                        return;
                    }
                }
            }
            InvokeInstruction insn = new InvokeInstruction();
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            insn.setMethod(method);
            insn.setType(type);
            insn.setArguments(arguments.stream().map(this::var).toArray(Variable[]::new));
            add(insn);
        }

        private boolean replaceFieldGetSet(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            int instanceIndex = variableMapping[instance.getIndex()];
            if (capturedValues[instanceIndex] == null) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location), "Can call {{m0}}"
                        + " method only on a reflected field captured by lambda from outer context", method);
                return false;
            }

            Object value = capturedValues[instanceIndex].obj;
            if (!(value instanceof ReflectFieldImpl)) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                        "Wrong call to {{m0}} method ", method);
                return false;
            }

            ReflectFieldImpl field = (ReflectFieldImpl) value;
            switch (method.getName()) {
                case "get": {
                    Variable var = program.createVariable();
                    GetFieldInstruction insn = new GetFieldInstruction();
                    insn.setInstance(!field.field.hasModifier(ElementModifier.STATIC) ? var(arguments.get(0)) : null);
                    insn.setReceiver(var);
                    insn.setField(field.getBackingField().getReference());
                    insn.setFieldType(field.getBackingField().getType());
                    add(insn);

                    var = box(var, field.getBackingField().getType());

                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(var);
                    assign.setReceiver(var(receiver));
                    add(assign);

                    return true;
                }
                case "set": {
                    PutFieldInstruction insn = new PutFieldInstruction();
                    insn.setInstance(!field.field.hasModifier(ElementModifier.STATIC) ? var(arguments.get(0)) : null);
                    insn.setValue(unbox(var(arguments.get(1)), field.getBackingField().getType()));
                    insn.setField(field.getBackingField().getReference());
                    insn.setFieldType(field.getBackingField().getType());
                    add(insn);
                    return true;
                }
                default:
                    diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location), "Can only "
                            + "call {{m0}} method from runtime domain", method);
                    return false;
            }
        }

        private boolean replaceMethodInvocation(VariableReader receiver, VariableReader instance,
                MethodReference method, List<? extends VariableReader> arguments) {
            int instanceIndex = variableMapping[instance.getIndex()];
            if (capturedValues[instanceIndex] == null) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                        "Can call {{m0}} method only on a reflected field captured by lambda from outer context",
                        method);
                return false;
            }

            Object value = capturedValues[instanceIndex].obj;
            if (!(value instanceof ReflectMethodImpl)) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                        "Wrong call to {{m0}} method ", method);
                return false;
            }

            ReflectMethodImpl reflectMethod = (ReflectMethodImpl) value;
            switch (method.getName()) {
                case "invoke": {
                    InvokeInstruction insn = new InvokeInstruction();
                    insn.setInstance(!Modifier.isStatic(reflectMethod.getModifiers()) ? var(arguments.get(0)) : null);
                    insn.setType(Modifier.isStatic(reflectMethod.getModifiers()) ? InvocationType.SPECIAL
                            : InvocationType.VIRTUAL);
                    insn.setMethod(reflectMethod.method.getReference());
                    insn.setArguments(emitArguments(var(arguments.get(1)), reflectMethod));
                    add(insn);

                    if (receiver != null) {
                        if (reflectMethod.method.getResultType() == ValueType.VOID) {
                            NullConstantInstruction nullInsn = new NullConstantInstruction();
                            nullInsn.setReceiver(var(receiver));
                            add(nullInsn);
                        } else {
                            Variable var = program.createVariable();
                            insn.setReceiver(var);
                            var = box(var, reflectMethod.method.getResultType());

                            AssignInstruction assign = new AssignInstruction();
                            assign.setAssignee(var);
                            assign.setReceiver(var(receiver));
                            add(assign);
                        }
                    }

                    return true;
                }
                case "construct": {
                    ConstructInstruction constructInsn = new ConstructInstruction();
                    constructInsn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    constructInsn.setType(reflectMethod.method.getOwnerName());
                    add(constructInsn);

                    InvokeInstruction insn = new InvokeInstruction();
                    insn.setInstance(constructInsn.getReceiver());
                    insn.setType(InvocationType.SPECIAL);
                    insn.setMethod(reflectMethod.method.getReference());
                    insn.setArguments(emitArguments(var(arguments.get(0)), reflectMethod));
                    add(insn);

                    return true;
                }
                default:
                    diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                            "Can only call {{m0}} method from runtime domain", method);
                    return false;
            }
        }

        private boolean replaceClassInvocation(VariableReader receiver, VariableReader instance,
                MethodReference method, List<? extends VariableReader> arguments) {
            int instanceIndex = variableMapping[instance.getIndex()];
            if (capturedValues[instanceIndex] == null) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                        "Can call {{m0}} method only on a reflected class captured by lambda from outer context",
                        method);
                return false;
            }

            Object value = capturedValues[instanceIndex].obj;
            if (!(value instanceof ReflectClassImpl)) {
                diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                        "Wrong call to {{m0}} method ", method);
                return false;
            }

            ReflectClassImpl<?> reflectClass = (ReflectClassImpl<?>) value;
            switch (method.getName()) {
                case "isInstance": {
                    IsInstanceInstruction insn = new IsInstanceInstruction();
                    insn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    insn.setValue(var(arguments.get(0)));
                    insn.setType(reflectClass.type);
                    add(insn);
                    return true;
                }
                case "cast": {
                    CastInstruction insn = new CastInstruction();
                    insn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    insn.setValue(var(arguments.get(0)));
                    insn.setTargetType(reflectClass.type);
                    add(insn);
                    return true;
                }
                case "asJavaClass": {
                    ClassConstantInstruction insn = new ClassConstantInstruction();
                    insn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    insn.setConstant(reflectClass.type);
                    add(insn);
                    return true;
                }
                case "createArray": {
                    ConstructArrayInstruction insn = new ConstructArrayInstruction();
                    insn.setItemType(reflectClass.type);
                    insn.setSize(var(arguments.get(0)));
                    insn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    add(insn);
                    return true;
                }
                case "getArrayLength": {
                    ArrayLengthInstruction insn = new ArrayLengthInstruction();
                    insn.setArray(unwrapArray(reflectClass.type, var(arguments.get(0))));
                    insn.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    add(insn);
                    return true;
                }
                case "getArrayElement": {
                    GetElementInstruction insn = new GetElementInstruction(asArrayType(reflectClass.type));
                    insn.setArray(unwrapArray(reflectClass.type, var(arguments.get(0))));
                    insn.setIndex(var(arguments.get(1)));
                    insn.setReceiver(program.createVariable());
                    add(insn);

                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(box(insn.getReceiver(), ((ValueType.Array) reflectClass.type).getItemType()));
                    assign.setReceiver(receiver != null ? var(receiver) : program.createVariable());
                    add(assign);

                    return true;
                }
                default:
                    diagnostics.error(new CallLocation(MetaprogrammingImpl.templateMethod, location),
                            "Can only call {{m0}} method from runtime domain", method);
                    return false;
            }
        }

        private Variable[] emitArguments(Variable argumentsVar, ReflectMethodImpl reflectMethod) {
            UnwrapArrayInstruction unwrapInsn = new UnwrapArrayInstruction(ArrayElementType.OBJECT);
            unwrapInsn.setArray(argumentsVar);
            unwrapInsn.setReceiver(program.createVariable());
            add(unwrapInsn);
            argumentsVar = unwrapInsn.getReceiver();

            Variable[] arguments = new Variable[reflectMethod.getParameterCount()];
            for (int i = 0; i < reflectMethod.getParameterCount(); ++i) {
                IntegerConstantInstruction indexInsn = new IntegerConstantInstruction();
                indexInsn.setConstant(i);
                indexInsn.setReceiver(program.createVariable());
                add(indexInsn);

                GetElementInstruction extractArgInsn = new GetElementInstruction(ArrayElementType.OBJECT);
                extractArgInsn.setArray(argumentsVar);
                extractArgInsn.setIndex(indexInsn.getReceiver());
                extractArgInsn.setReceiver(program.createVariable());
                add(extractArgInsn);

                arguments[i] = unbox(extractArgInsn.getReceiver(), reflectMethod.method.parameterType(i));
            }

            return arguments;
        }

        private Variable unwrapArray(ValueType type, Variable array) {
            CastInstruction cast = new CastInstruction();
            cast.setTargetType(ValueType.arrayOf(type));
            cast.setValue(array);
            cast.setReceiver(program.createVariable());
            add(cast);

            UnwrapArrayInstruction unwrap = new UnwrapArrayInstruction(asArrayType(type));
            unwrap.setArray(cast.getReceiver());
            unwrap.setReceiver(program.createVariable());
            add(unwrap);

            return unwrap.getReceiver();
        }

        private ArrayElementType asArrayType(ValueType type) {
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                    case BYTE:
                        return ArrayElementType.BYTE;
                    case SHORT:
                        return ArrayElementType.SHORT;
                    case CHARACTER:
                        return ArrayElementType.CHAR;
                    case INTEGER:
                        return ArrayElementType.INT;
                    case LONG:
                        return ArrayElementType.LONG;
                    case FLOAT:
                        return ArrayElementType.FLOAT;
                    case DOUBLE:
                        return ArrayElementType.DOUBLE;
                }
            }
            return ArrayElementType.OBJECT;
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            InvokeDynamicInstruction insn = new InvokeDynamicInstruction();
            insn.setBootstrapMethod(bootstrapMethod);
            insn.setInstance(var(instance));
            insn.setReceiver(var(receiver));
            insn.setMethod(method);
            insn.getArguments().addAll(arguments.stream().map(this::var).collect(Collectors.toList()));
            insn.getBootstrapArguments().addAll(bootstrapArguments);
            add(insn);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
            IsInstanceInstruction insn = new IsInstanceInstruction();
            insn.setReceiver(var(receiver));
            insn.setValue(var(value));
            insn.setType(type);
            add(insn);
        }

        @Override
        public void initClass(String className) {
            InitClassInstruction insn = new InitClassInstruction();
            insn.setClassName(className);
            add(insn);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            NullCheckInstruction insn = new NullCheckInstruction();
            insn.setReceiver(var(receiver));
            insn.setValue(var(value));
            add(insn);
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
            MonitorEnterInstruction insn = new MonitorEnterInstruction();
            insn.setObjectRef(var(objectRef));
            add(insn);
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            MonitorExitInstruction insn = new MonitorExitInstruction();
            insn.setObjectRef(var(objectRef));
            add(insn);
        }

        @Override
        public void boundCheck(VariableReader receiver, VariableReader index, VariableReader array, boolean lower) {
            BoundCheckInstruction instruction = new BoundCheckInstruction();
            instruction.setReceiver(var(receiver));
            instruction.setIndex(var(index));
            if (array != null) {
                instruction.setArray(var(array));
            }
            instruction.setLower(lower);
            add(instruction);
        }
    }
}
