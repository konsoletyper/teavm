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

import java.util.Collection;
import java.util.ServiceLoader;
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.generators.GeneratorContext;
import org.teavm.backend.c.generators.GeneratorFactory;
import org.teavm.backend.c.generators.GeneratorFactoryContext;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.interop.Address;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;

public class ServiceLoaderCSupport implements GeneratorFactory {
    static final MethodReference ALLOC_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateArray", RuntimeClass.class, int.class, Address.class);
    static final MethodReference ALLOC_METHOD = new MethodReference(Allocator.class,
            "allocate", RuntimeClass.class, Address.class);
    static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", ValueType.VOID);

    @Override
    public Generator createGenerator(GeneratorFactoryContext context) {
        return new ServiceLoaderIntrinsic(context.getServices().getService(ServiceLoaderInformation.class));
    }

    static class ServiceLoaderIntrinsic implements Generator {
        private ServiceLoaderInformation information;

        ServiceLoaderIntrinsic(ServiceLoaderInformation information) {
            this.information = information;
        }

        @Override
        public boolean canHandle(MethodReference method) {
            if (!method.getClassName().equals(ServiceLoader.class.getName())) {
                return false;
            }
            return method.getName().equals("loadServices");
        }

        @Override
        public void generate(GeneratorContext context, MethodReference method) {
            CodeWriter writer = context.writer();
            CodeWriter beforeWriter = context.writerBefore();
            NameProvider names = context.names();
            context.includes().addInclude("<stdbool.h>");
            Collection<? extends String> serviceTypes = information.serviceTypes();

            writer.println("static bool initialized = false;");
            writer.println("if (!initialized) {").indent();
            String methodName = names.forMethod(method);
            int index = 0;
            for (String serviceType : serviceTypes) {
                Collection<? extends String> implementations = information.serviceImplementations(serviceType);
                if (implementations.isEmpty()) {
                    continue;
                }

                String staticFieldName = methodName + "_" + index++;
                context.includes().includeClass(serviceType);
                writer.print(names.forClassInstance(ValueType.object(serviceType)))
                    .print(".services = (TeaVM_Services*) &").print(staticFieldName).println(";");

                beforeWriter.print("static struct { int32_t size; ")
                        .print("TeaVM_Service entries[" + implementations.size() + "]; } ")
                        .print(staticFieldName + " = { .size = " + implementations.size() + ", ")
                        .print(".entries = {").indent();
                boolean first = true;
                for (String implementation : implementations) {
                    if (!first) {
                        beforeWriter.print(",");
                    }
                    first = false;
                    context.includes().includeClass(implementation);
                    MethodReference constructor = new MethodReference(implementation, INIT_METHOD);
                    context.importMethod(constructor, false);
                    beforeWriter.println().print("{ .cls = (TeaVM_Class*) &")
                            .print(names.forClassInstance(ValueType.object(implementation)))
                            .print(", .constructor = &").print(names.forMethod(constructor))
                            .print(" }");
                }
                if (!first) {
                    beforeWriter.println();
                }
                beforeWriter.outdent().println("}};");
            }
            writer.outdent().println("}");

            CallSiteLocation location = new CallSiteLocation(null, method.getClassName(), method.getName(), -1);
            CallSiteDescriptor callSite = context.createCallSite(new CallSiteLocation[] { location },
                    new ExceptionHandlerDescriptor[0]);
            writer.println("TEAVM_ALLOC_STACK(INT32_C(2));");
            writer.println("TEAVM_CALL_SITE(" + callSite.getId() + ");");

            writer.println("TeaVM_Array* result = NULL;");
            writer.print("TeaVM_Services* services = ((TeaVM_Class*) ").print(context.parameterName(1))
                    .println(")->services;");
            writer.println("if (services == NULL) goto exit;");
            writer.println("TEAVM_GC_ROOT_RELEASE(0);");
            writer.println("TEAVM_GC_ROOT_RELEASE(1);");
            writer.print("result = ")
                    .print(names.forMethod(ALLOC_ARRAY_METHOD)).print("(&")
                    .print(names.forClassInstance(ValueType.parse(Object[].class))).print(", ")
                    .println("services->size);");
            if (!context.usesLongjmp()) {
                writer.println("if (TEAVM_EXCEPTION_HANDLER != " + callSite.getId() + ") goto exit;");
            }

            writer.println("TEAVM_GC_ROOT(0, result);");
            writer.println("void** arrayData = (void**) TEAVM_ARRAY_DATA(result, void*);");
            writer.println("for (int32_t i = 0; i < services->size; ++i) {").indent();
            writer.print("void* obj = ").print(names.forMethod(ALLOC_METHOD)).println("(services->entries[i].cls);");
            if (!context.usesLongjmp()) {
                writer.println("if (TEAVM_EXCEPTION_HANDLER != " + callSite.getId() + ") goto exit;");
            }
            writer.println("TEAVM_GC_ROOT(1, obj);");
            writer.println("services->entries[i].constructor(obj);");
            if (!context.usesLongjmp()) {
                writer.println("if (TEAVM_EXCEPTION_HANDLER != " + callSite.getId() + ") goto exit;");
            }
            writer.println("arrayData[i] = obj;");
            writer.outdent().println("}");

            writer.println("exit: TEAVM_RELEASE_STACK;");
            writer.println("return result;");
        }
    }
}
