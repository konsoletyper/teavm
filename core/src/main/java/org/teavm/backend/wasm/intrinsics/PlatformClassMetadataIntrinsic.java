/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.RuntimeClass;

public class PlatformClassMetadataIntrinsic implements WasmIntrinsic {
    private static final String PLATFORM_CLASS_METADATA = "org.teavm.platform.PlatformClassMetadata";
    private static final String RUNTIME_CLASS = RuntimeClass.class.getName();
    private static final FieldReference ITEM_TYPE_FIELD = new FieldReference(RUNTIME_CLASS, "itemType");
    private static final FieldReference SUPERCLASS_FIELD = new FieldReference(RUNTIME_CLASS, "parent");
    private static final FieldReference NAME_FIELD = new FieldReference(RUNTIME_CLASS, "name");
    private static final FieldReference SIMPLE_NAME_FIELD = new FieldReference(RUNTIME_CLASS, "simpleName");
    private static final FieldReference ENCLOSING_CLASS_FIELD = new FieldReference(RUNTIME_CLASS, "enclosingClass");
    private static final FieldReference DECLARING_CLASS_FIELD = new FieldReference(RUNTIME_CLASS, "declaringClass");

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(PLATFORM_CLASS_METADATA)) {
            return false;
        }
        switch (methodReference.getName()) {
            case "getArrayItem":
            case "getSuperclass":
            case "getName":
            case "getSimpleName":
            case "getEnclosingClass":
            case "getDeclaringClass":
                return true;
        }
        return false;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getArrayItem":
                return fieldAccess(manager, invocation, ITEM_TYPE_FIELD);
            case "getSuperclass":
                return fieldAccess(manager, invocation, SUPERCLASS_FIELD);
            case "getName":
                return fieldAccess(manager, invocation, NAME_FIELD);
            case "getSimpleName":
                return fieldAccess(manager, invocation, SIMPLE_NAME_FIELD);
            case "getEnclosingClass":
                return fieldAccess(manager, invocation, ENCLOSING_CLASS_FIELD);
            case "getDeclaringClass":
                return fieldAccess(manager, invocation, DECLARING_CLASS_FIELD);
            default:
                return new WasmUnreachable();
        }
    }

    private WasmExpression fieldAccess(WasmIntrinsicManager manager, InvocationExpr expr, FieldReference field) {
        QualificationExpr qualification = new QualificationExpr();
        qualification.setField(field);
        qualification.setQualified(expr.getArguments().get(0));
        qualification.setLocation(expr.getLocation());
        return manager.generate(qualification);
    }
}
