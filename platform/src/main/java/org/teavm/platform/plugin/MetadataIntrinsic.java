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

import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataProvider;

public class MetadataIntrinsic implements WasmIntrinsic {
    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private Properties properties;

    public MetadataIntrinsic(ClassReaderSource classSource, ClassLoader classLoader, Properties properties) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.properties = properties;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        ClassReader cls = classSource.get(methodReference.getClassName());
        if (cls == null) {
            return false;
        }

        AnnotationReader annot = cls.getAnnotations().get(MetadataProvider.class.getName());
        return annot != null;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        return null;
    }
}
