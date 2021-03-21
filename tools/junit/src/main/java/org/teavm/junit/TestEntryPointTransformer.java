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
package org.teavm.junit;

import static org.teavm.junit.TeaVMTestRunner.JUNIT3_AFTER;
import static org.teavm.junit.TeaVMTestRunner.JUNIT3_BASE_CLASS;
import static org.teavm.junit.TeaVMTestRunner.JUNIT3_BEFORE;
import static org.teavm.junit.TeaVMTestRunner.JUNIT4_AFTER;
import static org.teavm.junit.TeaVMTestRunner.JUNIT4_BEFORE;
import static org.teavm.junit.TeaVMTestRunner.TESTNG_AFTER;
import static org.teavm.junit.TeaVMTestRunner.TESTNG_BEFORE;
import static org.teavm.junit.TeaVMTestRunner.TESTNG_PROVIDER;
import static org.teavm.junit.TeaVMTestRunner.TESTNG_TEST;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.emit.PhiEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

abstract class TestEntryPointTransformer implements ClassHolderTransformer, TeaVMPlugin {
    private String testClassName;
    private int suffixGenerator;

    TestEntryPointTransformer(String testClassName) {
        this.testClassName = testClassName;
    }

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(TestEntryPoint.class.getName())) {
            suffixGenerator = 0;
            for (MethodHolder method : cls.getMethods()) {
                switch (method.getName()) {
                    case "createTestCase":
                        generateCreateTestCaseProgram(method, context.getHierarchy());
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                    case "launchers":
                        generateLaunchProgram(method, context);
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                    case "before":
                        generateBeforeProgram(method, context.getHierarchy());
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                    case "after":
                        generateAfterProgram(method, context.getHierarchy());
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                }
            }
        }
    }

    private void generateCreateTestCaseProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        pe.construct(testClassName).cast(Object.class).returnValue();
    }

    private void generateBeforeProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        ValueEmitter testCaseVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);

        if (hierarchy.isSuperType(JUNIT3_BASE_CLASS, testClassName, false)) {
            testCaseVar.cast(ValueType.object(JUNIT3_BASE_CLASS)).invokeVirtual(JUNIT3_BEFORE);
        }

        List<ClassReader> classes = collectSuperClasses(pe.getClassSource(), testClassName);
        Collections.reverse(classes);
        classes.stream()
                .flatMap(cls -> cls.getMethods().stream())
                .filter(m -> m.getAnnotations().get(JUNIT4_BEFORE) != null
                        || m.getAnnotations().get(TESTNG_BEFORE) != null)
                .forEach(m -> testCaseVar.cast(ValueType.object(m.getOwnerName())).invokeVirtual(m.getReference()));

        pe.exit();
    }

    private Program generateAfterProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        ValueEmitter testCaseVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);

        List<ClassReader> classes = collectSuperClasses(pe.getClassSource(), testClassName);
        classes.stream()
                .flatMap(cls -> cls.getMethods().stream())
                .filter(m -> m.getAnnotations().get(JUNIT4_AFTER) != null
                        || m.getAnnotations().get(TESTNG_AFTER) != null)
                .forEach(m -> testCaseVar.cast(ValueType.object(m.getOwnerName())).invokeVirtual(m.getReference()));

        if (hierarchy.isSuperType(JUNIT3_BASE_CLASS, testClassName, false)) {
            testCaseVar.cast(ValueType.object(JUNIT3_BASE_CLASS)).invokeVirtual(JUNIT3_AFTER);
        }

        pe.exit();
        return pe.getProgram();
    }

    private List<ClassReader> collectSuperClasses(ClassReaderSource classSource, String className) {
        List<ClassReader> result = new ArrayList<>();
        while (className != null && !className.equals(JUNIT3_BASE_CLASS)) {
            ClassReader cls = classSource.get(className);
            if (cls == null) {
                break;
            }
            result.add(cls);
            className = cls.getParent();
        }
        return result;
    }

    protected abstract void generateLaunchProgram(MethodHolder method, ClassHolderTransformerContext context);

    protected final void generateSingleMethodLaunchProgram(MethodReference testMethod,
            ClassHolderTransformerContext context, ProgramEmitter pe) {
        ClassHolder launcherClass = generateLauncherClass(testMethod, context.getHierarchy());
        context.submit(launcherClass);
        ValueEmitter list = pe.var(2, List.class);

        MethodReader testMethodReader = context.getHierarchy().getClassSource().resolve(testMethod);
        AnnotationReader testNgAnnot = testMethodReader.getAnnotations().get(TESTNG_TEST);
        if (testNgAnnot != null) {
            AnnotationValue dataProviderValue = testNgAnnot.getValue("dataProvider");
            if (dataProviderValue != null) {
                generateAddLaunchersWithProvider(testMethodReader, context.getHierarchy(), pe, list,
                        dataProviderValue.getString(), launcherClass.getName());
                return;
            }
        }

        list.invokeVirtual("add", boolean.class, pe.construct(launcherClass.getName()).cast(Object.class));
        pe.exit();
    }

    private void generateAddLaunchersWithProvider(MethodReader testMethodReader, ClassHierarchy hierarchy,
            ProgramEmitter pe, ValueEmitter list, String providerName, String launcherClassName) {
        ClassReader owningClass = hierarchy.getClassSource().get(testMethodReader.getOwnerName());
        MethodReader providerMethod = null;
        for (MethodReader method : owningClass.getMethods()) {
            AnnotationReader annot = method.getAnnotations().get(TESTNG_PROVIDER);
            if (annot != null && annot.getValue("name").getString().equals(providerName)) {
                providerMethod = method;
                break;
            }
        }

        ValueEmitter data = pe.getField(TestEntryPoint.class, "testCase", Object.class)
                .cast(ValueType.object(testMethodReader.getOwnerName()))
                .invokeSpecial(providerMethod.getReference());
        if (data.getType() instanceof ValueType.Array) {
            generateAddLaunchersWithProviderArray(testMethodReader, pe, list, data, launcherClassName);
        } else {
            generateAddLaunchersWithProviderIterator(testMethodReader, pe, list, data, launcherClassName);
        }
    }

    private void generateAddLaunchersWithProviderArray(MethodReader testMethodReader, ProgramEmitter pe,
            ValueEmitter list, ValueEmitter data, String launcherClassName) {
        ValueEmitter size = data.arrayLength();
        BasicBlock loopHead = pe.getProgram().createBasicBlock();
        BasicBlock loopBody = pe.getProgram().createBasicBlock();
        BasicBlock loopExit = pe.getProgram().createBasicBlock();
        PhiEmitter index = pe.phi(int.class, loopHead);
        pe.constant(0).propagateTo(index);
        pe.jump(loopHead);

        pe.enter(loopHead);
        pe.when(index.getValue().isLessThan(size))
                .thenDo(() -> pe.jump(loopBody))
                .elseDo(() -> pe.jump(loopExit));

        pe.enter(loopBody);
        ValueEmitter dataRow = data.getElement(index.getValue());
        generateAddLauncherWithData(testMethodReader, pe, list, dataRow, launcherClassName);
        index.getValue().add(1).propagateTo(index);
        pe.jump(loopHead);

        pe.enter(loopExit);
        pe.exit();
    }

    private void generateAddLaunchersWithProviderIterator(MethodReader testMethodReader, ProgramEmitter pe,
            ValueEmitter list, ValueEmitter data, String launcherClassName) {
        BasicBlock loopHead = pe.getProgram().createBasicBlock();
        BasicBlock loopBody = pe.getProgram().createBasicBlock();
        BasicBlock loopExit = pe.getProgram().createBasicBlock();
        pe.jump(loopHead);

        pe.enter(loopHead);
        pe.when(data.invokeVirtual("hasNext", boolean.class).isTrue())
                .thenDo(() -> pe.jump(loopBody))
                .elseDo(() -> pe.jump(loopExit));

        pe.enter(loopBody);
        ValueEmitter dataRow = data.invokeVirtual("next", Object.class).cast(Object[].class);
        generateAddLauncherWithData(testMethodReader, pe, list, dataRow, launcherClassName);
        pe.jump(loopHead);

        pe.enter(loopExit);
        pe.exit();
    }

    private void generateAddLauncherWithData(MethodReader testMethodReader, ProgramEmitter pe, ValueEmitter list,
            ValueEmitter dataRow, String launcherClassName) {
        List<ValueEmitter> arguments = new ArrayList<>();
        for (int i = 0; i < testMethodReader.parameterCount(); ++i) {
            ValueType type = testMethodReader.parameterType(i);
            arguments.add(convertArgument(dataRow.getElement(i), type));
        }

        list.invokeVirtual("add", boolean.class, pe.construct(launcherClassName,
                arguments.toArray(new ValueEmitter[0])).cast(Object.class));
    }

    private ValueEmitter convertArgument(ValueEmitter value, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return value.cast(Boolean.class).invokeVirtual("booleanValue", boolean.class);
                case CHARACTER:
                    return value.cast(Character.class).invokeVirtual("charValue", char.class);
                case BYTE:
                    return value.cast(Number.class).invokeVirtual("byteValue", byte.class);
                case SHORT:
                    return value.cast(Number.class).invokeVirtual("shortValue", byte.class);
                case INTEGER:
                    return value.cast(Number.class).invokeVirtual("intValue", int.class);
                case LONG:
                    return value.cast(Number.class).invokeVirtual("longValue", long.class);
                case FLOAT:
                    return value.cast(Number.class).invokeVirtual("floatValue", float.class);
                case DOUBLE:
                    return value.cast(Number.class).invokeVirtual("doubleValue", double.class);
            }
        }
        return value.cast(type);
    }

    private ClassHolder generateLauncherClass(MethodReference testMethod, ClassHierarchy hierarchy) {
        ClassHolder cls = new ClassHolder(TestEntryPoint.Launcher.class.getName() + "Impl" + suffixGenerator++);
        cls.setParent("java.lang.Object");
        cls.getInterfaces().add(TestEntryPoint.Launcher.class.getName());

        MethodHolder constructor = new MethodHolder("<init>", testMethod.getSignature());
        cls.addMethod(constructor);
        ProgramEmitter pe = ProgramEmitter.create(constructor, hierarchy);
        pe.var(0, ValueType.object(cls.getName())).invokeSpecial(Object.class, "<init>");
        ValueEmitter self = pe.var(0, ValueType.object(cls.getName()));
        for (int i = 0; i < testMethod.parameterCount(); ++i) {
            FieldHolder paramField = new FieldHolder("param_" + i);
            paramField.setType(testMethod.parameterType(i));
            cls.addField(paramField);
            self.setField(paramField.getName(), pe.var(i + 1, testMethod.parameterType(i)));
        }
        pe.exit();

        MethodHolder launchMethod = new MethodHolder("launch", ValueType.parse(Object.class), ValueType.VOID);
        cls.addMethod(launchMethod);
        pe = ProgramEmitter.create(launchMethod, hierarchy);
        List<ValueEmitter> arguments = new ArrayList<>();
        self = pe.var(0, ValueType.object(cls.getName()));
        for (int i = 0; i < testMethod.parameterCount(); ++i) {
            arguments.add(self.getField("param_" + i, testMethod.parameterType(i)));
        }
        generateRunMethodOnce(testMethod, hierarchy, pe, pe.var(1, Object.class), arguments);
        pe.exit();

        return cls;
    }

    private void generateRunMethodOnce(MethodReference testMethod, ClassHierarchy hierarchy, ProgramEmitter pe,
            ValueEmitter testCase, List<ValueEmitter> arguments) {
        testCase.cast(ValueType.object(testMethod.getClassName()))
                .invokeSpecial(testMethod, arguments.toArray(new ValueEmitter[0]));

        MethodReader testMethodReader = hierarchy.getClassSource().resolve(testMethod);
        String[] expectedExceptions = TeaVMTestRunner.getExpectedExceptions(testMethodReader);
        if (expectedExceptions.length != 0) {
            BasicBlock handler = pe.getProgram().createBasicBlock();

            for (String exceptionType : expectedExceptions) {
                TryCatchBlock tryCatch = new TryCatchBlock();
                tryCatch.setExceptionType(exceptionType);
                tryCatch.setHandler(handler);
                pe.getBlock().getTryCatchBlocks().add(tryCatch);
            }

            BasicBlock nextBlock = pe.getProgram().createBasicBlock();
            pe.jump(nextBlock);
            pe.enter(nextBlock);
            pe.construct(AssertionError.class, pe.constant("Expected exception not thrown")).raise();

            pe.enter(handler);
        }
    }
}
