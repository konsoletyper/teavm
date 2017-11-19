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
package org.teavm.model.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;

public class MissingItemsProcessor {
    private DependencyInfo dependencyInfo;
    private Diagnostics diagnostics;
    private List<Instruction> instructionsToAdd = new ArrayList<>();
    private MethodHolder methodHolder;
    private Program program;
    private Collection<String> reachableClasses;
    private Collection<MethodReference> reachableMethods;
    private Collection<FieldReference> reachableFields;

    public MissingItemsProcessor(DependencyInfo dependencyInfo, Diagnostics diagnostics) {
        this.dependencyInfo = dependencyInfo;
        this.diagnostics = diagnostics;
        reachableClasses = dependencyInfo.getReachableClasses();
        reachableMethods = dependencyInfo.getReachableMethods();
        reachableFields = dependencyInfo.getReachableFields();
    }

    public void processClass(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (reachableMethods.contains(method.getReference()) && method.getProgram() != null) {
                processMethod(method);
            }
        }
    }

    public void processMethod(MethodHolder method) {
        this.methodHolder = method;
        this.program = method.getProgram();
        boolean wasModified = false;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            instructionsToAdd.clear();
            boolean missing = false;
            for (Instruction insn : block) {
                insn.acceptVisitor(instructionProcessor);
                if (!instructionsToAdd.isEmpty()) {
                    wasModified = true;
                    truncateBlock(insn);
                    missing = true;
                    break;
                }
            }
            if (!missing) {
                for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                    checkClass(null, tryCatch.getExceptionType());
                }
            }
        }
        if (wasModified) {
            new UnreachableBasicBlockEliminator().optimize(program);
        }
    }

    private void truncateBlock(Instruction instruction) {
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        BasicBlock block = instruction.getBasicBlock();
        if (block.getLastInstruction() != null) {
            block.getLastInstruction().acceptVisitor(transitionExtractor);
        }
        for (BasicBlock successor : transitionExtractor.getTargets()) {
            successor.removeIncomingsFrom(block);
        }
        while (instruction.getNext() != null) {
            instruction.getNext().delete();
        }
        instruction.insertNextAll(instructionsToAdd);
        instruction.delete();
    }

    private void emitExceptionThrow(TextLocation location, String exceptionName, String text) {
        Variable exceptionVar = program.createVariable();
        ConstructInstruction newExceptionInsn = new ConstructInstruction();
        newExceptionInsn.setType(exceptionName);
        newExceptionInsn.setReceiver(exceptionVar);
        newExceptionInsn.setLocation(location);
        instructionsToAdd.add(newExceptionInsn);

        Variable constVar = program.createVariable();
        StringConstantInstruction constInsn = new StringConstantInstruction();
        constInsn.setConstant(text);
        constInsn.setReceiver(constVar);
        constInsn.setLocation(location);
        instructionsToAdd.add(constInsn);

        InvokeInstruction initExceptionInsn = new InvokeInstruction();
        initExceptionInsn.setInstance(exceptionVar);
        initExceptionInsn.setMethod(new MethodReference(exceptionName, "<init>", ValueType.object("java.lang.String"),
                ValueType.VOID));
        initExceptionInsn.setType(InvocationType.SPECIAL);
        initExceptionInsn.getArguments().add(constVar);
        initExceptionInsn.setLocation(location);
        instructionsToAdd.add(initExceptionInsn);

        RaiseInstruction raiseInsn = new RaiseInstruction();
        raiseInsn.setException(exceptionVar);
        raiseInsn.setLocation(location);
        instructionsToAdd.add(raiseInsn);
    }

    private boolean checkClass(TextLocation location, String className) {
        if (!reachableClasses.contains(className) || !dependencyInfo.getClass(className).isMissing()) {
            return true;
        }
        diagnostics.error(new CallLocation(methodHolder.getReference(), location), "Class {{c0}} was not found",
                className);
        emitExceptionThrow(location, NoClassDefFoundError.class.getName(), "Class not found: " + className);
        return false;
    }

    private boolean checkClass(TextLocation location, ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            return checkClass(location, ((ValueType.Object) type).getClassName());
        }
        return true;
    }

    private boolean checkMethod(TextLocation location, MethodReference method) {
        if (!checkClass(location, method.getClassName())) {
            return false;
        }
        if (!reachableMethods.contains(method)) {
            return true;
        }
        MethodDependencyInfo methodDep = dependencyInfo.getMethod(method);
        if (!methodDep.isMissing() || !methodDep.isUsed()) {
            return true;
        }

        diagnostics.error(new CallLocation(methodHolder.getReference(), location), "Method {{m0}} was not found",
                method);
        emitExceptionThrow(location, NoSuchMethodError.class.getName(), "Method not found: " + method);
        return true;
    }

    private boolean checkVirtualMethod(TextLocation location, MethodReference method) {
        if (!checkClass(location, method.getClassName())) {
            return false;
        }
        if (!reachableMethods.contains(method)) {
            return true;
        }

        if (dependencyInfo.getClassSource().resolve(method) != null) {
            return true;
        }

        diagnostics.error(new CallLocation(methodHolder.getReference(), location), "Method {{m0}} was not found",
                method);
        emitExceptionThrow(location, NoSuchMethodError.class.getName(), "Method not found: " + method);
        return true;
    }

    private boolean checkField(TextLocation location, FieldReference field) {
        if (!checkClass(location, field.getClassName())) {
            return false;
        }
        if (!reachableFields.contains(field) || !dependencyInfo.getField(field).isMissing()) {
            return true;
        }
        diagnostics.error(new CallLocation(methodHolder.getReference(), location), "Field {{f0}} was not found",
                field);
        emitExceptionThrow(location, NoSuchFieldError.class.getName(), "Field not found: " + field);
        return true;
    }

    private InstructionVisitor instructionProcessor = new InstructionVisitor() {
        @Override
        public void visit(NullCheckInstruction insn) {
        }

        @Override
        public void visit(InitClassInstruction insn) {
            checkClass(insn.getLocation(), insn.getClassName());
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            checkClass(insn.getLocation(), insn.getType());
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getType() != InvocationType.VIRTUAL) {
                checkMethod(insn.getLocation(), insn.getMethod());
            } else {
                checkVirtualMethod(insn.getLocation(), insn.getMethod());
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
        }

        @Override
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(GetElementInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            checkField(insn.getLocation(), insn.getField());
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            checkField(insn.getLocation(), insn.getField());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            checkClass(insn.getLocation(), insn.getItemType());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            checkClass(insn.getLocation(), insn.getType());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            checkClass(insn.getLocation(), insn.getItemType());
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
        }

        @Override
        public void visit(BranchingInstruction insn) {
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
        }

        @Override
        public void visit(CastNumberInstruction insn) {
        }

        @Override
        public void visit(CastInstruction insn) {
            checkClass(insn.getLocation(), insn.getTargetType());
        }

        @Override
        public void visit(AssignInstruction insn) {
        }

        @Override
        public void visit(NegateInstruction insn) {
        }

        @Override
        public void visit(BinaryInstruction insn) {
        }

        @Override
        public void visit(StringConstantInstruction insn) {
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
        }

        @Override
        public void visit(LongConstantInstruction insn) {
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
        }

        @Override
        public void visit(NullConstantInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            checkClass(insn.getLocation(), insn.getConstant());
        }

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
        }
    };
}
