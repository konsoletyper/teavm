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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ConcurrentCachedMapper<T, R> implements Mapper<T, R> {
    private Mapper<T, R> innerMapper;
    private ConcurrentMap<T, Wrapper<R>> cache = new ConcurrentHashMap<>();
    private List<KeyListener<T>> keyListeners = new ArrayList<>();

    private static class Wrapper<S> {
        volatile S value;
        volatile CountDownLatch latch = new CountDownLatch(1);
    }

    public ConcurrentCachedMapper(Mapper<T, R> innerMapper) {
        this.innerMapper = innerMapper;
    }

    public R getKnown(T preimage) {
        Wrapper<R> wrapper = cache.get(preimage);
        return wrapper != null ? wrapper.value : null;
    }

    @Override
    public R map(T preimage) {
        Wrapper<R> wrapper = cache.get(preimage);
        if (wrapper == null) {
            wrapper = new Wrapper<>();
            Wrapper<R> oldWrapper = cache.putIfAbsent(preimage, wrapper);
            if (oldWrapper == null) {
                wrapper.value = innerMapper.map(preimage);
                wrapper.latch.countDown();
                wrapper.latch = null;
                for (KeyListener<T> listener : keyListeners) {
                    listener.keyAdded(preimage);
                }
            } else {
                wrapper = oldWrapper;
            }
        }
        CountDownLatch latch = wrapper.latch;
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return wrapper.value;
    }

    public boolean caches(T preimage) {
        return cache.get(preimage) != null;
    }

    public Collection<T> getCachedPreimages() {
        return new HashSet<>(cache.keySet());
    }

    public void addKeyListener(KeyListener<T> listener) {
        keyListeners.add(listener);
    }

    public interface KeyListener<S> {
        void keyAdded(S key);
    }
}
