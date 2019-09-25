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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class ProxyVariableContext extends VariableContext {
    private Map<ValueImpl<?>, Variable> cache = new HashMap<>();
    private BasicBlock startBlock;
    private ClassHolder proxyClass;
    private int suffixGenerator;
    private Map<Variable, CapturedValue> capturedValueMap = new HashMap<>();
    private List<CapturedValue> capturedValues = new ArrayList<>();
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", ValueType.VOID);

    public ProxyVariableContext(VariableContext parent, ClassHolder proxyClass) {
        super(parent);
        this.proxyClass = proxyClass;
    }

    public void init(BasicBlock startBlock) {
        this.startBlock = startBlock;
        cache.clear();
    }

    @Override
    public Variable emitVariable(ValueImpl<?> value, CallLocation location) {
        return cache.computeIfAbsent(value, v -> createVariable(v, location));
    }

    private Variable createVariable(ValueImpl<?> value, CallLocation location) {
        if (value.context == this) {
            return value.innerValue;
        }
        Variable outerVar = getParent().emitVariable(value, location);

        CapturedValue capturedValue = capturedValueMap.computeIfAbsent(outerVar, v -> {
            FieldHolder field = new FieldHolder("proxyCapture" + suffixGenerator++);
            field.setLevel(AccessLevel.PUBLIC);
            field.setType(value.type);
            proxyClass.addField(field);

            CapturedValue result = new CapturedValue(field, v);
            capturedValues.add(result);
            return result;
        });

        Program program = startBlock.getProgram();
        Variable var = program.createVariable();
        GetFieldInstruction insn = new GetFieldInstruction();
        insn.setInstance(program.variableAt(0));
        insn.setField(capturedValue.field.getReference());
        insn.setFieldType(capturedValue.field.getType());
        insn.setReceiver(var);
        startBlock.add(insn);

        return var;
    }

    public Variable createInstance(CompositeMethodGenerator generator) {
        ValueType[] signature = new ValueType[capturedValues.size() + 1];
        for (int i = 0; i < capturedValues.size(); ++i) {
            signature[i] = capturedValues.get(i).field.getType();
        }
        signature[capturedValues.size()] = ValueType.VOID;

        MethodHolder ctor = new MethodHolder("<init>", signature);
        ctor.setLevel(AccessLevel.PUBLIC);
        Program ctorProgram = new Program();
        ctor.setProgram(ctorProgram);
        BasicBlock ctorBlock = ctorProgram.createBasicBlock();

        InvokeInstruction invokeSuper = new InvokeInstruction();
        invokeSuper.setInstance(ctorProgram.createVariable());
        invokeSuper.setMethod(new MethodReference(proxyClass.getParent(), INIT_METHOD));
        invokeSuper.setType(InvocationType.SPECIAL);
        ctorBlock.add(invokeSuper);

        for (int i = 0; i < capturedValues.size(); ++i) {
            PutFieldInstruction putInsn = new PutFieldInstruction();
            putInsn.setField(capturedValues.get(i).field.getReference());
            putInsn.setFieldType(capturedValues.get(i).field.getType());
            putInsn.setValue(ctorProgram.createVariable());
            putInsn.setInstance(ctorProgram.variableAt(0));
            ctorBlock.add(putInsn);
        }

        ExitInstruction exit = new ExitInstruction();
        ctorBlock.add(exit);

        proxyClass.addMethod(ctor);

        ConstructInstruction constructInsn = new ConstructInstruction();
        constructInsn.setReceiver(generator.program.createVariable());
        constructInsn.setType(proxyClass.getName());
        generator.add(constructInsn);

        InvokeInstruction initInsn = new InvokeInstruction();
        initInsn.setInstance(constructInsn.getReceiver());
        initInsn.setMethod(ctor.getReference());
        initInsn.setType(InvocationType.SPECIAL);
        Variable[] initArgs = new Variable[capturedValues.size()];
        for (int i = 0; i < capturedValues.size(); ++i) {
            initArgs[i] =  capturedValues.get(i).value;
        }
        initInsn.setArguments(initArgs);
        generator.add(initInsn);

        return constructInsn.getReceiver();
    }

    class CapturedValue {
        FieldHolder field;
        Variable value;

        CapturedValue(FieldHolder field, Variable value) {
            this.field = field;
            this.value = value;
        }
    }
}
