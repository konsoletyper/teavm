/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.SupportedOn;
import org.teavm.interop.UnsupportedOn;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.ProgramUtils;

public class ReferenceResolver {
    private ClassReaderSource classSource;
    private MethodReference currentMethod;
    private Program program;
    private boolean modified;
    private List<Instruction> instructionsToAdd = new ArrayList<>();
    private Map<FieldReference, FieldWrapper> fieldCache = new HashMap<>();
    private Map<String, Map<MethodDescriptor, Optional<MethodReader>>> methodCache = new HashMap<>(1000, 0.5f);
    private Set<String> platformTags = new HashSet<>();
    private UnreachableBasicBlockEliminator unreachableBlockEliminator;
    private Map<MethodReference, List<Consumer<Diagnostics>>> pendingErrors = new HashMap<>();
    private boolean shouldStop;

    public ReferenceResolver(ClassReaderSource classSource, String[] platformTags) {
        this.classSource = classSource;
        this.platformTags.addAll(List.of(platformTags));
        unreachableBlockEliminator = new UnreachableBasicBlockEliminator();
    }

    public void use(MethodReference method, Diagnostics diagnostics) {
        var errors = pendingErrors.remove(method);
        if (errors != null) {
            for (var error : errors) {
                error.accept(diagnostics);
            }
        }
    }

    public Program resolve(MethodHolder method, Program program) {
        this.currentMethod = method.getReference();
        this.program = program;
        for (var block : program.getBasicBlocks()) {
            shouldStop = false;
            for (var insn : block) {
                insn.acceptVisitor(visitor);
                if (shouldStop) {
                    break;
                }
            }
        }
        if (modified) {
            unreachableBlockEliminator.optimize(program);
            modified = false;
        }
        this.program = null;
        this.currentMethod = null;
        return program;
    }

