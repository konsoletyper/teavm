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

import java.io.IOException;
import java.util.Collection;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class ServiceLoaderJSSupport implements Generator {
    private static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        ServiceLoaderInformation information = context.getService(ServiceLoaderInformation.class);
        writer.append("if (!").appendClass("java.util.ServiceLoader").append(".$$services$$) {").indent()
                .softNewLine();
        writer.appendClass("java.util.ServiceLoader").append(".$$services$$ = true;").softNewLine();
        for (String serviceType : information.serviceTypes()) {
            writer.appendClass(serviceType).append(".$$serviceList$$ = [");
            Collection<? extends String> implementations = information.serviceImplementations(serviceType);
            boolean first = true;
            for (String implName : implementations) {
                if (context.getClassSource().getClassNames().contains(implName)) {
                    if (!first) {
                        writer.append(", ");
                    }
                    first = false;
                    writer.append("[").appendClass(implName).append(", ").appendMethodBody(
                            new MethodReference(implName, INIT_METHOD))
                            .append("]");
                }
            }
            writer.append("];").softNewLine();
        }
        writer.outdent().append("}").softNewLine();
        String param = context.getParameterName(1);
        writer.append("var cls = " + param + ";").softNewLine();
        writer.append("if (!cls.$$serviceList$$) {").indent().softNewLine();
        writer.append("return $rt_createArray($rt_objcls(), 0);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var result = $rt_createArray($rt_objcls(), cls.$$serviceList$$.length);").softNewLine();
        writer.append("for (var i = 0; i < result.data.length; ++i) {").indent().softNewLine();
        writer.append("var serviceDesc = cls.$$serviceList$$[i];").softNewLine();
        writer.append("result.data[i] = new serviceDesc[0]();").softNewLine();
        writer.append("serviceDesc[1](result.data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
