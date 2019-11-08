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

public class TSpecializedConcatStream<T> extends TSimpleStreamImpl<T> {
    TSimpleStreamImpl<T> first;
    TSimpleStreamImpl<T> second;
    TSimpleStreamImpl<T> current;

    public TSpecializedConcatStream(TSimpleStreamImpl<T> first, TSimpleStreamImpl<T> second) {
        this.first = first;
        this.second = second;
        current = first;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (current == null) {
            return false;
        }
        if (current.next(consumer)) {
            return true;
        }
        if (current == first) {
            current = second;
            return true;
        } else {
            current = null;
            return false;
        }
    }

    @Override
    protected int estimateSize() {
        int firstSize = first.estimateSize();
        int secondSize = second.estimateSize();
        return firstSize >= 0 && secondSize >= 0 ? firstSize + secondSize : -1;
    }

    @Override
    public long count() {
        return first.count() + second.count();
    }

    @Override
    public void close() throws Exception {
        RuntimeException suppressed = null;
        try {
            first.close();
        } catch (RuntimeException e) {
            suppressed = e;
        }
        try {
            second.close();
        } catch (RuntimeException e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }
}