    private InstructionVisitor visitor = new AbstractInstructionVisitor() {
        @Override
        public void visit(InvokeInstruction insn) {
            if (!resolve(insn)) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (!resolve(insn)) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (!resolve(insn)) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(CastInstruction insn) {
            if (!checkType(insn, insn.getTargetType())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            if (!checkType(insn, insn.getType())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(InitClassInstruction insn) {
            if (!checkClass(insn, insn.getClassName())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(ConstructInstruction insn) {
            if (!checkClass(insn, insn.getType())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            if (!checkType(insn, insn.getItemType())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            if (!checkType(insn, insn.getItemType())) {
                shouldStop = true;
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            if (!checkType(insn, insn.getConstant())) {
                shouldStop = true;
            }
        }
    };

    private boolean resolve(InvokeInstruction instruction) {
        var calledRef = instruction.getMethod();

        if (!checkClass(instruction, calledRef.getClassName())) {
            return false;
        }

        if (instruction.getType() == InvocationType.SPECIAL) {
            return resolveSpecial(instruction, calledRef);
        } else {
            return resolveVirtual(instruction, calledRef);
        }
    }

    private boolean resolve(GetFieldInstruction instruction) {
        var resolvedField = resolve(instruction.getField());
        if (resolvedField == null) {
            reportError(instruction.getLocation(), "Field {{f0}} was not found", instruction.getField());
            emitExceptionThrow(instruction.getLocation(), NoSuchFieldError.class.getName(),
                    "Field not found: " + instruction.getField());
            truncateBlock(instruction);
            return false;
        }
        instruction.setField(resolvedField.getReference());
        return true;
    }

    private boolean resolve(PutFieldInstruction instruction) {
        var resolvedField = resolve(instruction.getField());
        if (resolvedField == null) {
            reportError(instruction.getLocation(), "Field {{f0}} was not found", instruction.getField());
            emitExceptionThrow(instruction.getLocation(), NoSuchFieldError.class.getName(),
                    "Field not found: " + instruction.getField());
            truncateBlock(instruction);
            return false;
        }
        instruction.setField(resolvedField.getReference());
        return true;
    }

    private boolean resolveSpecial(InvokeInstruction instruction, MethodReference calledRef) {
        var resolvedMethod = resolve(calledRef);
        if (resolvedMethod == null) {
            reportError(instruction.getLocation(), "Method {{m0}} was not found", calledRef);
            emitExceptionThrow(instruction.getLocation(), NoSuchMethodError.class.getName(),
                    "Method not found: " + instruction.getMethod());
            truncateBlock(instruction);
            return false;
        }
        if (!checkMethod(instruction, resolvedMethod.getReference())) {
            return false;
        }

        instruction.setMethod(resolvedMethod.getReference());
        return true;
    }

    private boolean resolveVirtual(InvokeInstruction instruction, MethodReference calledRef) {
        var resolvedMethod = resolve(calledRef);
        if (resolvedMethod == null) {
            reportError(instruction.getLocation(), "Method {{m0}} was not found", calledRef);
            emitExceptionThrow(instruction.getLocation(), NoSuchMethodError.class.getName(),
                    "Method not found: " + instruction.getMethod());
            truncateBlock(instruction);
            return false;
        }

        if (!checkMethod(instruction, resolvedMethod.getReference())) {
            return false;
        }

        boolean isFinal = false;
        if (resolvedMethod.hasModifier(ElementModifier.FINAL)
                || resolvedMethod.getLevel() == AccessLevel.PRIVATE) {
            isFinal = true;
        } else {
            var cls = classSource.get(resolvedMethod.getOwnerName());
            if (cls.hasModifier(ElementModifier.FINAL)) {
                isFinal = true;
            }
        }
        if (isFinal) {
            instruction.setType(InvocationType.SPECIAL);
            instruction.setMethod(resolvedMethod.getReference());
        }
        return true;
    }

    public FieldReader resolve(FieldReference field) {
        return fieldCache.computeIfAbsent(field, f -> {
            var result = classSource.resolve(f);
            return new FieldWrapper(result);
        }).value;
    }

    private boolean checkType(Instruction instruction, ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            return checkClass(instruction, ((ValueType.Object) type).getClassName());
        }
        return true;
    }

    private boolean checkClass(Instruction instruction, String className) {
        var cls = classSource.get(className);
        if (cls == null) {
            reportError(instruction.getLocation(), "Class {{c0}} was not found", className);
            emitExceptionThrow(instruction.getLocation(), NoClassDefFoundError.class.getName(),
                    "Class not found: " + className);
            truncateBlock(instruction);
            return false;
        }
        if (!checkPlatformSupported(cls.getAnnotations())) {
            reportError(instruction.getLocation(), "Class {{c0}} is not supported on current target", className);
            emitExceptionThrow(instruction.getLocation(), NoClassDefFoundError.class.getName(),
                    "Class not found: " + className);
            truncateBlock(instruction);
            return false;
        }
        return true;
    }

    private boolean checkMethod(Instruction instruction, MethodReference methodRef) {
        var cls = classSource.get(methodRef.getClassName());
        if (cls != null) {
            var methodReader = cls.getMethod(methodRef.getDescriptor());
            if (methodReader != null && !checkPlatformSupported(methodReader.getAnnotations())) {
                reportError(instruction.getLocation(), "Method {{m0}} is not supported on current target", methodRef);
                emitExceptionThrow(instruction.getLocation(), NoSuchMethodError.class.getName(),
                        "Method not found: " + methodRef);
                truncateBlock(instruction);
                return false;
            }
        }
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

    public MethodReader resolve(MethodReference method) {
        return methodCache
                .computeIfAbsent(method.getClassName(), k -> new HashMap<>(100, 0.5f))
                .computeIfAbsent(method.getDescriptor(), k -> {
                    var methodReader = classSource.resolve(method);
                    return Optional.ofNullable(methodReader);
                })
                .orElse(null);
    }


    private void truncateBlock(Instruction instruction) {
        modified = true;
        ProgramUtils.truncateBlock(instruction);
        instruction.insertNextAll(instructionsToAdd);
        instructionsToAdd.clear();
        instruction.delete();
    }

    private void emitExceptionThrow(TextLocation location, String exceptionName, String text) {
        var exceptionVar = program.createVariable();
        var newExceptionInsn = new ConstructInstruction();
        newExceptionInsn.setType(exceptionName);
        newExceptionInsn.setReceiver(exceptionVar);
        newExceptionInsn.setLocation(location);
        instructionsToAdd.add(newExceptionInsn);

        var constVar = program.createVariable();
        var constInsn = new StringConstantInstruction();
        constInsn.setConstant(text);
        constInsn.setReceiver(constVar);
        constInsn.setLocation(location);
        instructionsToAdd.add(constInsn);

        var initExceptionInsn = new InvokeInstruction();
        initExceptionInsn.setInstance(exceptionVar);
        initExceptionInsn.setMethod(new MethodReference(exceptionName, "<init>", ValueType.object("java.lang.String"),
                ValueType.VOID));
        initExceptionInsn.setType(InvocationType.SPECIAL);
        initExceptionInsn.setArguments(constVar);
        initExceptionInsn.setLocation(location);
        instructionsToAdd.add(initExceptionInsn);

        var raiseInsn = new RaiseInstruction();
        raiseInsn.setException(exceptionVar);
        raiseInsn.setLocation(location);
        instructionsToAdd.add(raiseInsn);
    }

    private void reportError(TextLocation location, String message, Object param) {
        var method = currentMethod;
        pendingErrors.computeIfAbsent(method, k -> new ArrayList<>()).add(diagnostics ->
            diagnostics.error(new CallLocation(method, location), message, param));
    }

    private static class FieldWrapper {
        final FieldReader value;

        FieldWrapper(FieldReader value) {
            this.value = value;
        }
    }
}
