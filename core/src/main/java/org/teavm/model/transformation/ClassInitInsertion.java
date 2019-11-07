/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.transformation;

import java.util.HashSet;
import java.util.Set;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.DominatorWalker;
import org.teavm.model.util.DominatorWalkerCallback;
import org.teavm.model.util.DominatorWalkerContext;

public class ClassInitInsertion {
    private static final MethodDescriptor CLINIT = new MethodDescriptor("<clinit>", void.class);
    private DependencyInfo dependencyInfo;

    public ClassInitInsertion(DependencyInfo dependencyInfo) {
        this.dependencyInfo = dependencyInfo;
    }

    public void apply(Program program, MethodReader method) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        String currentClass = null;
        if (method.hasModifier(ElementModifier.STATIC)) {
            currentClass = method.getOwnerName();
        }
        Visitor visitor = new Visitor(currentClass);
        new DominatorWalker(program).walk(visitor);
    }

    class Visitor extends AbstractInstructionVisitor implements DominatorWalkerCallback<State> {
        private String currentClass;
        private DominatorWalkerContext context;
        Set<String> initializedClasses = new HashSet<>();
        private State state;

        Visitor(String currentClass) {
            this.currentClass = currentClass;
            if (currentClass != null) {
                initializedClasses.add(currentClass);
            }
        }

        @Override
        public void setContext(DominatorWalkerContext context) {
            this.context = context;
        }

        @Override
        public State visit(BasicBlock block) {
            state = new State();
            if (context.isExceptionHandler(block.getIndex())) {
                markAllClassesAsNotInitialized();
                if (currentClass != null) {
                    markClassAsInitialized(currentClass);
                }
            }

            for (Instruction instruction : block) {
                instruction.acceptVisitor(this);
            }

            return state;
        }

        @Override
        public void endVisit(BasicBlock block, State state) {
            if (state.oldInitializedClasses != null) {
                initializedClasses.clear();
                initializedClasses.addAll(state.oldInitializedClasses);
            } else {
                initializedClasses.removeAll(state.newInitializedClasses);
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() == null) {
                initializeClass(insn.getField().getClassName(), insn);
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() == null) {
                initializeClass(insn.getField().getClassName(), insn);
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() == null) {
                markClassAsInitialized(insn.getMethod().getClassName());
            }
        }

        @Override
        public void visit(InitClassInstruction insn) {
            markClassAsInitialized(insn.getClassName());
        }

        private void initializeClass(String className, Instruction instruction) {
            if (markClassAsInitialized(className)) {
                ClassReader cls = dependencyInfo.getClassSource().get(className);
                if (cls == null || cls.getMethod(CLINIT) != null) {
                    InitClassInstruction initInsn = new InitClassInstruction();
                    initInsn.setClassName(className);
                    initInsn.setLocation(instruction.getLocation());
                    instruction.insertPrevious(initInsn);
                }
            }
        }

        boolean markClassAsInitialized(String className) {
            if (initializedClasses.add(className)) {
                if (state.newInitializedClasses != null) {
                    state.newInitializedClasses.add(className);
                }
                return true;
            }
            return false;
        }

        private void markAllClassesAsNotInitialized() {
            if (state.newInitializedClasses != null) {
                state.oldInitializedClasses = new HashSet<>();
                for (String className : initializedClasses) {
                    if (!state.newInitializedClasses.contains(className)) {
                        state.oldInitializedClasses.add(className);
                    }
                }
                state.newInitializedClasses = null;
            }
            initializedClasses.clear();
        }
    }

    static class State {
        Set<String> newInitializedClasses = new HashSet<>();
        Set<String> oldInitializedClasses;
    }
}
