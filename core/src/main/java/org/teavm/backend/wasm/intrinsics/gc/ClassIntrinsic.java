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
package org.teavm.backend.wasm.intrinsics.gc;

import java.lang.annotation.Annotation;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.ValueType;

public class ClassIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "getWasmGCFlags": {
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls, context.classInfoProvider().getClassFlagsOffset());
                result.setLocation(invocation.getLocation());
                return result;
            }
            case "getComponentType": {
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls,
                        context.classInfoProvider().getClassArrayItemOffset());
                result.setLocation(invocation.getLocation());
                return result;
            }
            case "getEnclosingClass": {
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls,
                        context.classInfoProvider().getClassEnclosingClassOffset());
                result.setLocation(invocation.getLocation());
                return result;
            }
            case "getDeclaringClass": {
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls,
                        context.classInfoProvider().getClassDeclaringClassOffset());
                result.setLocation(invocation.getLocation());
                return result;
            }
            case "getSuperclass": {
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls, context.classInfoProvider().getClassParentOffset());
                result.setLocation(invocation.getLocation());
                return result;
            }
            case "getNameImpl":
                return generateGetName(invocation, context);
            case "setNameImpl":
                return generateSetName(invocation, context);
            case "getSimpleNameCache":
                return generateGetSimpleName(invocation, context);
            case "setSimpleNameCache":
                return generateSetSimpleName(invocation, context);
            case "getCanonicalNameCache":
                return generateGetCanonicalName(invocation, context);
            case "setCanonicalNameCache":
                return generateSetCanonicalName(invocation, context);
            case "getDeclaredAnnotationsImpl":
                return generateGetDeclaredAnnotations(invocation, context);
            case "getInterfacesImpl":
                return generateGetInterfaces(invocation, context);
            case "getDeclaredFieldsImpl":
                return generateGetDeclaredFields(invocation, context);
            case "getDeclaredMethodsImpl":
                return generateGetDeclaredMethods(invocation, context);
            default:
                throw new IllegalArgumentException("Unsupported invocation method: " + invocation.getMethod());
        }
    }

    private WasmExpression generateGetName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(classCls.getStructure(), arg, context.classInfoProvider().getClassNameOffset());
    }

    private WasmExpression generateSetName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        var value = context.generate(invocation.getArguments().get(1));
        return new WasmStructSet(classCls.getStructure(), arg, context.classInfoProvider().getClassNameOffset(),
                value);
    }

    private WasmExpression generateGetSimpleName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(classCls.getStructure(), arg, context.classInfoProvider().getClassSimpleNameOffset());
    }

    private WasmExpression generateSetSimpleName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        var value = context.generate(invocation.getArguments().get(1));
        return new WasmStructSet(classCls.getStructure(), arg,
                context.classInfoProvider().getClassSimpleNameOffset(), value);
    }

    private WasmExpression generateGetCanonicalName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        return new WasmStructGet(classCls.getStructure(), arg,
                context.classInfoProvider().getClassCanonicalNameOffset());
    }

    private WasmExpression generateSetCanonicalName(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var arg = context.generate(invocation.getArguments().get(0));
        var value = context.generate(invocation.getArguments().get(1));
        return new WasmStructSet(classCls.getStructure(), arg,
                context.classInfoProvider().getClassCanonicalNameOffset(), value);
    }

    private WasmExpression generateGetDeclaredAnnotations(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var util = new WasmGCGenerationUtil(context.classInfoProvider());
        return util.allocateArray(ValueType.parse(Annotation.class), a -> {
            var block = new WasmBlock(false);
            block.setType(a.getNonNullReference());
            var arg = context.generate(invocation.getArguments().get(0));
            var annotationsData = new WasmStructGet(classCls.getStructure(), arg,
                    context.classInfoProvider().getClassAnnotationsOffset());
            var nullCheck = new WasmNullBranch(WasmNullCondition.NOT_NULL, annotationsData, block);
            block.getBody().add(nullCheck);
            block.getBody().add(new WasmArrayNewFixed(a));
            return block;
        });
    }

    private WasmExpression generateGetInterfaces(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        var util = new WasmGCGenerationUtil(context.classInfoProvider());
        return util.allocateArray(ValueType.parse(Class.class), a -> {
            var block = new WasmBlock(false);
            block.setType(a.getNonNullReference());
            var arg = context.generate(invocation.getArguments().get(0));
            var interfacesData = new WasmStructGet(classCls.getStructure(), arg,
                    context.classInfoProvider().getClassInterfacesOffset());
            var nullCheck = new WasmNullBranch(WasmNullCondition.NOT_NULL, interfacesData, block);
            block.getBody().add(nullCheck);
            block.getBody().add(new WasmArrayNewFixed(a));
            return block;
        });
    }

    private WasmExpression generateGetDeclaredFields(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var fieldIndex = context.classInfoProvider().getClassFieldsOffset();
        if (fieldIndex < 0) {
            return new WasmUnreachable();
        }
        var arg = context.generate(invocation.getArguments().get(0));
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        return new WasmStructGet(classCls.getStructure(), arg, fieldIndex);
    }


    private WasmExpression generateGetDeclaredMethods(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var fieldIndex = context.classInfoProvider().getClassMethodsOffset();
        if (fieldIndex < 0) {
            return new WasmUnreachable();
        }
        var arg = context.generate(invocation.getArguments().get(0));
        var classCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        return new WasmStructGet(classCls.getStructure(), arg, fieldIndex);
    }
}
