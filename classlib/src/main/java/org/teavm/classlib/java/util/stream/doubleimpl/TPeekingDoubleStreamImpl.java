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
package org.teavm.classlib.java.util.stream.doubleimpl;

import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;

public class TPeekingDoubleStreamImpl extends TWrappingDoubleStreamImpl {
    private DoubleConsumer elementConsumer;

    public TPeekingDoubleStreamImpl(TSimpleDoubleStreamImpl sourceStream, DoubleConsumer elementConsumer) {
        super(sourceStream);
        this.elementConsumer = elementConsumer;
    }

    @Override
    protected DoublePredicate wrap(DoublePredicate consumer) {
        return e -> {
            elementConsumer.accept(e);
            return consumer.test(e);
        };
    }
}
