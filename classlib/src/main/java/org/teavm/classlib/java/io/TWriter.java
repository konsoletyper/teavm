/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TAppendable;
import org.teavm.classlib.java.lang.TCharSequence;

public abstract class TWriter implements TAppendable, TCloseable, TFlushable {
    protected final Object lock;

    protected TWriter() {
        super();
        lock = this;
    }

    protected TWriter(Object lock) {
        if (lock == null) {
            throw new NullPointerException();
        }
        this.lock = lock;
    }

    public void write(char[] buf) throws TIOException {
        write(buf, 0, buf.length);
    }

    public abstract void write(char[] buf, int offset, int count) throws TIOException;

    public void write(int oneChar) throws TIOException {
        synchronized (lock) {
            char[] oneCharArray = new char[1];
            oneCharArray[0] = (char) oneChar;
            write(oneCharArray);
        }
    }

    public void write(String str) throws TIOException {
        write(str, 0, str.length());
    }

    public void write(String str, int offset, int count) throws TIOException {
        if (count < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        char[] buf = new char[count];
        str.getChars(offset, offset + count, buf, 0);
        synchronized (lock) {
            write(buf, 0, buf.length);
        }
    }

    @Override
    public TWriter append(char c) throws TIOException {
        write(c);
        return this;
    }

    @Override
    public TWriter append(TCharSequence csq) throws TIOException {
        write(csq != null ? csq.toString() : "null");
        return this;
    }

    @Override
    public TWriter append(TCharSequence csq, int start, int end) throws TIOException {
        write(csq != null ? csq.subSequence(start, end).toString() : "null");
        return this;
    }
}
