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
package org.teavm.vm.intrinsic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.interop.Intrinsified;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReference;

public class DefaultIntrinsicRegistry<I> implements IntrinsicRegistry<I>, IntrinsicProvider<I> {
    private Map<MethodReference, Holder<I>> intrinsics = new HashMap<>();
    private Map<String, ClassScopedIntrinsic<I>> intrinsicsByClass = new HashMap<>();
    private List<IntrinsicProvider<I>> providedIntrinsics = new ArrayList<>();
    private ClassReaderSource classes;

    public DefaultIntrinsicRegistry(ClassReaderSource classes) {
        this.classes = classes;
    }

    @Override
    public void registerIntrinsic(MethodReference method, I intrinsic) {
        intrinsics.put(method, new Holder<>(intrinsic));
    }

    @Override
    public void registerIntrinsic(String className, I intrinsic, String... methods) {
        intrinsicsByClass.put(className, new ClassScopedIntrinsic<>(intrinsic, Set.of(methods)));
    }

    @Override
    public void registerIntrinsic(IntrinsicProvider<I> provider) {
        providedIntrinsics.add(provider);
    }

    @Override
    public I getIntrinsic(MethodReference method) {
        var holder = intrinsics.computeIfAbsent(method, m -> new Holder<>(resolveIntrinsic(m)));
        return holder.value;
    }

    private I resolveIntrinsic(MethodReference method) {
        var classIntrinsic = intrinsicsByClass.get(method.getClassName());
        if (classIntrinsic != null) {
            var resolvedMethod = classes.resolve(method);
            if (resolvedMethod != null && resolvedMethod.hasModifier(ElementModifier.NATIVE)) {
                if (resolvedMethod.getAnnotations().get(Intrinsified.class.getName()) != null
                        || classIntrinsic.methodNames.contains(resolvedMethod.getName())) {
                    return classIntrinsic.intrinsic;
                }
            }
        }
        for (var provider : providedIntrinsics) {
            var intrinsic = provider.getIntrinsic(method);
            if (intrinsic != null) {
                return intrinsic;
            }
        }
        return null;
    }
    
    private static class Holder<T> {
        T value;

        Holder(T value) {
            this.value = value;
        }
    }

    private static class ClassScopedIntrinsic<I> {
        I intrinsic;
        Set<String> methodNames;

        ClassScopedIntrinsic(I intrinsic, Set<String> methodNames) {
            this.intrinsic = intrinsic;
            this.methodNames = methodNames;
        }
    }
}
