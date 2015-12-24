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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.*;
import org.teavm.platform.PlatformClass;

/**
 *
 * @author Alexey Andreev
 * @param <S>
 */
public final class TServiceLoader<S> extends TObject implements TIterable<S> {
    private Object[] services;

    private TServiceLoader(Object[] services) {
        this.services = services;
    }

    @Override
    public TIterator<S> iterator() {
        return new TIterator<S>() {
            private int index;
            @Override public boolean hasNext() {
                return index < services.length;
            }
            @SuppressWarnings("unchecked") @Override public S next() {
                if (index == services.length) {
                    throw new TNoSuchElementException();
                }
                return (S) services[index++];
            }
            @Override public void remove() {
                throw new TUnsupportedOperationException();
            }
        };
    }

    public static <S> TServiceLoader<S> load(TClass<S> service) {
        return new TServiceLoader<>(loadServices(service.getPlatformClass()));
    }

    public static <S> TServiceLoader<S> load(TClass<S> service, @SuppressWarnings("unused") TClassLoader loader) {
        return load(service);
    }

    public static <S> TServiceLoader<S> loadInstalled(TClass<S> service) {
        return load(service);
    }

    private static native <T> T[] loadServices(PlatformClass cls);

    public void reload() {
        // Do nothing, services are bound at build time
    }
}
