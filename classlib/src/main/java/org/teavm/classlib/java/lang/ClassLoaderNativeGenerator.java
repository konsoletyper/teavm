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
package org.teavm.classlib.java.lang;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;
import org.teavm.classlib.impl.Base64Impl;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;

public class ClassLoaderNativeGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "supplyResources":
                generateSupplyResources(context);
                break;
        }
    }

    private void generateSupplyResources(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("{").indent();

        ClassLoader classLoader = context.getClassLoader();
        Set<String> resourceSet = new HashSet<>();
        SupplierContextImpl supplierContext = new SupplierContextImpl(context);
        for (ResourceSupplier supplier : ServiceLoader.load(ResourceSupplier.class, classLoader)) {
            String[] resources = supplier.supplyResources(supplierContext);
            if (resources != null) {
                resourceSet.addAll(Arrays.asList(resources));
            }
        }

        boolean first = true;
        for (String resource : resourceSet) {
            try (InputStream input = classLoader.getResourceAsStream(resource)) {
                if (input == null) {
                    continue;
                }
                if (!first) {
                    writer.append(',');
                }
                first = false;
                writer.newLine();
                byte[] dataBytes = Base64Impl.encode(IOUtils.toByteArray(new BufferedInputStream(input)), true);
                char[] dataChars = new char[dataBytes.length];
                for (int i = 0; i < dataBytes.length; ++i) {
                    dataChars[i] = (char) dataBytes[i];
                }
                writer.append("\"").append(RenderingUtil.escapeString(resource)).append("\"");
                writer.ws().append(':').ws();
                writer.append("\"").append(new String(dataChars)).append("\"");
            }
        }

        if (!first) {
            writer.newLine();
        }
        writer.outdent().append('}');
    }

    static class SupplierContextImpl implements ResourceSupplierContext {
        InjectorContext injectorContext;

        public SupplierContextImpl(InjectorContext injectorContext) {
            this.injectorContext = injectorContext;
        }

        @Override
        public ClassLoader getClassLoader() {
            return injectorContext.getClassLoader();
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return injectorContext.getClassSource();
        }

        @Override
        public Properties getProperties() {
            return injectorContext.getProperties();
        }
    }
}
