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

import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.STRING_TO_JS;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.backend.javascript.rendering.AstWriter;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.jso.JSObject;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.jso.impl.JSBodyBloatedEmitter;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJsoCommonGenerator {
    private WasmGCJSFunctions jsFunctions;
    private boolean initialized;
    private List<Consumer<WasmFunction>> initializerParts = new ArrayList<>();
    private boolean rethrowExported;

    WasmGCJsoCommonGenerator(WasmGCJSFunctions jsFunctions) {
        this.jsFunctions = jsFunctions;
    }

    private void initialize(WasmGCJsoContext context) {
        if (initialized) {
            return;
        }
        initialized = true;
        context.addToInitializer(this::writeToInitializer);
        exportRethrowException(context);
    }

    private void writeToInitializer(WasmFunction function) {
        for (var part : initializerParts) {
            part.accept(function);
        }
    }

    void addInitializerPart(WasmGCJsoContext context, Consumer<WasmFunction> part) {
        initialize(context);
        initializerParts.add(part);
    }

    WasmGlobal addJSBody(WasmGCJsoContext context, JSBodyEmitter emitter, boolean inlined) {
        initialize(context);
        var paramCount = emitter.method().parameterCount();
        if (!emitter.isStatic()) {
            paramCount++;
        }
        var global = new WasmGlobal(context.names().suggestForMethod(emitter.method()),
                WasmType.Reference.EXTERN, new WasmNullConstant(WasmType.Reference.EXTERN));
        context.module().globals.add(global);
        var body = "";
        if (emitter instanceof JSBodyBloatedEmitter) {
            body = ((JSBodyBloatedEmitter) emitter).script;
        } else if (emitter instanceof JSBodyAstEmitter) {
            var writer = new WasmGCJSBodyWriter();
            if (inlined) {
                writer.sb.append("return ");
            }
            var astEmitter = (JSBodyAstEmitter) emitter;
            var astWriter = new AstWriter(writer, name -> (w, prec) -> w.append(name));
            if (!emitter.isStatic()) {
                astWriter.declareNameEmitter("this", (w, prec) -> w.append("__this__"));
            }
            astWriter.print(astEmitter.ast);
            if (inlined) {
                writer.sb.append(";");
            }
            body = writer.sb.toString();
        } else {
            throw new IllegalArgumentException();
        }

        var constructor = new WasmCall(jsFunctions.getFunctionConstructor(context, paramCount));
        var paramNames = new ArrayList<String>();
        if (!emitter.isStatic()) {
            paramNames.add("__this__");
        }
        paramNames.addAll(List.of(emitter.parameterNames()));
        for (var parameter : paramNames) {
            var paramName = new WasmGetGlobal(context.strings().getStringConstant(parameter).global);
            constructor.getArguments().add(stringToJs(context, paramName));
        }
        var functionBody = new WasmGetGlobal(context.strings().getStringConstant(body).global);
        constructor.getArguments().add(stringToJs(context, functionBody));
        initializerParts.add(initializer -> initializer.getBody().add(new WasmSetGlobal(global, constructor)));

        return global;
    }

    private WasmFunction stringToJsFunction(WasmGCJsoContext context) {
        return context.functions().forStaticMethod(STRING_TO_JS);
    }

    WasmExpression stringToJs(WasmGCJsoContext context, WasmExpression str) {
        return new WasmCall(stringToJsFunction(context), str);
    }

    private void exportRethrowException(WasmGCJsoContext context) {
        if (rethrowExported) {
            return;
        }
        rethrowExported = true;
        var fn = context.functions().forStaticMethod(new MethodReference(WasmGCJSRuntime.class, "wrapException",
                JSObject.class, Throwable.class));
        fn.setExportName("teavm.js.wrapException");

        fn = context.functions().forStaticMethod(new MethodReference(WasmGCJSRuntime.class, "extractException",
                Throwable.class, JSObject.class));
        fn.setExportName("teavm.js.extractException");

        createThrowExceptionFunction(context);
    }

    private void createThrowExceptionFunction(WasmGCJsoContext context) {
        var fn = new WasmFunction(context.functionTypes().of(null, WasmType.Reference.EXTERN));
        fn.setName(context.names().topLevel("teavm@throwException"));
        fn.setExportName("teavm.js.throwException");
        context.module().functions.add(fn);

        var exceptionLocal = new WasmLocal(WasmType.Reference.EXTERN);
        fn.add(exceptionLocal);

        var asAny = new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY, new WasmGetLocal(exceptionLocal));
        var throwableType = (WasmType.Reference) context.typeMapper().mapType(ValueType.parse(Throwable.class));
        var asThrowable = new WasmCast(asAny, throwableType);
        var throwExpr = new WasmThrow(context.exceptionTag());
        throwExpr.getArguments().add(asThrowable);
        fn.getBody().add(throwExpr);
    }
}
