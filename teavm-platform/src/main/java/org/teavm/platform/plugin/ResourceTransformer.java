/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.*;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.platform.metadata.Resource;

/**
 *
 * @author Alexey Andreev
 */
class ResourceTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                transformProgram(innerSource, program);
            }
        }
    }

    private void transformProgram(ClassReaderSource innerSource, Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transformBasicBlock(innerSource, program.basicBlockAt(i));
        }
    }

    private void transformBasicBlock(ClassReaderSource innerSource, BasicBlock block) {
        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction insn = instructions.get(i);
            if (insn instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction)insn;
                List<Instruction> replacement = transformInvoke(innerSource, invoke);
                if (replacement != null) {
                    instructions.set(i, new EmptyInstruction());
                    instructions.addAll(i, replacement);
                    i += replacement.size();
                }
            }
        }
    }

    private List<Instruction> transformInvoke(ClassReaderSource innerSource, InvokeInstruction insn) {
        if (insn.getType() != InvocationType.VIRTUAL) {
            return null;
        }
        MethodReference method = insn.getMethod();
        ClassReader iface = innerSource.get(method.getClassName());
        if (iface.getAnnotations().get(Resource.class.getName()) == null) {
            return null;
        }
        if (method.getName().startsWith("get")) {
            if (method.getName().length() > 3) {
                return transformGetterInvocation(insn, method.getName().substring(3));
            }
        } else if (method.getName().startsWith("is")) {
            if (method.getName().length() > 2) {
                return transformGetterInvocation(insn, method.getName().substring(2));
            }
        } else if (method.getName().startsWith("set")) {
            if (method.getName().length() > 3) {
                return transformSetterInvocation(insn, method.getName().substring(3));
            }
        }
        return null;
    }

    private List<Instruction> transformGetterInvocation(InvokeInstruction insn, String property) {
        if (insn.getReceiver() == null) {
            return Collections.emptyList();
        }
        ValueType type = insn.getMethod().getDescriptor().getResultType();
        Program program = insn.getProgram();
        List<Instruction> instructions = new ArrayList<>();
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    Variable nameVar = program.createVariable();
                    Variable resultVar = program.createVariable();
                    StringConstantInstruction nameInsn = new StringConstantInstruction();
                    nameInsn.setConstant(property);
                    nameInsn.setReceiver(nameVar);
                    instructions.add(nameInsn);
                    InvokeInstruction accessorInvoke = new InvokeInstruction();
                    accessorInvoke.setType(InvocationType.SPECIAL);
                    accessorInvoke.setMethod(new MethodReference(ResourceAccessor.class, "get",
                            Object.class, String.class, Object.class));
                    accessorInvoke.getArguments().add(insn.getInstance());
                    accessorInvoke.getArguments().add(nameVar);
                    accessorInvoke.setReceiver(resultVar);
                    instructions.add(accessorInvoke);
                    InvokeInstruction castInvoke = new InvokeInstruction();
                    castInvoke.setType(InvocationType.SPECIAL);
                    castInvoke.setMethod(new MethodReference(ResourceAccessor.class, "castToBoolean", Object.class,
                            boolean.class));
                    castInvoke.getArguments().add(resultVar);
                    castInvoke.setReceiver(insn.getReceiver());
                    return instructions;
            }
        } else if (type instanceof ValueType.Object) {

        }
        return null;
    }

    private List<Instruction> transformSetterInvocation(InvokeInstruction insn, String property) {
        return null;
    }

    private String getPropertyName(String name) {
        if (name.length() == 1) {
            return name;
        }
        if (Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
