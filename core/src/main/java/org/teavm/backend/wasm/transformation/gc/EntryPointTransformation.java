/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.transformation.gc;

import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.runtime.Fiber;
import org.teavm.vm.TeaVM;

public class EntryPointTransformation implements ClassHolderTransformer {
    private static final MethodDescriptor MAIN_METHOD = new MethodDescriptor("main", String[].class, void.class);
    private String entryPoint;
    private String entryPointName;

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void setEntryPointName(String entryPointName) {
        this.entryPointName = entryPointName;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(entryPoint)) {
            var mainMethod = getMainMethod(cls);
            if (mainMethod != null) {
                generateMainMethodCaller(mainMethod, cls, context);
            }
        } else if (cls.getName().equals(Fiber.class.getName())) {
            var setCurrentThread = cls.getMethod(new MethodDescriptor("setCurrentThread", Thread.class, void.class));
            setCurrentThread.getModifiers().remove(ElementModifier.NATIVE);
            var pe = ProgramEmitter.create(setCurrentThread, context.getHierarchy());
            pe.invoke(Thread.class, "setCurrentThread", pe.var(1, Thread.class));
            pe.exit();

            var mainMethod = getMainMethod(context.getHierarchy().getClassSource().get(entryPoint));
            if (mainMethod == null) {
                return;
            }

            var runMain = cls.getMethod(new MethodDescriptor("runMain", String[].class, void.class));
            runMain.getModifiers().remove(ElementModifier.NATIVE);
            pe = ProgramEmitter.create(runMain, context.getHierarchy());
            var args = mainMethod.parameterCount() == 1
                    ? new ValueEmitter[] { pe.var(1, String[].class) }
                    : new ValueEmitter[0];
            if (mainMethod.hasModifier(ElementModifier.STATIC)) {
                pe.invoke(new MethodReference(entryPoint, MAIN_METHOD), args);
            } else {
                pe.construct(entryPoint).invokeSpecial("main", args);
            }
            pe.exit();
        }
    }

    private void generateMainMethodCaller(MethodReader mainMethod, ClassHolder cls,
            ClassHolderTransformerContext context) {
        var mainMethodCaller = new MethodHolder(mainMethod.getName() + "_$caller", mainMethod.getSignature());
        mainMethodCaller.setLevel(AccessLevel.PUBLIC);
        mainMethodCaller.getModifiers().add(ElementModifier.STATIC);

        cls.addMethod(mainMethodCaller);
        mainMethodCaller.getAnnotations().add(new AnnotationHolder("org.teavm.jso.JSExport"));

        var methodAnnot = new AnnotationHolder("org.teavm.jso.JSMethod");
        methodAnnot.getValues().put("value", new AnnotationValue(entryPointName));
        mainMethodCaller.getAnnotations().add(methodAnnot);

        var pe = ProgramEmitter.create(mainMethodCaller, context.getHierarchy());
        var arg = mainMethod.parameterCount() == 1
                ? pe.var(1, String[].class)
                : pe.constantNull(String[].class);
        pe.invoke(Fiber.class, "startMain", arg);
        pe.exit();
    }

    private MethodReader getMainMethod(ClassReader cls) {
        var mainMethod = cls.getMethod(TeaVM.MAIN_METHOD_DESC);
        if (mainMethod != null) {
            return mainMethod;
        }
        return cls.getMethod(TeaVM.SHORT_MAIN_METHOD_DESC);
    }
}
