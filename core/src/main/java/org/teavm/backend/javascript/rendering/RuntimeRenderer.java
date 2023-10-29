/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.templating.TemplatingAstTransformer;
import org.teavm.backend.javascript.templating.TemplatingAstWriter;
import org.teavm.model.ClassReaderSource;
import org.teavm.vm.RenderingException;

public class RuntimeRenderer {
    private final ClassReaderSource classSource;
    private final SourceWriter writer;

    public RuntimeRenderer(ClassReaderSource classSource, SourceWriter writer) {
        this.classSource = classSource;
        this.writer = writer;
    }

    public void renderRuntime() throws RenderingException {
        try {
            renderHandWrittenRuntime("runtime.js");
            renderHandWrittenRuntime("intern.js");
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderHandWrittenRuntime(String name) throws IOException {
        AstRoot ast = parseRuntime(name);
        ast.visit(new StringConstantElimination());
        new TemplatingAstTransformer(classSource).visit(ast);
        var astWriter = new TemplatingAstWriter(writer, null, null);
        astWriter.hoist(ast);
        astWriter.print(ast);
    }

    private AstRoot parseRuntime(String name) throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        JSParser factory = new JSParser(env);

        ClassLoader loader = RuntimeRenderer.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream("org/teavm/backend/javascript/" + name);
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return factory.parse(reader, null, 0);
        }
    }
}
