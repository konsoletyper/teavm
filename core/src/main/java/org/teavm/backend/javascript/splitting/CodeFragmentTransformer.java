/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.javascript.splitting;

import java.util.Arrays;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.IntegerStack;
import org.teavm.interop.CodeFragment;
import org.teavm.interop.CodeLoader;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class CodeFragmentTransformer implements ClassHolderTransformer {
    public static final String LAUNCHER_NAME = "$$launchAsync$$";
    private static final MethodReference SPLIT_METHOD = new MethodReference(CodeLoader.class, "split",
            CodeFragment.class, void.class);

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                replaceCallSites(method.getReference(), program, context);
            }
        }

        if (context.getHierarchy().isSuperType(CodeFragment.class.getName(), cls.getName(), false)
                && !cls.getName().equals(CodeFragment.class.getName())) {
            addLauncherMethod(cls);
        }
    }

    private void replaceCallSites(MethodReference method, Program program, ClassHolderTransformerContext context) {
        boolean hasSplitCalls = false;
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getType() == InvocationType.SPECIAL && invoke.getMethod().equals(SPLIT_METHOD)) {
                        hasSplitCalls = true;
                        break;
                    }
                }
            }
        }
        if (!hasSplitCalls) {
            return;
        }

        String[] classNames = new String[program.variableCount()];
        int[] assignPath = new int[program.variableCount()];
        Arrays.fill(assignPath, -1);
        IntegerStack stack = new IntegerStack(program.variableCount());
        boolean hasAssignPaths = false;

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof ConstructInstruction) {
                    ConstructInstruction construct = (ConstructInstruction) instruction;
                    classNames[construct.getReceiver().getIndex()] = construct.getType();
                    stack.push(construct.getReceiver().getIndex());
                } else if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    assignPath[assign.getReceiver().getIndex()] = assign.getAssignee().getIndex();
                    hasAssignPaths = true;
                }
            }
        }

        if (hasAssignPaths) {
            for (int i = 0; i < assignPath.length; ++i) {
                if (assignPath[i] >= 0) {
                    propagateAssigns(classNames, assignPath, i);
                }
            }
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (instruction instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) instruction;
                    if (invoke.getType() == InvocationType.SPECIAL && invoke.getMethod().equals(SPLIT_METHOD)) {
                        Variable arg = invoke.getArguments().get(0);
                        String className = classNames[arg.getIndex()];
                        if (className == null) {
                            context.getDiagnostics().error(new CallLocation(method, invoke.getLocation()),
                                    "{{m0}} should be called with lambda defined locally",
                                    SPLIT_METHOD);
                            continue;
                        }

                        InvokeInstruction replacement = new InvokeInstruction();
                        replacement.setLocation(instruction.getLocation());
                        replacement.setType(InvocationType.SPECIAL);
                        replacement.setMethod(new MethodReference(className, LAUNCHER_NAME));
                        replacement.setInstance(arg);
                        invoke.replace(replacement);
                    }
                }
            }
        }
    }

    private void propagateAssigns(String[] classNames, int[] assignPaths, int to) {
        int from = assignPaths[to];
        if (from < 0) {
            return;
        }
        assignPaths[to] = -1;
        propagateAssigns(classNames, assignPaths, from);
        classNames[to] = classNames[from];
    }

    private void addLauncherMethod(ClassHolder cls) {
        MethodHolder method = new MethodHolder(LAUNCHER_NAME, ValueType.VOID);
        method.setLevel(AccessLevel.PUBLIC);
        method.getModifiers().add(ElementModifier.NATIVE);
        method.getAnnotations().add(new AnnotationHolder(AsyncLauncher.class.getName()));
        cls.addMethod(method);
    }
}
