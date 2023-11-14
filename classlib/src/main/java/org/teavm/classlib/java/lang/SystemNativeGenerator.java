/*
 *  Copyright 2013 Alexey Andreev.
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

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplate;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.model.MethodReference;

public class SystemNativeGenerator implements Generator {
    private JavaScriptTemplate template;

    public SystemNativeGenerator(JavaScriptTemplateFactory templateFactory) {
        template = templateFactory.createFromResource("org/teavm/classlib/java/lang/System.js");
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        var fragment = template.builder(methodRef.getName()).withContext(context).build();
        fragment.write(writer, 0);
    }
}
