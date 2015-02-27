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
import java.util.Map;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.RenderingContext;
import org.teavm.jso.plugin.JSODependencyListener.ExposedClass;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.RendererListener;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class JSOAliasRenderer implements RendererListener {
    private JSODependencyListener dependencyListener;
    private SourceWriter writer;

    public JSOAliasRenderer(JSODependencyListener dependencyListener) {
        this.dependencyListener = dependencyListener;
    }

    @Override
    public void begin(RenderingContext context, BuildTarget buildTarget) throws IOException {
        writer = context.getWriter();
    }

    @Override
    public void complete() throws IOException {
        if (!dependencyListener.isAnyAliasExists()) {
            return;
        }

        writer.append("(function()").ws().append("{").softNewLine().indent();
        writer.append("var c;").softNewLine();
        for (Map.Entry<String, ExposedClass> entry : dependencyListener.getExposedClasses().entrySet()) {
            if (entry.getValue().methods.isEmpty()) {
                continue;
            }
            writer.append("c").ws().append("=").ws().appendClass(entry.getKey()).append(".prototype;").softNewLine();
            for (Map.Entry<MethodDescriptor, String> aliasEntry : entry.getValue().methods.entrySet()) {
                writer.append("c.").append(aliasEntry.getValue()).ws().append("=").ws().append("c.")
                        .appendMethod(new MethodReference(entry.getKey(), aliasEntry.getKey()))
                        .append(";").softNewLine();
            }
        }
        writer.outdent().append("})();").newLine();
    }
}
