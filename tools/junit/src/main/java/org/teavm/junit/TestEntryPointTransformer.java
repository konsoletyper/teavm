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

import org.junit.Test;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
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

class TestEntryPointTransformer implements ClassHolderTransformer, TeaVMPlugin {
    private String runnerClassName;
    private MethodReference testMethod;

    TestEntryPointTransformer(String runnerClassName, MethodReference testMethod) {
        this.runnerClassName = runnerClassName;
        this.testMethod = testMethod;
    }

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getName().equals(TestEntryPoint.class.getName())) {
            for (MethodHolder method : cls.getMethods()) {
                if (method.getName().equals("createRunner")) {
                    method.setProgram(generateRunnerProgram(method, innerSource));
                    method.getModifiers().remove(ElementModifier.NATIVE);
                } else if (method.getName().equals("launchTest")) {
                    method.setProgram(generateLaunchProgram(method, innerSource));
                    method.getModifiers().remove(ElementModifier.NATIVE);
                }
            }
        }
    }

    private Program generateRunnerProgram(MethodHolder method, ClassReaderSource innerSource) {
        ProgramEmitter pe = ProgramEmitter.create(method, innerSource);
        pe.construct(runnerClassName).returnValue();
        return pe.getProgram();
    }

    private Program generateLaunchProgram(MethodHolder method, ClassReaderSource innerSource) {
        ProgramEmitter pe = ProgramEmitter.create(method, innerSource);
        ValueEmitter testCaseVar = pe.getField(TestEntryPoint.class, "testCase", Object.class);
        pe.when(testCaseVar.isNull())
            .thenDo(() -> {
                pe.setField(TestEntryPoint.class, "testCase",
                        pe.construct(testMethod.getClassName()).cast(Object.class));
            });
        pe.getField(TestEntryPoint.class, "testCase", Object.class)
                .cast(ValueType.object(testMethod.getClassName()))
                .invokeSpecial(testMethod);

        MethodReader testMethodReader = innerSource.resolve(testMethod);
        AnnotationReader testAnnotation = testMethodReader.getAnnotations().get(Test.class.getName());
        AnnotationValue throwsValue = testAnnotation.getValue("expected");
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
        return pe.getProgram();
    }
}
