/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.classlib.impl.regex;

import java.util.IdentityHashMap;
import java.util.Map;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

/**
 * Builds a program that consists of calls to pattern factory methods.
 */
class PatternProgramBuilder implements PatternConstructionListener {
    private ClassReader factoryClass;
    private Program program;
    private BasicBlock block;
    private Map<Object, Variable> nodes = new IdentityHashMap<>();

    PatternProgramBuilder(ClassReader factoryClass) {
        this.factoryClass = factoryClass;
        program = new Program();
        program.createVariable();
        block = program.createBasicBlock();
    }

    @Override
    public void create(Object node, String factoryMethod, Object[] arguments) {
        nodes.put(node, invoke(factoryMethod, arguments, true));
    }

    @Override
    public void call(String factoryMethod, Object[] arguments) {
        invoke(factoryMethod, arguments, false);
    }

    Program complete(Object root) {
        var exit = new ExitInstruction();
        exit.setValueToReturn(nodes.get(root));
        block.add(exit);
        return program;
    }

    private Variable invoke(String factoryMethod, Object[] arguments, boolean hasResult) {
        var method = findMethod(factoryMethod);
        if (method.parameterCount() != arguments.length || hasResult == (method.getResultType() == ValueType.VOID)) {
            throw new IllegalStateException("Wrong usage of " + method.getReference());
        }
        var argumentVars = new Variable[arguments.length];
        for (var i = 0; i < arguments.length; ++i) {
            argumentVars[i] = argument(arguments[i], method.parameterType(i));
        }

        var insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(method.getReference());
        insn.setArguments(argumentVars);
        if (hasResult) {
            insn.setReceiver(program.createVariable());
        }
        block.add(insn);
        return insn.getReceiver();
    }

    private MethodReader findMethod(String name) {
        MethodReader result = null;
        for (var method : factoryClass.getMethods()) {
            if (method.getName().equals(name) && method.hasModifier(ElementModifier.STATIC)) {
                if (result != null) {
                    throw new IllegalStateException("Ambiguous factory method " + name);
                }
                result = method;
            }
        }
        if (result == null) {
            throw new IllegalStateException("Factory method not found: " + name);
        }
        return result;
    }

    private Variable argument(Object value, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            int intValue;
            if (value instanceof Integer) {
                intValue = (Integer) value;
            } else if (value instanceof Character) {
                intValue = (Character) value;
            } else if (value instanceof Boolean) {
                intValue = (Boolean) value ? 1 : 0;
            } else {
                throw new IllegalStateException("Unexpected value for parameter of type " + type + ": " + value);
            }
            var insn = new IntegerConstantInstruction();
            insn.setConstant(intValue);
            insn.setReceiver(program.createVariable());
            block.add(insn);
            return insn.getReceiver();
        }
        if (value == null) {
            var insn = new NullConstantInstruction();
            insn.setReceiver(program.createVariable());
            block.add(insn);
            return insn.getReceiver();
        }
        if (value instanceof String) {
            var insn = new StringConstantInstruction();
            insn.setConstant((String) value);
            insn.setReceiver(program.createVariable());
            block.add(insn);
            return insn.getReceiver();
        }
        var variable = nodes.get(value);
        if (variable == null) {
            throw new IllegalStateException("Node is not created yet: " + value);
        }
        return variable;
    }
}
