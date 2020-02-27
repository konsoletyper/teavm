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
import static org.teavm.junit.TeaVMTestRunner.JUNIT4_TEST;
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
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

abstract class TestEntryPointTransformer implements ClassHolderTransformer, TeaVMPlugin {
    private String testClassName;

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
            for (MethodHolder method : cls.getMethods()) {
                switch (method.getName()) {
                    case "launchTest":
                        method.setProgram(generateLaunchProgram(method, context.getHierarchy()));
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                    case "before":
                        method.setProgram(generateBeforeProgram(method, context.getHierarchy()));
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                    case "after":
                        method.setProgram(generateAfterProgram(method, context.getHierarchy()));
                        method.getModifiers().remove(ElementModifier.NATIVE);
                        break;
                }
            }
        }
    }

    private Program generateBeforeProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        ValueEmitter testCaseInitVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);
        pe.when(testCaseInitVar.isNull())
                .thenDo(() -> {
                    pe.setField(TestEntryPoint.class, "testCase",
                            pe.construct(testClassName).cast(Object.class));
                });
        ValueEmitter testCaseVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);

        if (hierarchy.isSuperType(JUNIT3_BASE_CLASS, testClassName, false)) {
            testCaseVar.cast(ValueType.object(JUNIT3_BASE_CLASS)).invokeVirtual(JUNIT3_BEFORE);
        }

        List<ClassReader> classes = collectSuperClasses(pe.getClassSource(), testClassName);
        Collections.reverse(classes);
        classes.stream()
                .flatMap(cls -> cls.getMethods().stream())
                .filter(m -> m.getAnnotations().get(JUNIT4_BEFORE) != null)
                .forEach(m -> testCaseVar.cast(ValueType.object(m.getOwnerName())).invokeVirtual(m.getReference()));

        pe.exit();
        return pe.getProgram();
    }

    private Program generateAfterProgram(MethodHolder method, ClassHierarchy hierarchy) {
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        ValueEmitter testCaseVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);

        List<ClassReader> classes = collectSuperClasses(pe.getClassSource(), testClassName);
        classes.stream()
                .flatMap(cls -> cls.getMethods().stream())
                .filter(m -> m.getAnnotations().get(JUNIT4_AFTER) != null)
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

    protected abstract Program generateLaunchProgram(MethodHolder method, ClassHierarchy hierarchy);

    protected final void generateSingleMethodLaunchProgram(MethodReference testMethod,
            ClassHierarchy hierarchy, ProgramEmitter pe) {
        pe.getField(TestEntryPoint.class, "testCase", Object.class)
                .cast(ValueType.object(testMethod.getClassName()))
                .invokeSpecial(testMethod);

        MethodReader testMethodReader = hierarchy.getClassSource().resolve(testMethod);
        AnnotationReader testAnnotation = testMethodReader.getAnnotations().get(JUNIT4_TEST);
        AnnotationValue throwsValue = testAnnotation != null ? testAnnotation.getValue("expected") : null;
        if (throwsValue != null) {
            BasicBlock handler = pe.getProgram().createBasicBlock();
            TryCatchBlock tryCatch = new TryCatchBlock();
            tryCatch.setExceptionType(((ValueType.Object) throwsValue.getJavaClass()).getClassName());
            tryCatch.setHandler(handler);
            pe.getBlock().getTryCatchBlocks().add(tryCatch);

            BasicBlock nextBlock = pe.getProgram().createBasicBlock();
            pe.jump(nextBlock);
            pe.enter(nextBlock);
            pe.construct(AssertionError.class, pe.constant("Expected exception not thrown")).raise();

            pe.enter(handler);
            pe.exit();
        } else {
            pe.exit();
        }
    }
}
