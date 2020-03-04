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
package org.teavm.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.teavm.model.optimization.GlobalValueNumbering;
import org.teavm.model.optimization.UnusedVariableElimination;
import org.teavm.model.transformation.NoSuchFieldCatchElimination;

public class PreOptimizingClassHolderSource implements ClassHolderSource {
    private ClassHolderSource innerClassSource;
    private Map<String, ClassHolder> cache = new LinkedHashMap<>();

    public PreOptimizingClassHolderSource(ClassHolderSource innerClassSource) {
        this.innerClassSource = innerClassSource;
    }

    @Override
    public ClassHolder get(String name) {
        ClassHolder cls = cache.get(name);
        if (cls == null) {
            cls = optimize(innerClassSource::get, name);
            if (cls == null) {
                return null;
            }

            cache.put(name, cls);
        }
        return cls;
    }

    public static ClassHolder optimize(Function<String, ClassHolder> innerSource, String name) {
        ClassHolder cls = innerSource.apply(name);
        if (cls == null) {
            return cls;
        }
        NoSuchFieldCatchElimination noSuchFieldCatchElimination = new NoSuchFieldCatchElimination();
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                noSuchFieldCatchElimination.apply(method.getProgram());
                new GlobalValueNumbering(true).optimize(method.getProgram());
                new UnusedVariableElimination().optimize(method, method.getProgram());
            }
        }
        return cls;
    }
}
