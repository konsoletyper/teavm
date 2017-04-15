/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.Resource;

public class ResourceReadIntrinsic implements WasmIntrinsic {
    private ClassReaderSource classSource;

    public ResourceReadIntrinsic(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return classSource.isSuperType(Resource.class.getTypeName(), methodReference.getClassName()).orElse(false);
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        return null;
    }
}
