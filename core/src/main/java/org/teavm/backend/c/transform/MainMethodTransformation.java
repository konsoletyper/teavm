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
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.runtime.Fiber;
import org.teavm.vm.TeaVM;

public class MainMethodTransformation implements ClassHolderTransformer {
    private String entryPoint;

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals(Fiber.class.getName()) || entryPoint == null) {
            return;
        }
        var mainClass = context.getHierarchy().getClassSource().get(entryPoint);
        if (mainClass == null) {
            return;
        }
        var mainMethod = mainClass.getMethod(TeaVM.MAIN_METHOD_DESC);
        if (mainMethod == null) {
            mainMethod = mainClass.getMethod(TeaVM.SHORT_MAIN_METHOD_DESC);
        }
        if (mainMethod == null) {
            return;
        }
        // Fiber itself is excluded from coroutine transformation. Keep allocation,
        // constructor and main calls in a normal managed method so each can suspend.
        var callerName = Fiber.class.getName() + "$MainCaller";
        while (context.getHierarchy().getClassSource().get(callerName) != null) {
            callerName += "_";
        }
        var callerClass = new ClassHolder(callerName);
        callerClass.setParent(Object.class.getName());
        var caller = new MethodHolder(new MethodDescriptor("run", String[].class, void.class));
        caller.setLevel(AccessLevel.PUBLIC);
        caller.getModifiers().add(ElementModifier.STATIC);
        callerClass.addMethod(caller);
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
        context.submit(callerClass);

        var runMain = cls.getMethod(new MethodDescriptor("runMain", String[].class, void.class));
        runMain.getModifiers().remove(ElementModifier.NATIVE);
        pe = ProgramEmitter.create(runMain, context.getHierarchy());
        pe.invoke(caller.getReference(), pe.var(1, String[].class));
        pe.exit();
    }
}
