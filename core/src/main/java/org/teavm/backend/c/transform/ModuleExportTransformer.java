/*
 *  Copyright 2025 Alexey Andreev.
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

import org.teavm.backend.c.CModuleExport;
import org.teavm.interop.Export;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.Fiber;

public class ModuleExportTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(context.getEntryPoint())) {
            for (var method : cls.getMethods()) {
                var exportAnnot = method.getAnnotations().get(Export.class.getName());
                if (exportAnnot != null) {
                    method.getAnnotations().remove(Export.class.getName());
                    addExportRunner(cls, exportAnnot.getValue("name").getString(), method, context);
                }
            }
        }
    }

    private void addExportRunner(ClassHolder cls, String externalName, MethodHolder method,
            ClassHolderTransformerContext context) {
        var location = new CallLocation(method.getReference());
        var valid = true;
        if (!method.hasModifier(ElementModifier.STATIC)) {
            context.getDiagnostics().error(location, "Can only export static methods");
            valid = false;
        }
        if (!valid) {
            return;
        }

        var callerClass = new ClassHolder(cls.getName() + "_" + method.getName() + "_Caller");
        callerClass.getInterfaces().add(Fiber.FiberRunner.class.getName());
        var ctor = new MethodHolder("<init>", ValueType.VOID);
        callerClass.addMethod(ctor);
        var pe = ProgramEmitter.create(ctor, context.getHierarchy());
        pe.construct(cls.getName());
        var thisVar = pe.var(0, ValueType.object(callerClass.getName()));
        thisVar.cast(Object.class).invokeSpecial("<init>");
        pe.exit();

        if (method.getResultType() != ValueType.VOID) {
            var resultField = new FieldHolder("result");
            resultField.setType(method.getResultType());
            callerClass.addField(resultField);
        }
        for (var i = 0; i < method.parameterCount(); ++i) {
            var paramField = new FieldHolder("param" + i);
            paramField.setType(method.parameterType(i));
            callerClass.addField(paramField);
        }

        var runMethod = new MethodHolder("run", ValueType.VOID);
        runMethod.setLevel(AccessLevel.PUBLIC);
        pe = ProgramEmitter.create(runMethod, context.getHierarchy());
        thisVar = pe.var(0, ValueType.object(callerClass.getName()));
        var args = new ValueEmitter[method.parameterCount()];
        for (var i = 0; i < method.parameterCount(); ++i) {
            args[i] = thisVar.getField("param" + i, method.parameterType(i));
        }
        var executionResult = pe.invoke(method.getReference(), args);
        if (method.getResultType() != ValueType.VOID) {
            thisVar.setField("result", executionResult);
        }
        pe.exit();

        var caller = new MethodHolder(method.getName() + "$export", method.getSignature());
        caller.setLevel(AccessLevel.PUBLIC);
        caller.getModifiers().add(ElementModifier.NATIVE);
        cls.addMethod(caller);
        var annot = new AnnotationHolder(CModuleExport.class.getName());
        annot.getValues().put("name", new AnnotationValue(externalName));
        caller.getAnnotations().add(annot);

        pe = ProgramEmitter.create(caller, context.getHierarchy());

        var runnableVar = pe.construct(callerClass.getName());
        for (var i = 0; i < method.parameterCount(); ++i) {
            runnableVar.setField("param" + i, pe.var(i + 1, method.parameterType(i)));
        }
        pe.invoke(Fiber.class, "start", runnableVar, pe.constant(0).cast(boolean.class));
        pe.invoke(EventQueue.class, "process");
        if (method.getResultType() != ValueType.VOID) {
            thisVar.getField("result", method.getResultType());
        } else {
            pe.exit();
        }
    }
}
