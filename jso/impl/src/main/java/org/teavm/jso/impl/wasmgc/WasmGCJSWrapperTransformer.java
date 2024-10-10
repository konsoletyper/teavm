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
package org.teavm.jso.impl.wasmgc;

import org.teavm.jso.JSObject;
import org.teavm.jso.impl.JSMarshallable;
import org.teavm.jso.impl.JSWrapper;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;

class WasmGCJSWrapperTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(JSWrapper.class.getName())) {
            transformMarshallMethod(cls.getMethod(new MethodDescriptor("marshallJavaToJs", Object.class,
                    JSObject.class)), context);
            transformDirectJavaToJs(cls.getMethod(new MethodDescriptor("directJavaToJs", Object.class,
                    JSObject.class)), context);
            transformDirectJavaToJs(cls.getMethod(new MethodDescriptor("dependencyJavaToJs", Object.class,
                    JSObject.class)), context);
            transformDirectJsToJava(cls.getMethod(new MethodDescriptor("dependencyJsToJava", JSObject.class,
                    Object.class)), context);
            transformWrapMethod(cls.getMethod(new MethodDescriptor("wrap", JSObject.class, Object.class)));
            transformIsJava(cls.getMethod(new MethodDescriptor("isJava", Object.class, boolean.class)), context);
            addCreateWrapperMethod(cls, context);
        }
    }

    private void transformMarshallMethod(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, Object.class);
        pe.when(obj.instanceOf(ValueType.parse(JSMarshallable.class)).isFalse()).thenDo(() -> {
            pe.invoke(WasmGCJSRuntime.class, "wrapObject", JSObject.class, obj).returnValue();
        });
        obj.cast(JSMarshallable.class).invokeVirtual("marshallToJs", JSObject.class).returnValue();
    }

    private void transformDirectJavaToJs(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, Object.class);
        pe.invoke(JSWrapper.class, "marshallJavaToJs", JSObject.class, obj).returnValue();
    }

    private void transformDirectJsToJava(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, JSObject.class);
        pe.invoke(JSWrapper.class, "unmarshallJavaFromJs", Object.class, obj).returnValue();
    }

    private void transformWrapMethod(MethodHolder method) {
        method.getModifiers().add(ElementModifier.NATIVE);
        method.setProgram(null);
    }

    private void addCreateWrapperMethod(ClassHolder cls, ClassHolderTransformerContext context) {
        var method = new MethodHolder(new MethodDescriptor("createWrapper", JSObject.class, Object.class));
        method.getModifiers().add(ElementModifier.STATIC);
        method.setLevel(AccessLevel.PUBLIC);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        pe.construct(JSWrapper.class, pe.var(1, JSObject.class)).returnValue();
        cls.addMethod(method);
    }

    private void transformIsJava(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        pe.constant(1).returnValue();
    }
}
