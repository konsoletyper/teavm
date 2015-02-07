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
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
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
        // Validate method
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());
        if (providerAnnot == null) {
            return;
        }
        if (!method.hasModifier(ElementModifier.NATIVE)) {
            throw new IllegalStateException("Method " + method.getReference() + " was marked with " +
                    MetadataProvider.class.getName() + " but it is not native");
        }

        // Find and instantiate metadata generator
        ValueType generatorType = providerAnnot.getValue("value").getJavaClass();
        String generatorClassName = ((ValueType.Object)generatorType).getClassName();
        Class<?> generatorClass;
        try {
            generatorClass = Class.forName(generatorClassName, true, context.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find metadata generator class: " + generatorClassName, e);
        }
        Constructor<?> cons;
        try {
            cons = generatorClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Metadata generator " + generatorClassName + " does not have a public " +
                    "no-arg constructor", e);
        }
        MetadataGenerator generator;
        try {
            generator = (MetadataGenerator)cons.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Error instantiating metadata generator " + generatorClassName, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error instantiating metadata generator " + generatorClassName,
                    e.getTargetException());
        }
        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(context.getClassSource(),
                context.getClassLoader(), context.getProperties(), context);

        // Generate resource loader
        Resource resource = generator.generateMetadata(metadataContext, methodRef);
        writer.append("if (!window.hasOwnProperty(\"").appendMethodBody(methodRef).append("$$resource\")) {")
                .indent().softNewLine();
        writer.append("window.").appendMethodBody(methodRef).append("$$resource = ");
        ResourceWriterHelper.write(writer, resource);
        writer.append(';').softNewLine();
        writer.outdent().append('}').softNewLine();
        writer.append("return ").appendMethodBody(methodRef).append("$$resource;").softNewLine();
    }
}
