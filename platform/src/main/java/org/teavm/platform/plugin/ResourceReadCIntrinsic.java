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
package org.teavm.platform.plugin;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.Resource;

public class ResourceReadCIntrinsic implements Intrinsic {
    private ClassReaderSource classSource;

    public ResourceReadCIntrinsic(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        return classSource.isSuperType(Resource.class.getTypeName(), method.getClassName()).orElse(false);
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        String name = invocation.getMethod().getName();
        if (name.startsWith("get")) {
            name = name.substring(3);
        } else if (name.startsWith("is")) {
            name = name.substring(2);
        } else {
            throw new IllegalArgumentException();
        }

        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        context.writer().print("FIELD(");
        context.emit(invocation.getArguments().get(0));
        context.writer().print(", ").print(context.names().forClass(invocation.getMethod().getClassName()));
        context.writer().print(", ").print(name).print(")");
    }
}
