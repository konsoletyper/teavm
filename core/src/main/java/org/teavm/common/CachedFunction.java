/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CachedFunction<T, R> implements Function<T, R> {
    private Function<T, R> innerFunction;
    private Map<T, Wrapper<R>> cache = new LinkedHashMap<>();
    private List<KeyListener<T>> keyListeners = new ArrayList<>();

    private static class Wrapper<S> {
        S value;
        boolean computed;
    }

    public CachedFunction(Function<T, R> innerFunction) {
        this.innerFunction = innerFunction;
    }

    public R getKnown(T preimage) {
        Wrapper<R> wrapper = cache.get(preimage);
        return wrapper != null ? wrapper.value : null;
    }

    @Override
    public R apply(T t) {
        Wrapper<R> wrapper = cache.get(t);
        if (wrapper == null) {
            wrapper = new Wrapper<>();
            cache.put(t, wrapper);
            wrapper.value = innerFunction.apply(t);
            wrapper.computed = true;
            for (KeyListener<T> listener : keyListeners) {
                listener.keyAdded(t);
            }
        }
        if (!wrapper.computed) {
            throw new IllegalStateException("Recursive calls are not allowed");
        }
        return wrapper.value;
    }

    public void replace(T preimage, R value) {
        cache.get(preimage).value = value;
    }

    public void invalidate(T preimage) {
        cache.remove(preimage);
    }

    public boolean caches(T preimage) {
        return cache.get(preimage) != null;
    }

    public Collection<T> getCachedPreimages() {
        return new LinkedHashSet<>(cache.keySet());
    }

    public void addKeyListener(KeyListener<T> listener) {
        keyListeners.add(listener);
    }

    public interface KeyListener<S> {
        void keyAdded(S key);
    }
}
