/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.io;

import java.io.IOException;
import org.teavm.classlib.java.lang.TObject;

public abstract class TOutputStream extends TObject implements TCloseable, TFlushable {
    public static TOutputStream nullOutputStream() {
        return new TOutputStream() {
            private boolean isClosed;

            @Override
            public void write(int b) throws IOException {
                if (isClosed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (isClosed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public void close() throws IOException {
                isClosed = true;
            }
        };
    }

    public abstract void write(int b) throws IOException;

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            write(b[off++]);
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }
}
