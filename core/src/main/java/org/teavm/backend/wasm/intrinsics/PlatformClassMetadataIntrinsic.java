/*
 *  Copyright 2016 Alexey Andreev.
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
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.RuntimeClass;
import org.teavm.backend.wasm.model.expression.WasmExpression;

public class PlatformClassMetadataIntrinsic implements WasmIntrinsic {
    private static final String PLATFORM_CLASS_METADATA_NAME = "org.teavm.platform.PlatformClassMetadata";

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.getClassName().equals(PLATFORM_CLASS_METADATA_NAME);
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getArrayItem": {
                QualificationExpr expr = new QualificationExpr();
                expr.setQualified(invocation.getArguments().get(0));
                expr.setField(new FieldReference(RuntimeClass.class.getName(), "itemType"));
                expr.setLocation(invocation.getLocation());
                return manager.generate(expr);
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
