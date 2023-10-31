/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.classlib.impl;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class ServiceLoaderJSSupport implements Generator {
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        var templateFactory = new JavaScriptTemplateFactory(context.getClassLoader(), context.getClassSource());
        var template = templateFactory.createFromResource("org/teavm/classlib/java/util/ServiceLoader.js");
        var information = context.getService(ServiceLoaderInformation.class);
        var fragment = template.builder("loadServices")
                .withContext(context)
                .withFragment("fillServices", (w, precedence) -> {
                    for (var serviceType : information.serviceTypes()) {
                        writer.appendClass(serviceType).append(".$$serviceList$$ = [");
                        var implementations = information.serviceImplementations(serviceType);
                        boolean first = true;
                        for (var implName : implementations) {
                            if (context.getClassSource().getClassNames().contains(implName)) {
                                if (!first) {
                                    writer.append(",").ws();
                                }
                                first = false;
                                writer.append("[").appendClass(implName).append(",").ws()
                                        .appendMethodBody(new MethodReference(implName, INIT_METHOD))
                                        .append("]");
                            }
                        }
                        writer.append("];").softNewLine();
                    }
                })
                .build();
        fragment.write(writer, 0);
    }
}
