/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.java.util.stream.impl;

import java.util.function.Predicate;

public class TSingleStreamImpl<T> extends TSimpleStreamImpl<T> {
    private T element;

    public TSingleStreamImpl(T element) {
        this.element = element;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        consumer.test(element);
        return false;
    }

    @Override
    protected int estimateSize() {
        return 1;
    }

    @Override
    public long count() {
        return 1;
    }
}
