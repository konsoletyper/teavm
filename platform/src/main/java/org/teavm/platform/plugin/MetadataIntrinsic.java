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
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.common.ServiceRepository;
import org.teavm.model.CallLocation;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataProvider;
import org.teavm.platform.metadata.Resource;

public class MetadataIntrinsic implements WasmIntrinsic {
    private ListableClassReaderSource classSource;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;

    public MetadataIntrinsic(ListableClassReaderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Properties properties) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.services = services;
        this.properties = properties;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        MethodReader method = classSource.resolve(methodReference);
        if (method == null) {
            return false;
        }

        return method.getAnnotations().get(MetadataProvider.class.getName()) != null;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        MethodReader method = classSource.resolve(invocation.getMethod());
        MetadataGenerator generator = MetadataUtils.createMetadataGenerator(classLoader, method,
                new CallLocation(invocation.getMethod()), manager.getDiagnostics());
        if (generator == null) {
            return new WasmInt32Constant(0);
        }

        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(classSource,
                classLoader, properties, services);
        Resource resource = generator.generateMetadata(metadataContext, invocation.getMethod());

        return null;
    }
}
