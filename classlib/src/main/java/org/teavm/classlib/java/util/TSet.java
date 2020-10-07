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

import java.util.Objects;

public interface TSet<E> extends TCollection<E> {

    static <E> TSet<E> of() {
        return TCollections.emptySet();
    }

    static <E> TSet<E> of(E e) {
        Objects.requireNonNull(e);
        return new TTemplateCollections.SingleElementSet<>(e);
    }

    static <E> TSet<E> of(E e1, E e2) {
        Objects.requireNonNull(e1);
        Objects.requireNonNull(e2);
        if (e1.equals(e2)) {
            throw new IllegalArgumentException();
        }
        return new TTemplateCollections.TwoElementsSet<>(e1, e2);
    }

    static <E> TSet<E> of(E e1, E e2, E e3) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5, e6);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5, e6, e7);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5, e6, e7, e8);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    static <E> TSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return new TTemplateCollections.NElementSet<>(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    @SafeVarargs
    static <E> TSet<E> of(E... elements) {
        return new TTemplateCollections.NElementSet<>(elements);
    }
}
