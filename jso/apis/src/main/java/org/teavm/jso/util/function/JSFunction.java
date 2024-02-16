/*
 *  Copyright 2023 Bernd Busse.
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
package org.teavm.jso.util.function;

import java.util.Objects;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@FunctionalInterface
@JSFunctor
public interface JSFunction<T, R> extends JSObject {
    R apply(T t);

    default <V> JSFunction<T, V> andThen(JSFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);

        return (T t) -> {
            R result = this.apply(t);
            return after.apply(result);
        };
    }

    default <V> JSFunction<V, R> compose(JSFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);

        return (V v) -> {
            T result = before.apply(v);
            return this.apply(result);
        };
    }

    static <T> JSFunction<T, T> identity() {
        return (T t) -> {
            return t;
        };
    }
}
