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
package org.teavm.html4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.*;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.RenderingContext;
import org.teavm.model.CallLocation;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.AbstractRendererListener;

/**
 *
 * @author Alexey Andreev
 */
public class EntryPointGenerator extends AbstractRendererListener implements DependencyListener {
    private List<String> classesToLoad = new ArrayList<>();
    private SourceWriter writer;

    public EntryPointGenerator(String classesToLoad) {
        for (String className : classesToLoad.split(",| |;")) {
            className = className.trim();
            if (!className.isEmpty()) {
                this.classesToLoad.add(className);
            }
        }
    }

    @Override
    public void begin(RenderingContext context, BuildTarget buildTarget) throws IOException {
        writer = context.getWriter();
    }

    @Override
    public void complete() throws IOException {
        if (classesToLoad.isEmpty()) {
            return;
        }
        writer.append("function VM() {").softNewLine();
        writer.append("}").newLine();
        writer.append("VM.prototype.loadClass = function(className) {").softNewLine().indent();
        writer.append("switch (className) {").indent().softNewLine();
        for (String className : classesToLoad) {
            writer.append("case \"").append(Renderer.escapeString(className)).append("\": ");
            writer.appendClass(className).append(".$clinit(); break;").softNewLine();
        }
        writer.append("default: throw \"Can't load class \" + className;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.outdent().append("}").newLine();
        writer.append("function bck2brwsr() { return new VM(); }").newLine();
    }

    @Override
    public void started(DependencyAgent agent) {
        for (String className : classesToLoad) {
            agent.linkClass(className, null).initClass(null);
        }
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
    }

    @Override
    public void fieldReached(DependencyAgent agent, FieldDependency field,  CallLocation location) {
    }
}
