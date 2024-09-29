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

import java.util.ArrayList;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.jso.impl.AliasCollector;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCMarshallMethodGenerator implements WasmGCCustomGenerator {
    private WasmGCJsoCommonGenerator commonGen;
    private WasmFunction javaObjectToJSFunction;
    private WasmFunction createClassFunction;
    private WasmFunction defineMethodFunction;
    private WasmFunction definePropertyFunction;

    WasmGCMarshallMethodGenerator(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var jsoContext = WasmGCJsoContext.wrap(context);

        var thisLocal = new WasmLocal(context.typeMapper().mapType(ValueType.object(method.getClassName())), "this");
        function.add(thisLocal);

        var cls = context.classes().get(method.getClassName());
        var jsClassGlobal = defineClass(jsoContext, cls);
        var wrapperFunction = javaObjectToJSFunction(context);
        function.getBody().add(new WasmCall(wrapperFunction, new WasmGetLocal(thisLocal),
                new WasmGetGlobal(jsClassGlobal)));
    }

    private WasmFunction javaObjectToJSFunction(WasmGCCustomGeneratorContext context) {
        if (javaObjectToJSFunction == null) {
            javaObjectToJSFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    context.typeMapper().mapType(ValueType.parse(Object.class)), WasmType.Reference.EXTERN));
            javaObjectToJSFunction.setName(context.names().topLevel("teavm.jso@javaObjectToJS"));
            javaObjectToJSFunction.setImportName("javaObjectToJS");
            javaObjectToJSFunction.setImportModule("teavmJso");
            context.module().functions.add(javaObjectToJSFunction);
        }
        return javaObjectToJSFunction;
    }

    WasmGlobal defineClass(WasmGCJsoContext context, ClassReader cls) {
        var name = context.names().topLevel(context.names().suggestForClass(cls.getName()));
        var global = new WasmGlobal(name, WasmType.Reference.EXTERN, new WasmNullConstant(WasmType.Reference.EXTERN));
        context.module().globals.add(global);

        var expressions = new ArrayList<WasmExpression>();
        var className = context.strings().getStringConstant(cls.getName());
        var jsClassName = commonGen.stringToJs(context, new WasmGetGlobal(className.global));
        var createClass = new WasmCall(createClassFunction(context), jsClassName);
        expressions.add(new WasmSetGlobal(global, createClass));

        var members = AliasCollector.collectMembers(cls, AliasCollector::isInstanceMember);
        for (var aliasEntry : members.methods.entrySet()) {
            if (!aliasEntry.getValue().getClassName().equals(cls.getName())) {
                continue;
            }
            var fn = context.functions().forStaticMethod(aliasEntry.getValue());
            fn.setReferenced(true);
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = commonGen.stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineMethod = new WasmCall(defineMethodFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(fn));
            expressions.add(defineMethod);
        }

        for (var aliasEntry : members.properties.entrySet()) {
            var property = aliasEntry.getValue();
            if (!property.getter.getClassName().equals(cls.getName())) {
                continue;
            }
            var getter = context.functions().forStaticMethod(property.getter);
            getter.setReferenced(true);
            WasmFunction setter = null;
            if (property.setter != null) {
                setter = context.functions().forStaticMethod(property.setter);
                setter.setReferenced(true);
            }
            var setterRef = setter != null
                    ? new WasmFunctionReference(setter)
                    : new WasmNullConstant(WasmType.Reference.FUNC);
            var methodName = context.strings().getStringConstant(aliasEntry.getKey());
            var jsMethodName = commonGen.stringToJs(context, new WasmGetGlobal(methodName.global));
            var defineProperty = new WasmCall(definePropertyFunction(context), new WasmGetGlobal(global),
                    jsMethodName, new WasmFunctionReference(getter), setterRef);
            expressions.add(defineProperty);
        }

        context.addToInitializer(f -> f.getBody().addAll(expressions));
        return global;
    }

    private WasmFunction createClassFunction(WasmGCJsoContext context) {
        if (createClassFunction == null) {
            createClassFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    WasmType.Reference.EXTERN));
            createClassFunction.setName(context.names().suggestForClass("teavm.jso@createClass"));
            createClassFunction.setImportName("createClass");
            createClassFunction.setImportModule("teavmJso");
            context.module().functions.add(createClassFunction);
        }
        return createClassFunction;
    }

    private WasmFunction defineMethodFunction(WasmGCJsoContext context) {
        if (defineMethodFunction == null) {
            defineMethodFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC));
            defineMethodFunction.setName(context.names().suggestForClass("teavm.jso@defineMethod"));
            defineMethodFunction.setImportName("defineMethod");
            defineMethodFunction.setImportModule("teavmJso");
            context.module().functions.add(defineMethodFunction);
        }
        return defineMethodFunction;
    }

    private WasmFunction definePropertyFunction(WasmGCJsoContext context) {
        if (definePropertyFunction == null) {
            definePropertyFunction = new WasmFunction(context.functionTypes().of(null,
                    WasmType.Reference.EXTERN, WasmType.Reference.EXTERN, WasmType.Reference.FUNC,
                    WasmType.Reference.FUNC));
            definePropertyFunction.setName(context.names().suggestForClass("teavm.jso@defineProperty"));
            definePropertyFunction.setImportName("defineProperty");
            definePropertyFunction.setImportModule("teavmJso");
            context.module().functions.add(definePropertyFunction);
        }
        return definePropertyFunction;
    }
}
