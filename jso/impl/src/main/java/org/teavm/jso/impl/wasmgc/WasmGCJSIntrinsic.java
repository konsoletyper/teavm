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
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.impl.JSMethods;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvokeInstruction;

class WasmGCJSIntrinsic implements WasmGCInlineIntrinsic {
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGCJSFunctions jsFunctions;
    private Diagnostics diagnostics;
    private WasmGCClassInfoProvider classInfoProvider;
    private BaseWasmFunctionRepository functions;
    private WasmFunctionTypes functionTypes;
    private WasmGCNameProvider names;
    private WasmModule module;
    private WasmTag exceptionTag;

    private WasmFunction globalFunction;

    WasmGCJSIntrinsic(WasmGCJsoCommonGenerator commonGen, WasmGCJSFunctions jsFunctions, Diagnostics diagnostics,
            WasmGCClassInfoProvider classInfoProvider, BaseWasmFunctionRepository functions,
            WasmFunctionTypes functionTypes, WasmGCNameProvider names, WasmModule module, WasmTag exceptionTag) {
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
        this.diagnostics = diagnostics;
        this.classInfoProvider = classInfoProvider;
        this.functions = functions;
        this.functionTypes = functionTypes;
        this.names = names;
        this.module = module;
        this.exceptionTag = exceptionTag;
    }

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "wrap":
                wrapString(invocation.getArguments().get(0), context, builder);
                break;
            case "unwrapString": {
                var function = functions.forStaticMethod(JS_TO_STRING);
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.call(function);
                break;
            }
            case "global": {
                wrapString(invocation.getArguments().get(0), context, builder);
                builder.call(getGlobalFunction());
                break;
            }
            case "throwCCEIfFalse":
                throwCCEIfFalse(invocation, context, builder);
                break;
            case "isNull":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
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

    private void getProperty(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (!tryGetFromModule(invocation, context, builder)) {
            builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
            builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
            builder.call(jsFunctions.getGet());
        }
    }

    private boolean tryGetFromModule(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var target = context.definition(invocation.getArguments().get(0));
        if (!(target instanceof InvokeInstruction targetCall)) {
            return false;
        }
        if (!targetCall.getMethod().equals(JSMethods.IMPORT_MODULE)) {
            return false;
        }
        var moduleName = context.stringConstant(targetCall.getArguments().get(0));
        if (moduleName == null) {
            return false;
        }

        var property = context.definition(invocation.getArguments().get(1));
        if (!(property instanceof InvokeInstruction propertyCall)) {
            return false;
        }
        if (!propertyCall.getMethod().equals(JSMethods.WRAP_STRING)) {
            return false;
        }
        var name = context.stringConstant(propertyCall.getArguments().get(0));
        if (name == null) {
            return false;
        }

        var global = commonGen.getImportGlobal(moduleName, name);
        builder.getGlobal(global);
        return true;
    }

    private void importModule(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var name = context.stringConstant(invocation.getArguments().get(0));
        if (name == null) {
            diagnostics.error(new CallLocation(context.currentMethod(), invocation.getLocation()),
                    "Invalid JS module import call");
        }
        var global = commonGen.getImportGlobal(name, "__self__");
        builder.getGlobal(global);
    }

    private void wrapString(Variable string, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var constant = context.stringConstant(string);
        if (constant != null) {
            builder.getGlobal(commonGen.jsStringConstant(constant));
            return;
        }
        var function = functions.forStaticMethod(STRING_TO_JS);
        builder.getLocal(context.mapToLocal(string));
        builder.call(function);
    }

    private WasmFunction getGlobalFunction() {
        if (globalFunction == null) {
            globalFunction = new WasmFunction(functionTypes.of(WasmType.EXTERN, WasmType.EXTERN));
            globalFunction.setName(names.topLevel(names.suggestForMethod(new MethodReference(JS_CLASS,
                    "global", STRING, JS_OBJECT))));
            globalFunction.setImportName("global");
            globalFunction.setImportModule("teavmJso");
            module.functions.add(globalFunction);
        }
        return globalFunction;
    }

    private void throwCCEIfFalse(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var outerBlock = builder.block(WasmType.EXTERN);
        var innerBlock = outerBlock.block();
        innerBlock.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
        innerBlock.branch(innerBlock);
        var cceFunction = functions.forStaticMethod(new MethodReference(WasmGCSupport.class, "cce",
                ClassCastException.class));
        innerBlock.call(cceFunction);
        innerBlock.throw_(exceptionTag);
        outerBlock.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
    }

    private void arrayItem(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
        builder.getLocal(context.mapToLocal(invocation.getArguments().get(1)));
        var arrayType = classInfoProvider.getClassInfo(ValueType.parse(Object[].class)).getArray();
        builder.arrayGet(arrayType);
    }
}
