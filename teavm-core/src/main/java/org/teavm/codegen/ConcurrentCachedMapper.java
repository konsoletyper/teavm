package org.teavm.codegen;

import java.util.Collection;
import java.util.HashSet;
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

    private static class Wrapper<S> {
        volatile S value;
        volatile CountDownLatch latch = new CountDownLatch(1);
    }

    public ConcurrentCachedMapper(Mapper<T, R> innerMapper) {
        this.innerMapper = innerMapper;
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
            } else {
                CountDownLatch latch = oldWrapper.latch;
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
}
