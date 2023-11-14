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
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriterSink;
import org.teavm.backend.javascript.templating.AstRemoval;
import org.teavm.backend.javascript.templating.LetJoiner;
import org.teavm.backend.javascript.templating.RemovablePartsFinder;
import org.teavm.backend.javascript.templating.TemplatingAstTransformer;
import org.teavm.backend.javascript.templating.TemplatingAstWriter;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.vm.RenderingException;

public class RuntimeRenderer {
    private final List<AstRoot> runtimeAstParts = new ArrayList<>();
    private final List<AstRoot> epilogueAstParts = new ArrayList<>();
    private final RemovablePartsFinder removablePartsFinder = new RemovablePartsFinder();
    private final ClassReaderSource classSource;
    private final SourceWriter writer;
    private final ClassInitializerInfo classInitializerInfo;

    public RuntimeRenderer(ClassReaderSource classSource, SourceWriter writer,
            ClassInitializerInfo classInitializerInfo) {
        this.classSource = classSource;
        this.writer = writer;
        this.classInitializerInfo = classInitializerInfo;
    }

    public void prepareAstParts(boolean threadLibraryUsed) {
        runtimeAstParts.add(prepareAstPart("runtime.js"));
        runtimeAstParts.add(prepareAstPart("primitive.js"));
        runtimeAstParts.add(prepareAstPart("numeric.js"));
        runtimeAstParts.add(prepareAstPart("long.js"));
        runtimeAstParts.add(prepareAstPart("array.js"));
        runtimeAstParts.add(prepareAstPart("string.js"));
        runtimeAstParts.add(prepareAstPart("reflection.js"));
        runtimeAstParts.add(prepareAstPart("exception.js"));
        runtimeAstParts.add(prepareAstPart("check.js"));
        runtimeAstParts.add(prepareAstPart("console.js"));
        runtimeAstParts.add(prepareAstPart("metadata.js"));
        runtimeAstParts.add(prepareAstPart(threadLibraryUsed ? "thread.js" : "simpleThread.js"));
        epilogueAstParts.add(prepareAstPart("types.js"));
    }

    public void renderRuntime() {
        for (var ast : runtimeAstParts) {
            renderHandWrittenRuntime(ast);
        }
    }

    public void renderEpilogue() {
        for (var ast : epilogueAstParts) {
            renderHandWrittenRuntime(ast);
        }
    }

    private AstRoot prepareAstPart(String name) {
        var ast = parseRuntime(name);
        ast.visit(new StringConstantElimination());
        new TemplatingAstTransformer(classSource).visit(ast);
        removablePartsFinder.visit(ast);
        return ast;
    }

    private void renderHandWrittenRuntime(AstRoot ast)  {
        var astWriter = new TemplatingAstWriter(writer, null, null, classInitializerInfo);
        astWriter.hoist(ast);
        astWriter.print(ast);
    }

    private AstRoot parseRuntime(String name) {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        JSParser factory = new JSParser(env);

        ClassLoader loader = RuntimeRenderer.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream("org/teavm/backend/javascript/" + name);
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return factory.parse(reader, null, 0);
        } catch (IOException e) {
            throw new RenderingException(e);
        }
    }

    public final SourceWriterSink sink = new SourceWriterSink() {
        @Override
        public SourceWriterSink appendFunction(String name) {
            removablePartsFinder.markUsedDeclaration(name);
            return this;
        }
    };

    public void removeUnusedParts() {
        var removal = new AstRemoval(removablePartsFinder.getAllRemovableParts());
        var letJoiner = new LetJoiner();
        for (var part : runtimeAstParts) {
            removal.visit(part);
            letJoiner.visit(part);
        }
        for (var part : epilogueAstParts) {
            removal.visit(part);
            letJoiner.visit(part);
        }
    }
}
