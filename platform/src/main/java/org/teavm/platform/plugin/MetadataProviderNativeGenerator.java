/*
 *  Copyright 2014 Alexey Andreev.
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
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.*;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataProvider;
import org.teavm.platform.metadata.Resource;

/**
 *
 * @author Alexey Andreev
 */
public class MetadataProviderNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());

        AnnotationReader refAnnot = method.getAnnotations().get(MetadataProviderRef.class.getName());
        methodRef = MethodReference.parse(refAnnot.getValue("value").getString());

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
        MetadataGenerator generator;
        try {
            generator = (MetadataGenerator) cons.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            context.getDiagnostics().error(new CallLocation(methodRef), "Error instantiating metadata "
                    + "generator {{c0}}", generatorClassName);
            return;
        }
        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(context.getClassSource(),
                context.getClassLoader(), context.getProperties(), context);

        // Generate resource loader
        Resource resource = generator.generateMetadata(metadataContext, methodRef);
        writer.append("return ");
        ResourceWriterHelper.write(writer, resource);
        writer.append(';').softNewLine();
    }
}
