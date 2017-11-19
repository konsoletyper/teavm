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

import java.util.Iterator;
import java.util.function.Predicate;
import org.teavm.classlib.java.util.stream.TStream;

public class TGenericConcatStream<T> extends TSimpleStreamImpl<T> {
    TStream<? extends T> first;
    TStream<? extends T> second;
    Iterator<? extends T> iterator;
    boolean isSecond;

    public TGenericConcatStream(TStream<? extends T> first, TStream<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (iterator == null) {
            iterator = first.iterator();
        }
        while (true) {
            while (iterator.hasNext()) {
                if (!consumer.test(iterator.next())) {
                    return true;
                }
            }
            if (!isSecond) {
                isSecond = true;
                iterator = second.iterator();
            } else {
                return false;
            }
        }
    }

    @Override
    public long count() {
        return first.count() + second.count();
    }

    @Override
    public void close() throws Exception {
        Exception suppressed = null;
        try {
            first.close();
        } catch (Exception e) {
            suppressed = e;
        }
        try {
            second.close();
        } catch (Exception e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }
}
