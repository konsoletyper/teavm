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
package org.teavm.classlib.java.lang.reflect;

import java.lang.reflect.Array;
import java.util.Set;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplate;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.model.MethodReference;

public class ArrayNativeGenerator implements Generator {
    private JavaScriptTemplate template;

    public ArrayNativeGenerator(JavaScriptTemplateFactory templateFactory) {
        template = templateFactory.createFromResource("org/teavm/classlib/java/lang/reflect/Array.js");
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        if (methodRef.getName().equals("newInstanceImpl")) {
            generateNewInstance(context, writer);
            return;
        }
        template.builder(methodRef.getName()).withContext(context).build().write(writer, 0);
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer) {
        var dependency = context.getDependency().getMethod(new MethodReference(Array.class, "newInstance", Class.class,
                int.class, Object.class));
        template.builder("newInstanceImpl")
                .withContext(context)
                .withFragment("primitiveArrays", (w, p) -> {
                    w.append("switch").ws().append("(").append(context.getParameterName(1)).append(")")
                            .appendBlockStart();
                    var length = context.getParameterName(2);
                    var types = Set.of(dependency.getResult().getTypes());
                    if (types.contains("[Z")) {
                        writeArrayClause(w, "$rt_booleanArrayCls", "$rt_createBooleanArray", length);
                    }
                    if (types.contains("[B")) {
                        writeArrayClause(w, "$rt_byteArrayCls", "$rt_createByteArray", length);
                    }
                    if (types.contains("[C")) {
                        writeArrayClause(w, "$rt_charArrayCls", "$rt_createCharArray", length);
                    }
                    if (types.contains("[S")) {
                        writeArrayClause(w, "$rt_shortArrayCls", "$rt_createShortArray", length);
                    }
                    if (types.contains("[I")) {
                        writeArrayClause(w, "$rt_intArrayCls", "$rt_createIntArray", length);
                    }
                    if (types.contains("[J")) {
                        writeArrayClause(w, "$rt_longArrayCls", "$rt_createLongArray", length);
                    }
                    if (types.contains("~F")) {
                        writeArrayClause(w, "$rt_floatArrayCls", "$rt_createFloatArray", length);
                    }
                    if (types.contains("~D")) {
                        writeArrayClause(w, "$rt_doubleArrayCls", "$rt_createDoubleArray", length);
                    }
                    w.appendBlockEnd();
                })
                .build()
                .write(writer, 0);
    }

    private void writeArrayClause(SourceWriter writer, String test, String construct, String length) {
        writer.append("case ").appendFunction(test).append(":").ws().append("return ")
                .appendFunction(construct).append("(").append(length).append(");").softNewLine();
    }
}
