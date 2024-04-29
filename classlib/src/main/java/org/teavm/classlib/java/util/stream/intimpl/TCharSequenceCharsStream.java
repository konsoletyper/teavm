/*
 *  Copyright 2023 JFronny.
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
package org.teavm.classlib.java.util.stream.intimpl;

import java.util.function.IntPredicate;
import org.teavm.classlib.java.lang.TCharSequence;

public class TCharSequenceCharsStream extends TSimpleIntStreamImpl {
    private final TCharSequence csq;
    private int index;

    public TCharSequenceCharsStream(TCharSequence csq) {
        this.csq = csq;
    }

    @Override
    public boolean next(IntPredicate consumer) {
        while (index < csq.length()) {
            if (!consumer.test(csq.charAt(index++))) {
                break;
            }
        }
        return index < csq.length();
    }

    @Override
    protected int estimateSize() {
        return csq.length();
    }

    @Override
    public long count() {
        return csq.length();
    }
}
