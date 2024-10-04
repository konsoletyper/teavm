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

import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.JS_TO_STRING;
import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.STRING_TO_JS;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.jso.JSObject;
import org.teavm.jso.impl.JS;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSIntrinsic implements WasmGCIntrinsic {
    private WasmFunction globalFunction;
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCJSIntrinsic(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "wrap":
                return wrapString(invocation.getArguments().get(0), context);
            case "unwrapString": {
                var function = context.functions().forStaticMethod(JS_TO_STRING);
                return new WasmCall(function, context.generate(invocation.getArguments().get(0)));
            }
            case "global": {
                var name = wrapString(invocation.getArguments().get(0), context);
                return new WasmCall(getGlobalFunction(context), name);
            }
            case "throwCCEIfFalse":
                return throwCCEIfFalse(invocation, context);
            case "isNull":
                return new WasmIsNull(context.generate(invocation.getArguments().get(0)));
            case "jsArrayItem":
                return arrayItem(invocation, context);
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmExpression wrapString(Expr stringExpr, WasmGCIntrinsicContext context) {
        if (stringExpr instanceof ConstantExpr) {
            var constantExpr = (ConstantExpr) stringExpr;
            if (constantExpr.getValue() instanceof String) {
                return commonGen.jsStringConstant(WasmGCJsoContext.wrap(context), (String) constantExpr.getValue());
            }
        }
        var function = context.functions().forStaticMethod(STRING_TO_JS);
        return new WasmCall(function, context.generate(stringExpr));
    }

    private WasmFunction getGlobalFunction(WasmGCIntrinsicContext context) {
        if (globalFunction == null) {
            globalFunction = new WasmFunction(context.functionTypes().of(WasmType.Reference.EXTERN,
                    WasmType.Reference.EXTERN));
            globalFunction.setName(context.names().suggestForMethod(new MethodReference(JS.class,
                    "global", String.class, JSObject.class)));
            globalFunction.setImportName("global");
            globalFunction.setImportModule("teavmJso");
            context.module().functions.add(globalFunction);
        }
        return globalFunction;
    }

    private WasmExpression throwCCEIfFalse(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var block = new WasmBlock(false);
        block.setType(WasmType.Reference.EXTERN);

        var innerBlock = new WasmBlock(false);
        block.getBody().add(innerBlock);
        var br = new WasmBranch(context.generate(invocation.getArguments().get(0)), innerBlock);
        innerBlock.getBody().add(br);

        var cceFunction = context.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "cce", ClassCastException.class));
        var cce = new WasmCall(cceFunction);
        var throwExpr = new WasmThrow(context.exceptionTag());
        throwExpr.getArguments().add(cce);
        innerBlock.getBody().add(throwExpr);

        block.getBody().add(context.generate(invocation.getArguments().get(1)));
        return block;
    }

    private WasmExpression arrayItem(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var array = context.generate(invocation.getArguments().get(0));
        var arrayType = context.classInfoProvider().getClassInfo(ValueType.parse(Object[].class)).getArray();
        return new WasmArrayGet(arrayType, array, context.generate(invocation.getArguments().get(1)));
    }
}
