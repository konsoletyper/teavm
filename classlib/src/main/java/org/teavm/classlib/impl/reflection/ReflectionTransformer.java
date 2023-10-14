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
package org.teavm.classlib.impl.reflection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.teavm.common.DisjointSet;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.ProgramUtils;

public class ReflectionTransformer implements ClassHolderTransformer {
    private static final MethodReference getNameMethod = new MethodReference(Class.class, "getName", String.class);
    private static final MethodReference forNameMethod = new MethodReference(Class.class, "forName", String.class,
            boolean.class, ClassLoader.class, Class.class);
    private static final MethodReference forNameShortMethod = new MethodReference(Class.class, "forName",
            String.class, Class.class);
    private static final MethodReference newRefUpdaterMethod = new MethodReference(AtomicReferenceFieldUpdater.class,
            "newUpdater", Class.class, Class.class, String.class, AtomicReferenceFieldUpdater.class);
    private static final MethodReference newIntUpdaterMethod = new MethodReference(AtomicIntegerFieldUpdater.class,
            "newUpdater", Class.class, String.class, AtomicIntegerFieldUpdater.class);
    private static final MethodReference newLongUpdaterMethod = new MethodReference(AtomicLongFieldUpdater.class,
            "newUpdater", Class.class, String.class, AtomicLongFieldUpdater.class);
    private static final MethodReference initMethod = new MethodReference(Class.class, "initialize", void.class);

    private Map<String, String> updaterClasses = new HashMap<>();

