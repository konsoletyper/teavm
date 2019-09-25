/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.generate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.c.generators.GeneratorContext;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;

class GeneratorContextImpl implements GeneratorContext {
    private GenerationContext context;
    private ClassGenerationContext classContext;
    private CodeWriter bodyWriter;
    private CodeWriter writerBefore;
    private CodeWriter writerAfter;
    private IncludeManager includes;
    private List<FileGeneratorImpl> fileGenerators = new ArrayList<>();

    public GeneratorContextImpl(ClassGenerationContext classContext, CodeWriter bodyWriter,
            CodeWriter writerBefore, CodeWriter writerAfter, IncludeManager includes) {
        this.context = classContext.getContext();
        this.classContext = classContext;
        this.bodyWriter = bodyWriter;
        this.writerBefore = writerBefore;
        this.writerAfter = writerAfter;
        this.includes = includes;
    }

    @Override
    public NameProvider names() {
        return context.getNames();
    }

    @Override
    public Diagnostics diagnotics() {
        return context.getDiagnostics();
    }

    @Override
    public ClassReaderSource classSource() {
        return context.getClassSource();
    }

    @Override
    public DependencyInfo dependencies() {
        return context.getDependencies();
    }

    @Override
    public String parameterName(int index) {
        return index == 0 ? "teavm_this_" : "teavm_local_" + index;
    }

    @Override
    public CodeWriter writer() {
        return bodyWriter;
    }

    @Override
    public StringPool stringPool() {
        return context.getStringPool();
    }

    @Override
    public CodeWriter writerBefore() {
        return writerBefore;
    }

    @Override
    public CodeWriter writerAfter() {
        return writerAfter;
    }

    @Override
    public IncludeManager includes() {
        return includes;
    }

    @Override
    public FileGenerator createSourceFile(String path) {
        return createFile(new BufferedCodeWriter(false), path);
    }

    @Override
    public void importMethod(MethodReference method, boolean isStatic) {
        classContext.importMethod(method, isStatic);
    }

    @Override
    public FileGenerator createHeaderFile(String path) {
        BufferedCodeWriter writer = new BufferedCodeWriter(false);
        writer.println("#pragma once");
        return createFile(writer, path);
    }

    private FileGenerator createFile(BufferedCodeWriter writer, String path) {
        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init(path);
        FileGeneratorImpl generator = new FileGeneratorImpl(path, writer, includes);
        fileGenerators.add(generator);
        return generator;
    }

    @Override
    public String escapeFileName(String name) {
        StringBuilder sb = new StringBuilder();
        ClassGenerator.escape(name, sb);
        return sb.toString();
    }

    void flush() throws IOException {
        for (FileGeneratorImpl generator : fileGenerators) {
            OutputFileUtil.write(generator.writer, generator.path, context.getBuildTarget());
        }
        fileGenerators.clear();
    }

    static class FileGeneratorImpl implements FileGenerator {
        String path;
        BufferedCodeWriter writer;
        private IncludeManager includes;

        FileGeneratorImpl(String path, BufferedCodeWriter writer, IncludeManager includes) {
            this.path = path;
            this.writer = writer;
            this.includes = includes;
        }

        @Override
        public CodeWriter writer() {
            return writer;
        }

        @Override
        public IncludeManager includes() {
            return includes;
        }
    }
}
