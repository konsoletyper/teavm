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
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;

class MetadataProviderNativeGenerator implements Generator {
    private MetadataGenerator generator;
    private MethodReference forMethod;

    MetadataProviderNativeGenerator(MetadataGenerator generator, MethodReference forMethod) {
        this.generator = generator;
        this.forMethod = forMethod;
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(context.getClassSource(),
                context.getClassLoader(), context.getProperties(), context);

        // Generate resource loader
        Resource resource = generator.generateMetadata(metadataContext, forMethod);
        writer.append("return ");
        ResourceWriterHelper.write(writer, resource);
        writer.append(';').softNewLine();
    }
}
