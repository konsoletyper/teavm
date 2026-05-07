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
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
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
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "wrap":
                wrapString(invocation.getArguments().get(0), context, builder);
                break;
            case "unwrapString": {
                var function = context.functions().forStaticMethod(JS_TO_STRING);
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(function);
                break;
            }
            case "global": {
                wrapString(invocation.getArguments().get(0), context, builder);
                builder.call(getGlobalFunction(context));
                break;
            }
            case "throwCCEIfFalse":
                throwCCEIfFalse(invocation, context, builder);
                break;
            case "isNull":
                context.generate(builder, invocation.getArguments().get(0));
                builder.isNull();
                break;
            case "jsArrayItem":
                arrayItem(invocation, context, builder);
                break;
            case "get":
            case "getPure":
                getProperty(invocation, context, builder);
                break;
            case "importModule":
                importModule(invocation, context, builder);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void getProperty(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (!tryGetFromModule(invocation, context, builder)) {
            var jsoContext = WasmGCJsoContext.wrap(context);
            context.generate(builder, invocation.getArguments().get(0));
            context.generate(builder, invocation.getArguments().get(1));
            builder.call(functions.getGet(jsoContext));
        }
    }

    private boolean tryGetFromModule(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var target = invocation.getArguments().get(0);
        if (!(target instanceof InvocationExpr)) {
            return false;
        }
        var targetCall = (InvocationExpr) target;
        if (!targetCall.getMethod().equals(JSMethods.IMPORT_MODULE)) {
            return false;
        }
        var moduleName = extractString(targetCall.getArguments().get(0));
        if (moduleName == null) {
            return false;
        }

        var property = invocation.getArguments().get(1);
        if (!(property instanceof InvocationExpr)) {
            return false;
        }
        var propertyCall = (InvocationExpr) property;
        if (!propertyCall.getMethod().equals(JSMethods.WRAP_STRING)) {
            return false;
        }
        var name = extractString(propertyCall.getArguments().get(0));
        if (name == null) {
            return false;
        }

        var jsoContext = WasmGCJsoContext.wrap(context);
        var global = commonGen.getImportGlobal(jsoContext, moduleName, name);
        builder.getGlobal(global);
        return true;
    }

    private void importModule(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var jsoContext = WasmGCJsoContext.wrap(context);
        var nameArg = invocation.getArguments().get(0);
        var name = extractString(nameArg);
        if (name == null) {
            context.diagnostics().error(new CallLocation(context.currentMethod(), invocation.getLocation()),
                    "Invalid JS module import call");
        }
        var global = commonGen.getImportGlobal(jsoContext, name, "__self__");
        builder.getGlobal(global);
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

    private void wrapString(Expr stringExpr, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (stringExpr instanceof ConstantExpr) {
            var constantExpr = (ConstantExpr) stringExpr;
            if (constantExpr.getValue() instanceof String) {
                builder.getGlobal(commonGen.jsStringConstant(WasmGCJsoContext.wrap(context),
                        (String) constantExpr.getValue()));
                return;
            }
        }
        var function = context.functions().forStaticMethod(STRING_TO_JS);
        context.generate(builder, stringExpr);
        builder.call(function);
    }

    private WasmFunction getGlobalFunction(WasmGCIntrinsicContext context) {
        if (globalFunction == null) {
            globalFunction = new WasmFunction(context.functionTypes().of(WasmType.EXTERN, WasmType.EXTERN));
            globalFunction.setName(context.names().suggestForMethod(new MethodReference(JS_CLASS,
                    "global", STRING, JS_OBJECT)));
            globalFunction.setImportName("global");
            globalFunction.setImportModule("teavmJso");
            context.module().functions.add(globalFunction);
        }
        return globalFunction;
    }

    private void throwCCEIfFalse(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var outerBlock = builder.block(WasmType.EXTERN);
        var innerBlock = outerBlock.block();
        context.generate(innerBlock, invocation.getArguments().get(0));
        innerBlock.branch(innerBlock);
        var cceFunction = context.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "cce", ClassCastException.class));
        innerBlock.call(cceFunction);
        innerBlock.throw_(context.exceptionTag());
        context.generate(outerBlock, invocation.getArguments().get(1));
    }

    private void arrayItem(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        context.generate(builder, invocation.getArguments().get(0));
        context.generate(builder, invocation.getArguments().get(1));
        var arrayType = context.classInfoProvider().getClassInfo(ValueType.parse(Object[].class)).getArray();
        builder.arrayGet(arrayType);
    }
}
