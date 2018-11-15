/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import java.io.IOException;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.AbstractRendererListener;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

class TestExceptionPlugin implements TeaVMPlugin {
    static final MethodDescriptor GET_MESSAGE = new MethodDescriptor("getMessage", ValueType.parse(String.class));

    @Override
    public void install(TeaVMHost host) {
        host.add(new TestExceptionDependencyListener());

        TeaVMJavaScriptHost jsHost = host.getExtension(TeaVMJavaScriptHost.class);
        if (jsHost != null) {
            install(jsHost);
        }
    }

    private void install(TeaVMJavaScriptHost host) {
        host.addVirtualMethods((context, methodRef) -> {
            if (!methodRef.getDescriptor().equals(GET_MESSAGE)) {
                return false;
            }
            return context.getClassSource().isSuperType("java.lang.Throwable", methodRef.getClassName()).orElse(false);
        });

        host.add(new AbstractRendererListener() {
            RenderingManager manager;

            @Override
            public void begin(RenderingManager manager, BuildTarget buildTarget) throws IOException {
                this.manager = manager;
            }

            @Override
            public void complete() throws IOException {
                renderExceptionMessage(manager.getWriter());
            }
        });
    }

    private void renderExceptionMessage(SourceWriter writer) throws IOException {
        writer.appendClass("java.lang.Throwable").append(".prototype.getMessage").ws().append("=").ws()
                .append("function()").ws().append("{").indent().softNewLine();
        writer.append("return $rt_ustr(this.").appendMethod("getMessage", String.class).append("());")
                .softNewLine();
        writer.outdent().append("};").newLine();
    }
}