    private boolean prepared;
    private DisjointSet varSet;
    private int[] nameRepresentatives;
    private String[] stringConstantsByClasses;
    private ValueType[] classConstantsByClasses;
    private boolean hasTruncatedBlocks;

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program != null) {
                transformProgram(program, context);
            }
        }
    }

    private void transformProgram(Program program, ClassHolderTransformerContext context) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) instruction;

                var method = invoke.getMethod();
                if (method.equals(forNameMethod) || method.equals(forNameShortMethod)) {
                    transformForName(program, invoke, context);
                } else if (method.equals(newRefUpdaterMethod)) {
                    transformRefUpdater(program, invoke, context);
                } else if (method.equals(newIntUpdaterMethod)) {
                    transformPrimitiveUpdater(program, invoke,
                            "java.util.concurrent.atomic.BaseAtomicIntegerFieldUpdater", ValueType.INTEGER, context);
                } else if (method.equals(newLongUpdaterMethod)) {
                    transformPrimitiveUpdater(program, invoke,
                            "java.util.concurrent.atomic.BaseAtomicLongFieldUpdater", ValueType.LONG, context);
                }
            }
        }
        if (hasTruncatedBlocks) {
            new UnreachableBasicBlockEliminator().optimize(program);
        }
        cleanup();
    }

    private void prepare(Program program) {
        if (prepared) {
            return;
        }
        prepared = true;

        varSet = new DisjointSet();
        for (int i = 0; i < program.variableCount(); i++) {
            varSet.create();
        }
        var nameIndexes = new int[program.variableCount()];
        var stringConstants = new String[program.variableCount()];
        var classConstants = new ValueType[program.variableCount()];
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
                    stringConstants[stringConstant.getReceiver().getIndex()] = stringConstant.getConstant();
                } else if (instruction instanceof ClassConstantInstruction) {
                    var classConstant = (ClassConstantInstruction) instruction;
                    classConstants[classConstant.getReceiver().getIndex()] = classConstant.getConstant();
                } else if (instruction instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) instruction;
                    varSet.union(assign.getAssignee().getIndex(), assign.getReceiver().getIndex());
                }
            }
        }

        nameRepresentatives = new int[varSet.size()];
        Arrays.fill(nameRepresentatives, -1);
        stringConstantsByClasses = new String[varSet.size()];
        classConstantsByClasses = new ValueType[varSet.size()];

        for (int i = 0; i < program.variableCount(); i++) {
            int varClass = varSet.find(i);
            if (nameIndexes[i] >= 0) {
                nameRepresentatives[varClass] = varSet.find(nameIndexes[i]);
            }

            stringConstantsByClasses[varClass] = stringConstants[i];
            classConstantsByClasses[varClass] = classConstants[i];
        }
    }

    private void cleanup() {
        if (prepared) {
            prepared = false;
            hasTruncatedBlocks = false;
            varSet = null;
            nameRepresentatives = null;
            stringConstantsByClasses = null;
            classConstantsByClasses = null;
        }
    }

    private void transformForName(Program program, InvokeInstruction invoke, ClassHolderTransformerContext context) {
        var hierarchy = context.getHierarchy();
        prepare(program);

        Variable representative;

        int classNameIndex = varSet.find(invoke.getArguments().get(0).getIndex());
        var nameIndex = nameRepresentatives[classNameIndex];
        String constant = stringConstantsByClasses[classNameIndex];
        if (nameIndex >= 0) {
            representative = program.variableAt(nameIndex);
        } else if (constant != null) {
            if (hierarchy.getClassSource().get(constant) == null || !filterClassName(constant)) {
                emitException(invoke, ClassNotFoundException.class);
                return;
            } else {
                ClassConstantInstruction classConstant = new ClassConstantInstruction();
                classConstant.setConstant(ValueType.object(constant));
                classConstant.setReceiver(program.createVariable());
                classConstant.setLocation(invoke.getLocation());
                invoke.insertPrevious(classConstant);
                representative = classConstant.getReceiver();
            }
        } else {
            return;
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

    private void transformRefUpdater(Program program, InvokeInstruction invoke,
            ClassHolderTransformerContext context) {
        prepare(program);

        var targetTypeConstant = classConstantsByClasses[varSet.find(invoke.getArguments().get(0).getIndex())];
        var varTypeConstant = classConstantsByClasses[varSet.find(invoke.getArguments().get(1).getIndex())];
        var nameConstant = stringConstantsByClasses[varSet.find(invoke.getArguments().get(2).getIndex())];

        if (targetTypeConstant == null || varTypeConstant == null || nameConstant == null) {
            return;
        }

        if (!(targetTypeConstant instanceof ValueType.Object)) {
            emitException(invoke, IllegalArgumentException.class);
            return;
        }

        var className = ((ValueType.Object) targetTypeConstant).getClassName();
        var cls = context.getHierarchy().getClassSource().get(className);
        if (cls == null) {
            emitException(invoke, NoClassDefFoundError.class);
            return;
        }

        var field = cls.getField(nameConstant);
        if (field == null) {
            emitException(invoke, RuntimeException.class, NoSuchFieldException.class);
            return;
        }

        if (!field.getType().equals(varTypeConstant)) {
            emitException(invoke, ClassCastException.class);
            return;
        }

        if (!field.hasModifier(ElementModifier.VOLATILE) || varTypeConstant instanceof ValueType.Primitive
                || varTypeConstant == ValueType.VOID || field.hasModifier(ElementModifier.STATIC)) {
            emitException(invoke, IllegalArgumentException.class);
            return;
        }

        var updaterClassName = getRefUpdaterClass(context, field);
        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference(updaterClassName, "INSTANCE"));
        getField.setFieldType(ValueType.object(updaterClassName));
        getField.setLocation(invoke.getLocation());
        getField.setReceiver(invoke.getReceiver());
        invoke.replace(getField);
    }

    private String getRefUpdaterClass(ClassHolderTransformerContext context, FieldReader field) {
        var key = field.getReference().toString();
        return updaterClasses.computeIfAbsent(key, k -> createRefUpdaterClass(context, field));
    }

    private String createRefUpdaterClass(ClassHolderTransformerContext context, FieldReader field) {
        var className = field.getOwnerName() + "$" + field.getName() + "$_AtomicUpdater$";
        var updaterClass = new ClassHolder(className);
        updaterClass.setLevel(AccessLevel.PUBLIC);
        updaterClass.setParent("java.util.concurrent.atomic.BaseAtomicReferenceFieldUpdater");
        fillClass(updaterClass, field.getOwnerName(), context.getHierarchy());
        updaterClass.addMethod(createGetRefMethod(field, className, context.getHierarchy()));
        updaterClass.addMethod(createSetRefMethod(field, className, context.getHierarchy()));
        context.submit(updaterClass);
        return className;
    }

    private MethodHolder createGetRefMethod(FieldReader field, String className, ClassHierarchy hierarchy) {
        var method = new MethodHolder("get", ValueType.object("java.lang.Object"),
                ValueType.object("java.lang.Object"));
        method.setLevel(AccessLevel.PUBLIC);

        var pe = ProgramEmitter.create(method, hierarchy);
        var instance = pe.var(1, Object.class);
        pe.invoke(className, "check", ValueType.object(field.getOwnerName()), instance)
                .getField(field.getName(), field.getType())
                .returnValue();

        return method;
    }

    private MethodHolder createSetRefMethod(FieldReader field, String className, ClassHierarchy hierarchy) {
        var method = new MethodHolder("set", ValueType.object("java.lang.Object"),
                ValueType.object("java.lang.Object"), ValueType.VOID);
        method.setLevel(AccessLevel.PUBLIC);

        var pe = ProgramEmitter.create(method, hierarchy);
        var instance = pe.var(1, Object.class);
        var value = pe.var(2, Object.class);
        pe.invoke(className, "check", ValueType.object(field.getOwnerName()), instance)
                .setField(field.getName(), value.cast(field.getType()));
        pe.exit();

        return method;
    }

    private void transformPrimitiveUpdater(Program program, InvokeInstruction invoke, String superclass,
            ValueType primitiveType, ClassHolderTransformerContext context) {
        prepare(program);

        var targetTypeConstant = classConstantsByClasses[varSet.find(invoke.getArguments().get(0).getIndex())];
        var nameConstant = stringConstantsByClasses[varSet.find(invoke.getArguments().get(1).getIndex())];

        if (targetTypeConstant == null || nameConstant == null) {
            return;
        }

        if (!(targetTypeConstant instanceof ValueType.Object)) {
            emitException(invoke, IllegalArgumentException.class);
            return;
        }

        var className = ((ValueType.Object) targetTypeConstant).getClassName();
        var cls = context.getHierarchy().getClassSource().get(className);
        if (cls == null) {
            emitException(invoke, NoClassDefFoundError.class);
            return;
        }

        var field = cls.getField(nameConstant);
        if (field == null) {
            emitException(invoke, RuntimeException.class, NoSuchFieldException.class);
            return;
        }

        if (!field.hasModifier(ElementModifier.VOLATILE) || field.hasModifier(ElementModifier.STATIC)
                || !field.getType().equals(primitiveType)) {
            emitException(invoke, IllegalArgumentException.class);
            return;
        }

        var updaterClassName = getPrimitiveUpdaterClass(context, field, superclass);
        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference(updaterClassName, "INSTANCE"));
        getField.setFieldType(ValueType.object(updaterClassName));
        getField.setLocation(invoke.getLocation());
        getField.setReceiver(invoke.getReceiver());
        invoke.replace(getField);
    }

    private String getPrimitiveUpdaterClass(ClassHolderTransformerContext context, FieldReader field,
            String superclass) {
        var key = field.getReference().toString();
        return updaterClasses.computeIfAbsent(key, k -> createPrimitiveUpdaterClass(context, field, superclass));
    }

    private String createPrimitiveUpdaterClass(ClassHolderTransformerContext context, FieldReader field,
            String superclass) {
        var className = field.getOwnerName() + "$" + field.getName() + "$_AtomicUpdater$";
        var updaterClass = new ClassHolder(className);
        updaterClass.setLevel(AccessLevel.PUBLIC);
        updaterClass.setParent(superclass);
        fillClass(updaterClass, field.getOwnerName(), context.getHierarchy());
        updaterClass.addMethod(createGetPrimitiveMethod(field, className, context.getHierarchy()));
        updaterClass.addMethod(createSetPrimitiveMethod(field, className, context.getHierarchy()));
        context.submit(updaterClass);
        return className;
    }

    private MethodHolder createGetPrimitiveMethod(FieldReader field, String className, ClassHierarchy hierarchy) {
        var method = new MethodHolder("get", ValueType.object("java.lang.Object"), field.getType());
        method.setLevel(AccessLevel.PUBLIC);

        var pe = ProgramEmitter.create(method, hierarchy);
        var instance = pe.var(1, Object.class);
        pe.invoke(className, "check", ValueType.object(field.getOwnerName()), instance)
                .getField(field.getName(), field.getType())
                .returnValue();

        return method;
    }

    private MethodHolder createSetPrimitiveMethod(FieldReader field, String className, ClassHierarchy hierarchy) {
        var method = new MethodHolder("set", ValueType.object("java.lang.Object"), field.getType(), ValueType.VOID);
        method.setLevel(AccessLevel.PUBLIC);

        var pe = ProgramEmitter.create(method, hierarchy);
        var instance = pe.var(1, Object.class);
        var value = pe.var(2, field.getType());
        pe.invoke(className, "check", ValueType.object(field.getOwnerName()), instance)
                .setField(field.getName(), value);
        pe.exit();

        return method;
    }

    private void fillClass(ClassHolder cls, String targetClassName, ClassHierarchy hierarchy) {
        var instanceField = new FieldHolder("INSTANCE");
        instanceField.setType(ValueType.object(cls.getName()));
        instanceField.setLevel(AccessLevel.PUBLIC);
        instanceField.getModifiers().add(ElementModifier.STATIC);
        cls.addField(instanceField);

        cls.addMethod(createConstructor(cls, hierarchy));
        cls.addMethod(createInitializer(cls, hierarchy));
        cls.addMethod(createCheck(targetClassName, hierarchy));
    }

    private MethodHolder createConstructor(ClassHolder cls, ClassHierarchy hierarchy) {
        var ctor = new MethodHolder("<init>", ValueType.VOID);
        ctor.setLevel(AccessLevel.PRIVATE);

        var pe = ProgramEmitter.create(ctor, hierarchy);
        pe.var(0, AtomicReferenceFieldUpdater.class).invokeSpecial(cls.getParent(), "<init>", ValueType.VOID);
        pe.exit();

        return ctor;
    }

    private MethodHolder createInitializer(ClassHolder cls, ClassHierarchy hierarchy) {
        var initializer = new MethodHolder("<clinit>", ValueType.VOID);
        initializer.setLevel(AccessLevel.PRIVATE);
        initializer.getModifiers().add(ElementModifier.STATIC);

        var pe = ProgramEmitter.create(initializer, hierarchy);
        pe.setField(cls.getName(), "INSTANCE", pe.construct(cls.getName()));
        pe.exit();

        return initializer;
    }

    private MethodHolder createCheck(String targetClassName, ClassHierarchy hierarchy) {
        var method = new MethodHolder("check", ValueType.object("java.lang.Object"),
                ValueType.object(targetClassName));
        method.setLevel(AccessLevel.PRIVATE);
        method.getModifiers().add(ElementModifier.STATIC);

        var pe = ProgramEmitter.create(method, hierarchy);
        var instance = pe.var(1, ValueType.object("java.lang.Object"));
        pe.when(instance.isNull()).thenDo(() -> {
            pe.construct(ClassCastException.class).raise();
        });
        instance.cast(ValueType.object(targetClassName)).returnValue();

        return method;
    }

    private void emitException(Instruction instruction, Class<?> exceptionType) {
        emitException(instruction, exceptionType, null);
    }

    private void emitException(Instruction instruction, Class<?> exceptionType, Class<?> wrappedExceptionType) {
        hasTruncatedBlocks = true;
        ProgramUtils.truncateBlock(instruction);

        var program = instruction.getProgram();
        var block = instruction.getBasicBlock();

        var construct = new ConstructInstruction();
        construct.setType(exceptionType.getName());
        construct.setReceiver(program.createVariable());
        construct.setLocation(instruction.getLocation());
        block.add(construct);

        var init = new InvokeInstruction();
        init.setType(InvocationType.SPECIAL);
        init.setInstance(construct.getReceiver());

        if (wrappedExceptionType != null) {
            var wrappedConstruct = new ConstructInstruction();
            wrappedConstruct.setType(wrappedExceptionType.getName());
            wrappedConstruct.setReceiver(program.createVariable());
            wrappedConstruct.setLocation(instruction.getLocation());
            block.add(wrappedConstruct);

            var wrappedInit = new InvokeInstruction();
            wrappedInit.setType(InvocationType.SPECIAL);
            wrappedInit.setInstance(wrappedConstruct.getReceiver());
            wrappedInit.setMethod(new MethodReference(wrappedExceptionType, "<init>", void.class));
            wrappedInit.setLocation(instruction.getLocation());
            block.add(wrappedInit);

            init.setMethod(new MethodReference(exceptionType, "<init>", Throwable.class, void.class));
            init.setArguments(wrappedConstruct.getReceiver());
        } else {
            init.setMethod(new MethodReference(exceptionType, "<init>", void.class));
        }
        init.setLocation(instruction.getLocation());
        block.add(init);

        var raise = new RaiseInstruction();
        raise.setException(construct.getReceiver());
        raise.setLocation(instruction.getLocation());
        block.add(raise);

        instruction.delete();
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
