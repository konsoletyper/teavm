/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.util.Arrays;
import org.teavm.common.DisjointSet;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

public class ClassForNameTransformer implements ClassHolderTransformer {
    private static final MethodReference getNameMethod = new MethodReference(Class.class, "getName", String.class);
    private static final MethodReference forNameMethod = new MethodReference(Class.class, "forName", String.class,
            boolean.class, ClassLoader.class, Class.class);
    private static final MethodReference forNameShortMethod = new MethodReference(Class.class, "forName",
            String.class, Class.class);
    private static final MethodReference initMethod = new MethodReference(Class.class, "initialize", void.class);

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                transformProgram(program, context.getHierarchy());
            }
        }
    }

    private void transformProgram(Program program, ClassHierarchy hierarchy) {
        if (!hasForNameCall(program)) {
            return;
        }

        DisjointSet varSet = new DisjointSet();
        for (int i = 0; i < program.variableCount(); i++) {
            varSet.create();
        }
        int[] nameIndexes = new int[program.variableCount()];
        String[] constants = new String[program.variableCount()];
        Arrays.fill(nameIndexes, -1);

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getMethod().equals(getNameMethod)) {
                        if (invoke.getReceiver() != null) {
                            nameIndexes[invoke.getReceiver().getIndex()] = invoke.getInstance().getIndex();
                        }
                    }
                } else if (instruction instanceof StringConstantInstruction) {
                    StringConstantInstruction stringConstant = (StringConstantInstruction) instruction;
                    constants[stringConstant.getReceiver().getIndex()] = stringConstant.getConstant();
                } else if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    varSet.union(assign.getAssignee().getIndex(), assign.getReceiver().getIndex());
                }
            }
        }

        nameIndexes = Arrays.copyOf(nameIndexes, varSet.size());
        int[] nameRepresentatives = new int[nameIndexes.length];
        Arrays.fill(nameRepresentatives, -1);
        String[] constantsByClasses = new String[varSet.size()];

        for (int i = 0; i < program.variableCount(); i++) {
            int varClass = varSet.find(i);
            if (nameRepresentatives[varClass] < 0) {
                nameRepresentatives[varClass] = i;
            }
            if (nameIndexes[i] >= 0) {
                nameIndexes[varClass] = varSet.find(nameIndexes[i]);
            }

            constantsByClasses[varClass] = constants[i];
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) instruction;

                if (!invoke.getMethod().equals(forNameMethod) && !invoke.getMethod().equals(forNameShortMethod)) {
                    continue;
                }

                Variable representative;

                int classNameIndex = invoke.getArguments().get(0).getIndex();
                int nameIndex = nameIndexes[classNameIndex];
                String constant = constantsByClasses[invoke.getArguments().get(0).getIndex()];
                if (nameIndex >= 0) {
                    representative = program.variableAt(nameRepresentatives[nameIndex]);
                } else if (constant != null) {
                    if (hierarchy.getClassSource().get(constant) == null || !filterClassName(constant)) {
                        InvokeInstruction invokeException = new InvokeInstruction();
                        invokeException.setType(InvocationType.SPECIAL);
                        invokeException.setMethod(new MethodReference(ExceptionHelpers.class, "classNotFound",
                                Class.class));
                        invokeException.setReceiver(program.createVariable());
                        invokeException.setLocation(invoke.getLocation());
                        invoke.insertPrevious(invokeException);
                        representative = invokeException.getReceiver();
                    } else {
                        ClassConstantInstruction classConstant = new ClassConstantInstruction();
                        classConstant.setConstant(ValueType.object(constant));
                        classConstant.setReceiver(program.createVariable());
                        classConstant.setLocation(invoke.getLocation());
                        invoke.insertPrevious(classConstant);
                        representative = classConstant.getReceiver();
                    }
                } else {
                    continue;
                }

                InvokeInstruction initInvoke = new InvokeInstruction();
                initInvoke.setLocation(invoke.getLocation());
                initInvoke.setType(InvocationType.SPECIAL);
                initInvoke.setMethod(initMethod);
                initInvoke.setInstance(representative);
                invoke.insertPrevious(initInvoke);

                if (invoke.getReceiver() == null) {
                    invoke.delete();
                } else {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setLocation(invoke.getLocation());
                    assign.setAssignee(representative);
                    assign.setReceiver(invoke.getReceiver());
                    invoke.replace(assign);
                }
            }
        }
    }

    private boolean hasForNameCall(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }

                InvokeInstruction invoke = (InvokeInstruction) instruction;

                if (invoke.getMethod().equals(forNameMethod) || invoke.getMethod().equals(forNameShortMethod)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean filterClassName(String className) {
        switch (className) {
            // It's a hack for Kotlin. Kotlin full reflection library is too heavyweight for TeaVM.
            // This optimization enables full reflection when there's Kotlin/JVM reflection library
            // in the classpath, since Kotlin uses Class.forName() to check whether there's
            // full reflection library presents. If program does not use full reflection,
            // but build configuration includes kotlin-reflect artifact as a dependency,
            // it gets into classpath, which allows this optimization to be applied.
            case "kotlin.reflect.jvm.internal.ReflectionFactoryImpl":
                return false;
        }
        return true;
    }
}
