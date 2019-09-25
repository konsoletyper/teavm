/*
 *  Copyright 2019 konsoletyper.
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
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;

public class DeclaringClassGenerator implements Generator {
    private static final MethodReference METHOD = new MethodReference(Class.class, "getDeclaringClass",
            Class.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        writer.append("var p").ws().append("=").ws().append("\"teavm_declaredClass\";").softNewLine();
        writer.append("if").ws().append("(!").appendMethodBody(methodRef).append(".initialized").append(")")
                .ws().append("{").indent().softNewLine();

        MethodDependencyInfo methodDep = context.getDependency().getMethod(METHOD);
        if (methodDep != null) {
            for (String type : methodDep.getVariable(0).getClassValueNode().getTypes()) {
                ClassReader cls = context.getClassSource().get(type);
                if (cls != null) {
                    writer.appendClass(type).append("[p]").ws().append("=").ws();
                    if (cls.getOwnerName() != null) {
                        writer.appendClass(cls.getOwnerName());
                    } else {
                        writer.append("null");
                    }
                    writer.append(";").softNewLine();
                }
            }
        }

        writer.appendMethodBody(methodRef).append(".initialized").ws().append("=").ws().append("true;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return ").append(context.getParameterName(1)).append("[p];").softNewLine();
    }
}
