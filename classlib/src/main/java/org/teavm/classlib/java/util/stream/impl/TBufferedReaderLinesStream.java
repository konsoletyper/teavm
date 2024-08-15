/*
 *  Copyright 2024 Alexey Andreev.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Predicate;
import org.teavm.classlib.java.io.TBufferedReader;

public class TBufferedReaderLinesStream extends TSimpleStreamImpl<String> {
    private TBufferedReader reader;
    private boolean done;

    public TBufferedReaderLinesStream(TBufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean next(Predicate<? super String> consumer) {
        if (!done) {
            while (true) {
                try {
                    var line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (!consumer.test(line)) {
                        return true;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            done = true;
        }
        return false;
    }
}
