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
package org.teavm.jso.plugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class JSBodyGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader annot = method.getAnnotations().get(JSBodyImpl.class.getName());
        boolean isStatic = annot.getValue("isStatic").getBoolean();
        List<AnnotationValue> paramNames = annot.getValue("params").getList();

        int bodyParamCount = isStatic ? method.parameterCount() : method.parameterCount() - 1;

        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        IRFactory factory = new IRFactory(env, new TeaVMErrorReporter(context.getDiagnostics(),
                new CallLocation(methodRef)));
        String script = annot.getValue("script").getString();
        AstRoot rootNode;
        try {
            rootNode = factory.parse(new StringReader(script), null, 0);
        } catch (IOException e) {
            context.getDiagnostics().error(new CallLocation(methodRef), "IO error parsing JSBody script");
            return;
        }

        rootNode.visit(new NodeVisitor() {
            @Override
            public boolean visit(AstNode node) {
                return false;
            }
        });
    }
}
