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

import java.util.function.LongPredicate;

public class TCloseHandlingLongStream extends TSimpleLongStreamImpl {
    private TSimpleLongStreamImpl innerStream;
    private Runnable closeHandler;

    public TCloseHandlingLongStream(TSimpleLongStreamImpl innerStream, Runnable closeHandler) {
        this.innerStream = innerStream;
        this.closeHandler = closeHandler;
    }

    @Override
    public boolean next(LongPredicate consumer) {
        return innerStream.next(consumer);
    }

    @Override
    public void close() {
        RuntimeException previousException = null;
        try {
            closeHandler.run();
        } catch (RuntimeException e) {
            previousException = e;
        }

        try {
            innerStream.close();
        } catch (RuntimeException e) {
            if (previousException != null) {
                e.addSuppressed(previousException);
            }
            throw e;
        }
    }
}
