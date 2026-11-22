/*
 *  Copyright 2026 TeaVM contributors.
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
package org.teavm.backend.c.transform;

import org.teavm.model.AccessLevel;
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

public class MainMethodTransformation implements ClassHolderTransformer {
    private static final MethodDescriptor RUN_MAIN = new MethodDescriptor("runMain", String[].class, void.class);
    private static final MethodDescriptor MAIN_CALLER = new MethodDescriptor("main_$caller", String[].class,
            void.class);
    private String entryPoint;

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (entryPoint == null) {
            return;
        }
        if (cls.getName().equals(entryPoint)) {
            var mainMethod = getMainMethod(cls);
            if (mainMethod != null) {
                generateMainMethodCaller(mainMethod, cls, context);
            }
        } else if (cls.getName().equals(Fiber.class.getName())) {
            if (getMainMethod(context.getHierarchy().getClassSource().get(entryPoint)) == null) {
                return;
            }
            // Fiber methods are excluded from coroutine transformation, so the actual call to main
            // (and to the constructor, in case of instance main) lives in the entry point class
            var runMain = cls.getMethod(RUN_MAIN);
            runMain.getModifiers().remove(ElementModifier.NATIVE);
            var pe = ProgramEmitter.create(runMain, context.getHierarchy());
            pe.invoke(new MethodReference(entryPoint, MAIN_CALLER), pe.var(1, String[].class));
            pe.exit();
        }
    }

    private void generateMainMethodCaller(MethodReader mainMethod, ClassHolder cls,
            ClassHolderTransformerContext context) {
        var caller = new MethodHolder(MAIN_CALLER);
        caller.setLevel(AccessLevel.PUBLIC);
        caller.getModifiers().add(ElementModifier.STATIC);
        cls.addMethod(caller);

        var pe = ProgramEmitter.create(caller, context.getHierarchy());
        var args = mainMethod.parameterCount() == 1
                ? new ValueEmitter[] { pe.var(1, String[].class) }
                : new ValueEmitter[0];
        if (mainMethod.hasModifier(ElementModifier.STATIC)) {
            pe.invoke(mainMethod.getReference(), args);
        } else {
            pe.construct(entryPoint).invokeSpecial("main", args);
        }
        pe.exit();
    }

    private MethodReader getMainMethod(ClassReader cls) {
        if (cls == null) {
            return null;
        }
        var mainMethod = cls.getMethod(TeaVM.MAIN_METHOD_DESC);
        if (mainMethod != null) {
            return mainMethod;
        }
        return cls.getMethod(TeaVM.SHORT_MAIN_METHOD_DESC);
    }
}
