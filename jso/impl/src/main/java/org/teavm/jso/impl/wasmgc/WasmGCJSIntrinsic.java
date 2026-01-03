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

import static org.teavm.jso.impl.JSMethods.JS_CLASS;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.STRING;
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
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmSequence;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.jso.impl.JSMethods;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSIntrinsic implements WasmGCIntrinsic {
    private WasmFunction globalFunction;
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGCJSFunctions functions;

    WasmGCJSIntrinsic(WasmGCJsoCommonGenerator commonGen, WasmGCJSFunctions functions) {
        this.commonGen = commonGen;
        this.functions = functions;
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
            case "get":
            case "getPure":
                return getProperty(invocation, context);
            case "importModule":
                return importModule(invocation, context);
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmExpression getProperty(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var result = tryGetFromModule(invocation, context);
        if (result != null) {
            return result;
        }
        var jsoContext = WasmGCJsoContext.wrap(context);
        return new WasmCall(functions.getGet(jsoContext), context.generate(invocation.getArguments().get(0)),
                context.generate(invocation.getArguments().get(1)));
    }

    private WasmExpression tryGetFromModule(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var target = invocation.getArguments().get(0);
        if (!(target instanceof InvocationExpr)) {
            return null;
        }
        var targetCall = (InvocationExpr) target;
        if (!targetCall.getMethod().equals(JSMethods.IMPORT_MODULE)) {
            return null;
        }
        var moduleName = extractString(targetCall.getArguments().get(0));
        if (moduleName == null) {
            return null;
        }

        var property = invocation.getArguments().get(1);
        if (!(property instanceof InvocationExpr)) {
            return null;
        }
        var propertyCall = (InvocationExpr) property;
        if (!propertyCall.getMethod().equals(JSMethods.WRAP_STRING)) {
            return null;
        }
        var name = extractString(propertyCall.getArguments().get(0));
        if (name == null) {
            return null;
        }

        var jsoContext = WasmGCJsoContext.wrap(context);
        var global = commonGen.getImportGlobal(jsoContext, moduleName, name);
        return new WasmGetGlobal(global);
    }

    private WasmExpression importModule(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var jsoContext = WasmGCJsoContext.wrap(context);
        var nameArg = invocation.getArguments().get(0);
        var name = extractString(nameArg);
        if (name == null) {
            context.diagnostics().error(new CallLocation(context.currentMethod(), invocation.getLocation()),
                    "Invalid JS module import call");
        }
        var global = commonGen.getImportGlobal(jsoContext, name, "__self__");
        return new WasmGetGlobal(global);
    }

    private String extractString(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return null;
        }
        var constant = ((ConstantExpr) expr).getValue();
        if (!(constant instanceof String)) {
            return null;
        }
        return (String) constant;
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
            globalFunction.setName(context.names().suggestForMethod(new MethodReference(JS_CLASS,
                    "global", STRING, JS_OBJECT)));
            globalFunction.setImportName("global");
            globalFunction.setImportModule("teavmJso");
            context.module().functions.add(globalFunction);
        }
        return globalFunction;
    }

    private WasmExpression throwCCEIfFalse(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var block = new WasmSequence();

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
