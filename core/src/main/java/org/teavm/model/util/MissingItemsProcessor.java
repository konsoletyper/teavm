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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.SupportedOn;
import org.teavm.interop.UnsupportedOn;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;

public class MissingItemsProcessor {
    private DependencyInfo dependencyInfo;
    private ClassHierarchy hierarchy;
    private Diagnostics diagnostics;
    private List<Instruction> instructionsToAdd = new ArrayList<>();
    private MethodReference methodRef;
    private Program program;
    private Collection<String> reachableClasses;
    private Collection<MethodReference> reachableMethods;
    private Collection<FieldReference> reachableFields;
    private Set<String> platformTags = new HashSet<>();

    public MissingItemsProcessor(DependencyInfo dependencyInfo, ClassHierarchy hierarchy, Diagnostics diagnostics,
            String[] platformTags) {
        this.dependencyInfo = dependencyInfo;
        this.diagnostics = diagnostics;
        this.hierarchy = hierarchy;
        reachableClasses = dependencyInfo.getReachableClasses();
        reachableMethods = dependencyInfo.getReachableMethods();
        reachableFields = dependencyInfo.getReachableFields();
        this.platformTags.addAll(Arrays.asList(platformTags));
    }

    public void processClass(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (reachableMethods.contains(method.getReference()) && method.getProgram() != null) {
                processMethod(method);
            }
        }
    }

    public void processMethod(MethodHolder method) {
        processMethod(method.getReference(), method.getProgram());
    }

    public void processMethod(MethodReference method, Program program) {
        this.methodRef = method;
        this.program = program;
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
        TransitionExtractor transitionExtractor = new TransitionExtractor();
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
        initExceptionInsn.setArguments(constVar);
        initExceptionInsn.setLocation(location);
        instructionsToAdd.add(initExceptionInsn);

        RaiseInstruction raiseInsn = new RaiseInstruction();
        raiseInsn.setException(exceptionVar);
        raiseInsn.setLocation(location);
        instructionsToAdd.add(raiseInsn);
    }

    private boolean checkClass(TextLocation location, String className) {
        if (!reachableClasses.contains(className)) {
            return false;
        }

        if (!dependencyInfo.getClass(className).isMissing()) {
            ClassReader cls = dependencyInfo.getClassSource().get(className);
            if (cls != null && !checkPlatformSupported(cls.getAnnotations())) {
                diagnostics.error(new CallLocation(methodRef, location), "Class {{c0}} is not supported on "
                        + "current target", className);
                emitExceptionThrow(location, NoClassDefFoundError.class.getName(), "Class not found: " + className);
                return false;
            }
            return true;
        }

        diagnostics.error(new CallLocation(methodRef, location), "Class {{c0}} was not found",
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
        if (!methodDep.isUsed()) {
            return true;
        }

        if (!methodDep.isMissing()) {
            ClassReader cls = dependencyInfo.getClassSource().get(method.getClassName());
            if (cls != null) {
                MethodReader methodReader = cls.getMethod(method.getDescriptor());
                if (methodReader != null && !checkPlatformSupported(methodReader.getAnnotations())) {
                    diagnostics.error(new CallLocation(methodRef, location), "Method {{m0}} is not supported on "
                            + "current target", method);
                    emitExceptionThrow(location, NoSuchMethodError.class.getName(), "Method not found: " + method);
                    return false;
                }
            }
            return true;
        }

        diagnostics.error(new CallLocation(methodRef, location), "Method {{m0}} was not found",
                method);
        emitExceptionThrow(location, NoSuchMethodError.class.getName(), "Method not found: " + method);
        return false;
    }

    private boolean checkVirtualMethod(TextLocation location, MethodReference method) {
        if (!checkClass(location, method.getClassName())) {
            return false;
        }
        if (!reachableMethods.contains(method)) {
            return true;
        }

        if (hierarchy.resolve(method) != null) {
            return true;
        }

        diagnostics.error(new CallLocation(methodRef, location), "Method {{m0}} was not found",
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
        diagnostics.error(new CallLocation(methodRef, location), "Field {{f0}} was not found",
                field);
        emitExceptionThrow(location, NoSuchFieldError.class.getName(), "Field not found: " + field);
        return true;
    }

    private boolean checkPlatformSupported(AnnotationContainerReader annotations) {
        AnnotationReader supportedAnnot = annotations.get(SupportedOn.class.getName());
        AnnotationReader unsupportedAnnot = annotations.get(UnsupportedOn.class.getName());
        if (supportedAnnot != null) {
            for (AnnotationValue value : supportedAnnot.getValue("value").getList()) {
                if (platformTags.contains(value.getString())) {
                    return true;
                }
            }
            return false;
        }
        if (unsupportedAnnot != null) {
            for (AnnotationValue value : unsupportedAnnot.getValue("value").getList()) {
                if (platformTags.contains(value.getString())) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private InstructionVisitor instructionProcessor = new AbstractInstructionVisitor() {
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
        public void visit(CastInstruction insn) {
            checkClass(insn.getLocation(), insn.getTargetType());
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            checkClass(insn.getLocation(), insn.getConstant());
        }
    };
}
