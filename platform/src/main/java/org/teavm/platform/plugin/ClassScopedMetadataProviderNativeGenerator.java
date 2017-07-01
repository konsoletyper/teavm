/*
 *  Copyright 2015 Alexey Andreev.
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.ClassScopedMetadataGenerator;
import org.teavm.platform.metadata.ClassScopedMetadataProvider;
import org.teavm.platform.metadata.Resource;

public class ClassScopedMetadataProviderNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        // Validate method
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader providerAnnot = method.getAnnotations().get(ClassScopedMetadataProvider.class.getName());
        if (providerAnnot == null) {
            return;
        }
        if (!method.hasModifier(ElementModifier.NATIVE)) {
            context.getDiagnostics().error(new CallLocation(methodRef), "Method {{m0}} is marked with "
                    + "{{c1}} annotation, but it is not native", methodRef,
                    ClassScopedMetadataProvider.class.getName());
            return;
        }

        // Find and instantiate metadata generator
        ValueType generatorType = providerAnnot.getValue("value").getJavaClass();
        String generatorClassName = ((ValueType.Object) generatorType).getClassName();
        Class<?> generatorClass;
        try {
            generatorClass = Class.forName(generatorClassName, true, context.getClassLoader());
        } catch (ClassNotFoundException e) {
            context.getDiagnostics().error(new CallLocation(methodRef), "Can't find metadata provider class {{c0}}",
                    generatorClassName);
            return;
        }
        Constructor<?> cons;
        try {
            cons = generatorClass.getConstructor();
        } catch (NoSuchMethodException e) {
            context.getDiagnostics().error(new CallLocation(methodRef), "Metadata generator {{c0}} does not have "
                    + "a public no-arg constructor", generatorClassName);
            return;
        }
        ClassScopedMetadataGenerator generator;
        try {
            generator = (ClassScopedMetadataGenerator) cons.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            context.getDiagnostics().error(new CallLocation(methodRef), "Error instantiating metadata "
                    + "generator {{c0}}", generatorClassName);
            return;
        }
        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(context.getClassSource(),
                context.getClassLoader(), context.getProperties(), context);

        Map<String, Resource> resourceMap = generator.generateMetadata(metadataContext, methodRef);
        writer.append("var p").ws().append("=").ws().append("\"" + RenderingUtil.escapeString("$$res_"
                + writer.getNaming().getFullNameFor(methodRef)) + "\"").append(";").softNewLine();
        for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
            writer.appendClass(entry.getKey()).append("[p]").ws().append("=").ws();
            ResourceWriterHelper.write(writer, entry.getValue());
            writer.append(";").softNewLine();
        }
        writer.appendMethodBody(methodRef).ws().append('=').ws().append("function(cls)").ws().append("{")
                .softNewLine().indent();
        writer.append("return cls.hasOwnProperty(p)").ws().append("?").ws().append("cls[p]").ws().append(":")
                .ws().append("null;").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("return ").appendMethodBody(methodRef).append("(").append(context.getParameterName(1))
                .append(");").softNewLine();
    }
}
