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

import org.teavm.extension.introspect.IntrospectClassImpl;
import org.teavm.metaprogramming.ClassHandle;

public class ClassHandleImpl<T> implements ClassHandle<T> {
    public final IntrospectClassImpl<T> cls;

    public ClassHandleImpl(IntrospectClassImpl<T> cls) {
        this.cls = cls;
    }

    @Override
    public T getArrayElement(Object array, int index) {
        throw new UnsupportedOperationException("Can only be called from compile time context");
    }

    @Override
    public int getArrayLength(Object array) {
        throw new UnsupportedOperationException("Can only be called from compile time context");
    }

    @Override
    public boolean isInstance(Object obj) {
        throw new UnsupportedOperationException("Can only be called from compile time context");
    }

    @Override
    public T cast(Object obj) {
        throw new UnsupportedOperationException("Can only be called from compile time context");
    }

    @Override
    public T[] createArray(int size) {
        throw new UnsupportedOperationException("Can only be called from compile time context");
    }
}
