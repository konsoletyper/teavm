/*
 *  Copyright 2014 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.util.TArrays;

public class TByteArrayOutputStream extends TOutputStream {
    protected byte[] buf;
    protected int count;

    public TByteArrayOutputStream() {
        this(32);
    }

    public TByteArrayOutputStream(int size) {
        buf = new byte[size];
    }

    @Override
    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count++] = (byte) b;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureCapacity(count + len);
        for (int i = 0; i < len; ++i) {
            buf[count++] = b[off++];
        }
    }

    private void ensureCapacity(int capacity) {
        if (buf.length < capacity) {
            capacity = TMath.max(capacity, buf.length * 3 / 2);
            buf = TArrays.copyOf(buf, capacity);
        }
    }

    public byte[] toByteArray() {
        return TArrays.copyOf(buf, count);
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    public TString toString(TString charsetName) throws TUnsupportedEncodingException {
        return new TString(buf, 0, count, charsetName);
    }

    public void writeTo(TOutputStream out) throws TIOException {
        out.write(buf, 0, count);
    }

    public void reset() {
        count = 0;
    }

    public int size() {
        return count;
    }
}
