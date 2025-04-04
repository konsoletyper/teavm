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
package org.teavm.backend.wasm.generators.gc;

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.model.MethodReference;

public class ClassGenerators implements WasmGCCustomGenerator {
    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        switch (method.getName()) {
            case "isAssignableFrom":
                generateIsAssignable(function, context);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    private void generateIsAssignable(WasmFunction function, WasmGCCustomGeneratorContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var thisVar = new WasmLocal(context.isCompactMode() ? WasmType.Reference.ANY : classCls.getType());
        var otherClassVar = new WasmLocal(classCls.getType());
        function.add(thisVar);
        function.add(otherClassVar);

        var conditional = new WasmConditional(new WasmIsNull(new WasmGetLocal(otherClassVar)));
        function.getBody().add(conditional);
        var npe = new WasmCall(context.functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "npe",
                NullPointerException.class)));
        var throwExpr = new WasmThrow(context.exceptionTag());
        throwExpr.getArguments().add(npe);
        conditional.getThenBlock().getBody().add(throwExpr);

        WasmExpression thisExpr = new WasmGetLocal(thisVar);
        if (context.isCompactMode()) {
            thisExpr = new WasmCast(thisExpr, classCls.getType());
        }
        var functionRef = new WasmStructGet(classCls.getStructure(), thisExpr,
                context.classInfoProvider().getClassSupertypeFunctionOffset());
        var call = new WasmCallReference(functionRef,
                context.functionTypes().of(WasmType.INT32, classCls.getType(), classCls.getType()));

        thisExpr = new WasmGetLocal(thisVar);
        if (context.isCompactMode()) {
            thisExpr = new WasmCast(thisExpr, classCls.getType());
        }
        call.getArguments().add(thisExpr);
        call.getArguments().add(new WasmGetLocal(otherClassVar));

        function.getBody().add(call);
    }
}
