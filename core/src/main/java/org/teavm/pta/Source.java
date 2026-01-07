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
package org.teavm.pta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Source<T> {
    void sink(Consumer<Collection<? extends T>> consumer);

    static <T> Source<T> empty() {
        return consumer -> {};
    }

    @SafeVarargs
    static <T> Source<T> of(T... values) {
        return consumer -> consumer.accept(Arrays.asList(values));
    }

    default <S> Source<S> map(Function<T, S> f) {
        return consumer -> sink(c -> {
            var mapped = new ArrayList<S>(c.size());
            for (var elem : c) {
                mapped.add(f.apply(elem));
            }
            consumer.accept(mapped);
        });
    }

    default Source<T> filter(Predicate<T> f) {
        return consumer -> sink(c -> {
            var filtered = new ArrayList<T>(c.size());
            for (var elem : c) {
                if (f.test(elem)) {
                    filtered.add(elem);
                }
            }
            if (!filtered.isEmpty()) {
                consumer.accept(c);
            }
        });
    }

    default <S> Source<S> flatMap(Function<T, Source<S>> f) {
        return consumer -> sink(c -> {
            for (var elem : c) {
                f.apply(elem).sink(consumer);
            }
        });
    }

    default Source<?> any(Predicate<T> f) {
        return consumer -> sink(new Consumer<>() {
            boolean triggered;

            @Override
            public void accept(Collection<? extends T> ts) {
                if (!triggered) {
                    for (var elem : ts) {
                        if (f.test(ts.iterator().next())) {
                            triggered = true;
                            consumer.accept(null);
                        }
                    }
                }
            }
        });
    }
}
