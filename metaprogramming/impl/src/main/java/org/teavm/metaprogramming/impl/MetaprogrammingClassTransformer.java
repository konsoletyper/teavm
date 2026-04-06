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
package org.teavm.metaprogramming.impl;

import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.teavm.metaprogramming.MetaprogrammingProvider;
import org.teavm.metaprogramming.MethodGenerator;
import org.teavm.metaprogramming.impl.reflect.ReflectMethodImpl;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;

public class MetaprogrammingClassTransformer implements ClassHolderTransformer {
    private MetaprogrammingGeneratorContextImpl genContext = new MetaprogrammingGeneratorContextImpl();


    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (MetaprogrammingImpl.reflectContext == null) {
            return;
        }

        var providers = ServiceLoader.load(MetaprogrammingProvider.class, MetaprogrammingImpl.classLoader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList());
        if (providers.isEmpty()) {
            return;
        }

        var index = 0;
        for (var method : cls.getMethods()) {
            var currentIndex = index++;
            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }
            var reflectMethod = new ReflectMethodImpl(MetaprogrammingImpl.reflectContext.findClass(cls.getName()),
                    method);
            MethodGenerator generator = null;
            for (var provider : providers) {
                generator = provider.provide(reflectMethod);
                if (generator != null) {
                    break;
                }
            }
            if (generator != null) {
                genContext.init(reflectMethod, currentIndex);
                generator.generate(genContext);
                method.setProgram(MetaprogrammingImpl.generator.getProgram());
                method.getModifiers().remove(ElementModifier.NATIVE);
                genContext.cleanup();
            }
        }
    }
}
