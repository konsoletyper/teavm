/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform;

import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.dependency.PluggableDependency;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.platform.plugin.PlatformQueueGenerator;

public abstract class PlatformQueue<T> implements JSObject {
    @JSProperty
    public abstract int getLength();

    public final boolean isEmpty() {
        return getLength() == 0;
    }

    abstract void push(PlatformObject obj);

    abstract PlatformObject shift();

    public final void add(T e) {
        push(wrap(e));
    }

    public final T remove() {
        return unwrap(shift());
    }

    private static PlatformObject wrap(Object obj) {
        return Platform.getPlatformObject(obj);
    }

    @InjectedBy(PlatformQueueGenerator.class)
    @PluggableDependency(PlatformQueueGenerator.class)
    private static native <S> S unwrap(PlatformObject obj);
}
