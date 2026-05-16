/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.metaprogramming.test;

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.MetaprogrammingProvider;
import org.teavm.metaprogramming.MethodGenerator;
import org.teavm.metaprogramming.MethodGeneratorContext;

@CompileTime
public class TestProvider implements MetaprogrammingProvider {
    @Override
    public MethodGenerator provide(IntrospectMethod method) {
        if (method.hasAnnotation(ArgWithType.class)) {
            return this::generateArgWithName;
        }
        return null;
    }

    private void generateArgWithName(MethodGeneratorContext context) {
        var result = emit(() -> new StringBuilder());
        for (var i = 0; i < context.method().parameters().size(); ++i) {
            var param = context.method().parameters().get(i).type().toString();
            var paramValue = context.parameters().get(i);
            emit(() -> result.get().append(param).append(": ").append(paramValue.get()).append("\n"));
        }
        exit(() -> result.get().toString());
    }
}
