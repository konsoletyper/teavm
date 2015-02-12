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
package org.teavm.classlib.impl;

import java.io.IOException;
import org.teavm.javascript.RenderingContext;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.spi.RendererListener;

/**
 *
 * @author Alexey Andreev
 */
public class ObjectEnrichRenderer implements RendererListener {
    private RenderingContext context;

    @Override
    public void begin(RenderingContext context, BuildTarget buildTarget) throws IOException {
        this.context = context;
    }

    @Override
    public void complete() throws IOException {
        ClassReader cls = context.getClassSource().get("java.lang.Object");
        MethodReader toString = cls.getMethod(new MethodDescriptor("toString", String.class));
        if (toString != null) {
            String clsName = context.getNaming().getNameFor(cls.getName());
            String toStringName = context.getNaming().getNameFor(toString.getReference());
            context.getWriter().append(clsName).append(".prototype.toString").ws().append('=').ws()
                    .append("function()").ws().append('{').indent().softNewLine();
            context.getWriter().append("return this.").append(toStringName).ws().append('?').ws()
                    .append("$rt_ustr(this.").append(toStringName).append("())").ws().append(':')
                    .append("Object.prototype.toString.call(this);").softNewLine();
            context.getWriter().outdent().append("}").newLine();
        }
    }
}
