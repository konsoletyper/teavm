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
package org.teavm.model.lowlevel;

import java.util.HashMap;
import java.util.Map;
import org.teavm.interop.Unmanaged;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class ManagedMethodRepository {
    private ClassReaderSource classSource;
    private Map<MethodReference, Boolean> cache = new HashMap<>();

    public ManagedMethodRepository(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public boolean isManaged(MethodReference methodReference) {
        return cache.computeIfAbsent(methodReference, this::computeIsManaged);
    }

    private boolean computeIsManaged(MethodReference methodReference) {
        MethodReader method = classSource.resolve(methodReference);
        if (method == null) {
            return true;
        }

        ClassReader cls = classSource.get(method.getOwnerName());
        if (cls.getAnnotations().get(Unmanaged.class.getName()) != null) {
            return false;
        }
        return method == null || method.getAnnotations().get(Unmanaged.class.getName()) == null;
    }
}
