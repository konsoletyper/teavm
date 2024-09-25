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

import java.lang.ref.ReferenceQueue;
import org.teavm.interop.Import;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class BaseClassesTransformation implements ClassHolderTransformer {
    private static final MethodReference GET_MONITOR = new MethodReference(Object.class.getName(),
            "getMonitor", ValueType.object("java.lang.Object$Monitor"));
    private static final MethodReference SET_MONITOR = new MethodReference(Object.class.getName(),
            "setMonitor", ValueType.object("java.lang.Object$Monitor"), ValueType.VOID);
    private static final FieldReference MONITOR = new FieldReference(Object.class.getName(), "monitor");

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.Object")) {
            for (var method : cls.getMethods()) {
                switch (method.getName()) {
                    case "getClass":
                        method.setProgram(null);
                        method.getModifiers().add(ElementModifier.NATIVE);
                        break;
                    default:
                        if (method.getProgram() != null) {
                            transformMonitorFieldAccess(method.getProgram());
                        }
                        break;
                }
            }
            var getMonitorMethod = new MethodHolder(GET_MONITOR.getDescriptor());
            getMonitorMethod.setLevel(AccessLevel.PRIVATE);
            getMonitorMethod.getModifiers().add(ElementModifier.NATIVE);
            cls.addMethod(getMonitorMethod);

            var setMonitorMethod = new MethodHolder(SET_MONITOR.getDescriptor());
            setMonitorMethod.setLevel(AccessLevel.PRIVATE);
            setMonitorMethod.getModifiers().add(ElementModifier.NATIVE);
            cls.addMethod(setMonitorMethod);
        } else if (cls.getName().equals("java.lang.Class")) {
            for (var method : cls.getMethods()) {
                switch (method.getName()) {
                    case "getComponentType":
                    case "isAssignableFrom":
                    case "getEnclosingClass":
                    case "getDeclaringClass":
                    case "getSimpleNameCache":
                    case "setSimpleNameCache":
                    case "getSuperclass":
                        method.setProgram(null);
                        method.getModifiers().add(ElementModifier.NATIVE);
                        break;
                }
            }
        } else if (cls.getName().equals("java.lang.System")) {
            Program arrayCopyProgram = null;
            MethodDescriptor arrayCopyDescriptor = null;
            for (var method : cls.getMethods()) {
                switch (method.getName()) {
                    case "arraycopy":
                        arrayCopyProgram = method.getProgram();
                        method.setProgram(null);
                        method.getModifiers().add(ElementModifier.NATIVE);
                        arrayCopyDescriptor = method.getDescriptor();
                        break;
                    case "currentTimeMillis": {
                        var annotation = new AnnotationHolder(Import.class.getName());
                        annotation.getValues().put("module", new AnnotationValue("teavm"));
                        annotation.getValues().put("name", new AnnotationValue("currentTimeMillis"));
                        method.getAnnotations().add(annotation);
                    }
                }
            }
            if (arrayCopyProgram != null) {
                var arrayCopyImpl = new MethodHolder(new MethodDescriptor("arrayCopyImpl",
                        arrayCopyDescriptor.getSignature()));
                arrayCopyImpl.setProgram(arrayCopyProgram);
                arrayCopyImpl.getModifiers().add(ElementModifier.STATIC);
                cls.addMethod(arrayCopyImpl);
            }
        } else if (cls.getName().equals("java.lang.ref.WeakReference")) {
            var constructor = cls.getMethod(new MethodDescriptor("<init>", Object.class, ReferenceQueue.class,
                    void.class));
            constructor.getModifiers().add(ElementModifier.NATIVE);
            constructor.setProgram(null);

            var get = cls.getMethod(new MethodDescriptor("get", Object.class));
            get.getModifiers().add(ElementModifier.NATIVE);
            get.setProgram(null);

            var clear = cls.getMethod(new MethodDescriptor("clear", void.class));
            clear.getModifiers().add(ElementModifier.NATIVE);
            clear.setProgram(null);
        }
    }

    private void transformMonitorFieldAccess(Program program) {
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof GetFieldInstruction) {
                    var getField = (GetFieldInstruction) instruction;
                    if (getField.getField().equals(MONITOR)) {
                        var invocation = new InvokeInstruction();
                        invocation.setType(InvocationType.SPECIAL);
                        invocation.setInstance(getField.getInstance());
                        invocation.setMethod(GET_MONITOR);
                        invocation.setReceiver(getField.getReceiver());
                        invocation.setLocation(getField.getLocation());
                        getField.replace(invocation);
                    }
                } else if (instruction instanceof PutFieldInstruction) {
                    var putField = (PutFieldInstruction) instruction;
                    if (putField.getField().equals(MONITOR)) {
                        var invocation = new InvokeInstruction();
                        invocation.setType(InvocationType.SPECIAL);
                        invocation.setInstance(putField.getInstance());
                        invocation.setMethod(SET_MONITOR);
                        invocation.setArguments(putField.getValue());
                        invocation.setLocation(putField.getLocation());
                        putField.replace(invocation);
                    }
                }
            }
        }
    }
}
