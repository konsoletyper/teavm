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

import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.classlib.java.lang.TString;

public class TStringReader extends TReader {
    private TString string;
    private int index;
    private int mark;

    public TStringReader(TString string) {
        if (string == null) {
            throw new TNullPointerException();
        }
        this.string = string;
    }

    @Override
    public int read() throws TIOException {
        checkOpened();
        if (index >= string.length()) {
            return -1;
        }
        return string.charAt(index++);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws TIOException {
        checkOpened();
        if (index >= string.length()) {
            return -1;
        }
        int n = TMath.min(string.length() - index, len);
        for (int i = 0; i < n; ++i) {
            cbuf[off++] = string.charAt(index++);
        }
        return n;
    }

    @Override
    public long skip(long n) throws TIOException {
        checkOpened();
        if (n < 0) {
            n = TMath.max(n, -index);
        } else {
            n = TMath.min(string.length() - index, n);
        }
        index += n;
        return n;
    }

    @Override
    public boolean ready() throws TIOException {
        checkOpened();
        return true;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) throws TIOException {
        checkOpened();
        if (readAheadLimit < 0) {
            throw new TIllegalArgumentException();
        }
        mark = index;
    }

    @Override
    public void reset() throws TIOException {
        checkOpened();
        index = mark;
    }

    @Override
    public void close() {
        string = null;
    }

    private void checkOpened() throws TIOException {
        if (string == null) {
            throw new TIOException();
        }
    }
}
