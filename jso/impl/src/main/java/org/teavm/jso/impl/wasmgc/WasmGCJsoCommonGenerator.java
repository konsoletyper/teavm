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
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.jso.impl.JSBodyBloatedEmitter;
import org.teavm.jso.impl.JSBodyEmitter;

class WasmGCJsoCommonGenerator {
    private WasmGCJSFunctions jsFunctions;
    private boolean initialized;
    private List<Consumer<WasmFunction>> initializerParts = new ArrayList<>();

    WasmGCJsoCommonGenerator(WasmGCJSFunctions jsFunctions) {
        this.jsFunctions = jsFunctions;
    }

    private void initialize(WasmGCJsoContext context) {
        if (initialized) {
            return;
        }
        initialized = true;
        context.addToInitializer(this::writeToInitializer);
    }

    private void writeToInitializer(WasmFunction function) {
        for (var part : initializerParts) {
            part.accept(function);
        }
    }

    void addInitializerPart(Consumer<WasmFunction> part) {
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
}
