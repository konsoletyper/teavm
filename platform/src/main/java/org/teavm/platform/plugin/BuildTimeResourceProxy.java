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
package org.teavm.platform.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

class BuildTimeResourceProxy implements InvocationHandler {
    private Map<Method, BuildTimeResourceMethod> methods;
    Object[] data;

    BuildTimeResourceProxy(Map<Method, BuildTimeResourceMethod> methods, Object[] initialData) {
        this.methods = methods;
        data = Arrays.copyOf(initialData, initialData.length);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return methods.get(method).invoke(this, args);
    }
}
