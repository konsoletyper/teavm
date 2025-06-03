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

import static org.teavm.jso.impl.JSMethods.JS_MARSHALLABLE;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import static org.teavm.jso.impl.JSMethods.WASM_GC_JS_RUNTIME_CLASS;
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
        if (cls.getName().equals(JS_WRAPPER_CLASS)) {
            transformMarshallMethod(cls.getMethod(new MethodDescriptor("marshallJavaToJs", OBJECT,
                    JS_OBJECT)), context);
            transformDirectJavaToJs(cls.getMethod(new MethodDescriptor("directJavaToJs", OBJECT, JS_OBJECT)), context);
            transformDirectJavaToJs(cls.getMethod(new MethodDescriptor("dependencyJavaToJs", OBJECT,
                    JS_OBJECT)), context);
            transformDirectJsToJava(cls.getMethod(new MethodDescriptor("dependencyJsToJava", JS_OBJECT,
                    OBJECT)), context);
            transformWrapMethod(cls.getMethod(new MethodDescriptor("wrap", JS_OBJECT, OBJECT)));
            transformIsJava(cls.getMethod(new MethodDescriptor("isJava", OBJECT, ValueType.BOOLEAN)), context);
            addCreateWrapperMethod(cls, context);
        }
    }

    private void transformMarshallMethod(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, Object.class);
        pe.when(obj.instanceOf(ValueType.object(JS_MARSHALLABLE)).isFalse()).thenDo(() -> {
            pe.invoke(WASM_GC_JS_RUNTIME_CLASS, "wrapObject", JS_OBJECT, obj).returnValue();
        });
        obj.cast(ValueType.object(JS_MARSHALLABLE)).invokeVirtual("marshallToJs", JS_OBJECT).returnValue();
    }

    private void transformDirectJavaToJs(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, OBJECT);
        pe.invoke(JS_WRAPPER_CLASS, "marshallJavaToJs", JS_OBJECT, obj).returnValue();
    }

    private void transformDirectJsToJava(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var obj = pe.var(1, JS_OBJECT);
        pe.invoke(JS_WRAPPER_CLASS, "unmarshallJavaFromJs", OBJECT, obj).returnValue();
    }

    private void transformWrapMethod(MethodHolder method) {
        method.getModifiers().add(ElementModifier.NATIVE);
        method.setProgram(null);
    }

    private void addCreateWrapperMethod(ClassHolder cls, ClassHolderTransformerContext context) {
        var method = new MethodHolder(new MethodDescriptor("createWrapper", JS_OBJECT, OBJECT));
        method.getModifiers().add(ElementModifier.STATIC);
        method.setLevel(AccessLevel.PUBLIC);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        pe.construct(JS_WRAPPER_CLASS, pe.var(1, JS_OBJECT)).returnValue();
        cls.addMethod(method);
    }

    private void transformIsJava(MethodHolder method, ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        pe.constant(1).returnValue();
    }
}
