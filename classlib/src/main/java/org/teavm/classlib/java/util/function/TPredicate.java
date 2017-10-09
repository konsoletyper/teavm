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
package org.teavm.classlib.java.util.function;

import org.teavm.classlib.java.util.TObjects;

@FunctionalInterface
public interface TPredicate<T> {
    boolean test(T t);

    default TPredicate<T> and(TPredicate<? super T> other) {
        return t -> test(t) && other.test(t);
    }

    default TPredicate<T> negate() {
        return t -> !test(t);
    }

    default TPredicate<T> or(TPredicate<? super T> other) {
        return t -> test(t) || other.test(t);
    }

    default TPredicate<T> isEqual(Object targetRef) {
        return t -> TObjects.equals(t, targetRef);
    }
}
