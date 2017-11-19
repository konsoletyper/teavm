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
package org.teavm.classlib.java.util.stream.longimpl;

import java.util.PrimitiveIterator;
import java.util.function.LongPredicate;
import org.teavm.classlib.java.util.stream.TLongStream;

public class TGenericConcatLongStream extends TSimpleLongStreamImpl {
    TLongStream first;
    TLongStream second;
    PrimitiveIterator.OfLong iterator;
    boolean isSecond;

    public TGenericConcatLongStream(TLongStream first, TLongStream second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean next(LongPredicate consumer) {
        if (iterator == null) {
            iterator = first.iterator();
        }
        while (true) {
            while (iterator.hasNext()) {
                if (!consumer.test(iterator.nextLong())) {
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
