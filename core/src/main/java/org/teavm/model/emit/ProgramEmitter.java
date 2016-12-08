/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.emit;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.util.InstructionTransitionExtractor;

public final class ProgramEmitter {
    private Program program;
    private BasicBlock block;
    ClassReaderSource classSource;
    private TextLocation currentLocation;

    private ProgramEmitter(Program program, BasicBlock block, ClassReaderSource classSource) {
        this.program = program;
        this.block = block;
        this.classSource = classSource;
    }

    public Program getProgram() {
        return program;
    }

    public BasicBlock getBlock() {
        return block;
    }

    public ProgramEmitter enter(BasicBlock block) {
        this.block = block;
        return this;
    }

    public BasicBlock prepareBlock() {
        return program.createBasicBlock();
    }

    public ValueEmitter constant(Class<?> cls) {
        return constant(ValueType.parse(cls));
    }

    public ValueEmitter constant(ValueType value) {
        Variable var = program.createVariable();
        ClassConstantInstruction insn = new ClassConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.object("java.lang.Class"));
    }

    public ValueEmitter constant(String value) {
        Variable var = program.createVariable();
        StringConstantInstruction insn = new StringConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.object("java.lang.String"));
    }

    public ValueEmitter constant(int value) {
        Variable var = program.createVariable();
        IntegerConstantInstruction insn = new IntegerConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.INTEGER);
    }

    public ValueEmitter constant(long value) {
        Variable var = program.createVariable();
        LongConstantInstruction insn = new LongConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.LONG);
    }

    public ValueEmitter constant(float value) {
        Variable var = program.createVariable();
        FloatConstantInstruction insn = new FloatConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.FLOAT);
    }

    public ValueEmitter constant(double value) {
        Variable var = program.createVariable();
        DoubleConstantInstruction insn = new DoubleConstantInstruction();
        insn.setReceiver(var);
        insn.setConstant(value);
        addInstruction(insn);
        return var(var, ValueType.DOUBLE);
    }

    public ValueEmitter constantNull(ValueType type) {
        Variable var = program.createVariable();
        NullConstantInstruction insn = new NullConstantInstruction();
        insn.setReceiver(var);
        addInstruction(insn);
        return var(var, type);
    }

    public ValueEmitter constantNull(Class<?> type) {
        return constantNull(ValueType.parse(type));
    }

    public ValueEmitter defaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return constant(0).cast(boolean.class);
                case BYTE:
                    return constant(0).cast(byte.class);
                case SHORT:
                    return constant(0).cast(short.class);
                case CHARACTER:
                    return constant(0).cast(char.class);
                case INTEGER:
                    return constant(0);
                case LONG:
                    return constant(0L);
                case FLOAT:
                    return constant(0F);
                case DOUBLE:
                    return constant(0.0);
            }
        }
        return constantNull(type);
    }

    public ValueEmitter getField(FieldReference field, ValueType type) {
        FieldReader resolvedField = classSource.resolve(field);
        if (resolvedField != null) {
            field = resolvedField.getReference();
        }

        Variable var = program.createVariable();
        GetFieldInstruction insn = new GetFieldInstruction();
        insn.setField(field);
        insn.setFieldType(type);
        insn.setReceiver(var);
        addInstruction(insn);
        return var(var, type);
    }

    public ValueEmitter getField(String className, String fieldName, ValueType type) {
        return getField(new FieldReference(className, fieldName), type);
    }

    public ValueEmitter getField(Class<?> cls, String fieldName, Class<?> type) {
        return getField(cls.getName(), fieldName, ValueType.parse(type));
    }

    public ProgramEmitter setField(FieldReference field, ValueEmitter value) {
        FieldReader resolvedField = classSource.resolve(field);
        if (resolvedField != null) {
            field = resolvedField.getReference();
        }

        PutFieldInstruction insn = new PutFieldInstruction();
        insn.setField(field);
        insn.setFieldType(value.type);
        insn.setValue(value.getVariable());
        addInstruction(insn);
        return this;
    }

    public ProgramEmitter setField(String className, String fieldName, ValueEmitter value) {
        return setField(new FieldReference(className, fieldName), value);
    }

    public ProgramEmitter setField(Class<?> cls, String fieldName, ValueEmitter value) {
        return setField(new FieldReference(cls.getName(), fieldName), value);
    }

    public ValueEmitter invoke(MethodReference method, ValueEmitter... arguments) {
        for (int i = 0; i < method.parameterCount(); ++i) {
            if (!classSource.isSuperType(method.parameterType(i), arguments[i].getType()).orElse(true)) {
                throw new EmitException("Argument " + i + " of type " + arguments[i].getType() + " is "
                        + "not compatible with method " + method);
            }
        }

        Variable result = null;
        if (method.getReturnType() != ValueType.VOID) {
            result = program.createVariable();
        }

        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(method);
        insn.setReceiver(result);
        for (ValueEmitter arg : arguments) {
            insn.getArguments().add(arg.variable);
        }
        addInstruction(insn);
        return result != null ? var(result, method.getReturnType()) : null;
    }

    public ValueEmitter invoke(String className, String methodName, ValueType resultType, ValueEmitter... arguments) {
        Variable result = null;
        if (resultType != ValueType.VOID) {
            result = program.createVariable();
        }

        ValueType[] argumentTypes = new ValueType[arguments.length + 1];
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = arguments[i].type;
        }
        argumentTypes[arguments.length] = resultType;
        MethodReference method = new MethodReference(className, methodName, argumentTypes);

        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(method);
        insn.setReceiver(result);
        for (ValueEmitter arg : arguments) {
            insn.getArguments().add(arg.variable);
        }
        addInstruction(insn);
        return result != null ? var(result, resultType) : null;
    }

    public ValueEmitter invoke(Class<?> cls, String methodName, Class<?> resultType, ValueEmitter... arguments) {
        return invoke(cls.getName(), methodName, ValueType.parse(resultType), arguments);
    }

    public ProgramEmitter invoke(String className, String methodName, ValueEmitter... arguments) {
        invoke(className, methodName, ValueType.VOID, arguments);
        return this;
    }

    public ProgramEmitter invoke(Class<?> cls, String methodName, ValueEmitter... arguments) {
        return invoke(cls.getName(), methodName, arguments);
    }

    public ValueEmitter construct(String className, ValueEmitter... arguments) {
        Variable var = program.createVariable();
        ConstructInstruction insn = new ConstructInstruction();
        insn.setReceiver(var);
        insn.setType(className);
        addInstruction(insn);
        ValueEmitter instance = var(var, ValueType.object(className));
        instance.invokeSpecial("<init>", void.class, arguments);
        return instance;
    }

    public ValueEmitter construct(Class<?> cls, ValueEmitter... arguments) {
        return construct(cls.getName(), arguments);
    }

    public ValueEmitter constructArray(ValueType type, ValueEmitter size) {
        Variable var = program.createVariable();
        ConstructArrayInstruction insn = new ConstructArrayInstruction();
        insn.setReceiver(var);
        insn.setSize(size.getVariable());
        insn.setItemType(type);
        addInstruction(insn);
        return var(var, ValueType.arrayOf(type));
    }

    public ValueEmitter constructArray(ValueType type, int size) {
        return constructArray(type, constant(size));
    }

    public ValueEmitter constructArray(Class<?> type, int size) {
        return constructArray(ValueType.parse(type), size);
    }

    public ValueEmitter constructArray(Class<?> type, ValueEmitter size) {
        return constructArray(ValueType.parse(type), size);
    }

    public ProgramEmitter initClass(String className) {
        InitClassInstruction insn = new InitClassInstruction();
        insn.setClassName(className);
        addInstruction(insn);
        return this;
    }

    public ProgramEmitter jump(BasicBlock block) {
        JumpInstruction insn = new JumpInstruction();
        insn.setTarget(block);
        addInstruction(insn);
        return this;
    }

    public void exit() {
        ExitInstruction insn = new ExitInstruction();
        addInstruction(insn);
    }

    public ValueEmitter var(Variable var, ValueType type) {
        return new ValueEmitter(this, block, var, type);
    }

    public ValueEmitter var(Variable var, Class<?> type) {
        return var(var, ValueType.parse(type));
    }

    public ValueEmitter var(Variable var, ClassReader type) {
        return var(var, ValueType.object(type.getName()));
    }

    public ValueEmitter var(int var, ValueType type) {
        return new ValueEmitter(this, block, program.variableAt(var), type);
    }

    public ValueEmitter var(int var, Class<?> type) {
        return var(var, ValueType.parse(type));
    }

    public ValueEmitter var(int var, ClassReader type) {
        return var(var, ValueType.object(type.getName()));
    }

    public ValueEmitter newVar(ValueType type) {
        return var(program.createVariable(), type);
    }

    public ValueEmitter newVar(ClassReader cls) {
        return var(program.createVariable(), ValueType.object(cls.getName()));
    }

    public ValueEmitter newVar(Class<?> type) {
        return var(program.createVariable(), type);
    }

    public TextLocation getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(TextLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    public void addInstruction(Instruction insn) {
        if (escapes()) {
            throw new EmitException("This block has already escaped");
        }
        if (currentLocation != null) {
            insn.setLocation(currentLocation);
        }
        block.add(insn);
    }

    public static ProgramEmitter create(MethodHolder method, ClassReaderSource classSource) {
        ProgramEmitter pe = create(method.getDescriptor(), classSource);
        method.setProgram(pe.getProgram());
        return pe;
    }

    public static ProgramEmitter create(MethodDescriptor method, ClassReaderSource classSource) {
        Program program = new Program();
        BasicBlock zeroBlock = program.createBasicBlock();
        BasicBlock block = program.createBasicBlock();

        JumpInstruction insn = new JumpInstruction();
        insn.setTarget(block);
        zeroBlock.add(insn);

        program.createVariable();
        for (int i = 0; i < method.parameterCount(); ++i) {
            program.createVariable();
        }

        return new ProgramEmitter(program, block, classSource);
    }

    public IfEmitter when(ConditionEmitter cond) {
        return new IfEmitter(this, cond.fork, prepareBlock());
    }

    public IfEmitter when(ConditionProducer cond) {
        return when(cond.produce());
    }

    public PhiEmitter phi(ValueType type, BasicBlock block) {
        ValueEmitter value = newVar(type);
        Phi phi = new Phi();
        phi.setReceiver(value.getVariable());
        block.getPhis().add(phi);
        return new PhiEmitter(phi, value);
    }

    public PhiEmitter phi(Class<?> cls, BasicBlock block) {
        return phi(ValueType.parse(cls), block);
    }

    public PhiEmitter phi(ClassReader cls, BasicBlock block) {
        return phi(ValueType.object(cls.getName()), block);
    }

    public PhiEmitter phi(ValueType type) {
        return phi(type, block);
    }

    public PhiEmitter phi(Class<?> cls) {
        return phi(ValueType.parse(cls));
    }

    public PhiEmitter phi(ClassReader cls) {
        return phi(ValueType.object(cls.getName()));
    }

    public ChooseEmitter choice(ValueEmitter value) {
        SwitchInstruction insn = new SwitchInstruction();
        insn.setCondition(value.getVariable());
        addInstruction(insn);
        return new ChooseEmitter(this, insn, prepareBlock());
    }

    public StringChooseEmitter stringChoice(ValueEmitter value) {
        SwitchInstruction insn = new SwitchInstruction();
        return new StringChooseEmitter(this, value, insn, prepareBlock());
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public boolean escapes() {
        Instruction insn = block.getLastInstruction();
        if (insn == null) {
            return false;
        }
        InstructionTransitionExtractor extractor = new InstructionTransitionExtractor();
        insn.acceptVisitor(extractor);
        return extractor.getTargets() != null;
    }

    public void emitAndJump(FragmentEmitter fragment, BasicBlock block) {
        fragment.emit();
        if (!escapes()) {
            jump(block);
        }
    }

    public StringBuilderEmitter string() {
        return new StringBuilderEmitter(this);
    }

    public static ProgramEmitter create(Program program, ClassReaderSource classSource) {
        return new ProgramEmitter(program, null, classSource);
    }
}
