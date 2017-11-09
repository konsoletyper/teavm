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

package org.teavm.classlib.java.util.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TCheckedInputStream extends FilterInputStream {
    private final TChecksum check;

    public TCheckedInputStream(InputStream is, TChecksum csum) {
        super(is);
        check = csum;
    }

    @Override
    public int read() throws IOException {
        int x = in.read();
        if (x != -1) {
            check.update(x);
        }
        return x;
    }

    @Override
    public int read(byte[] buf, int off, int nbytes) throws IOException {
        int x = in.read(buf, off, nbytes);
        if (x != -1) {
            check.update(buf, off, x);
        }
        return x;
    }

    public TChecksum getChecksum() {
        return check;
    }

    @Override
    public long skip(long nbytes) throws IOException {
        if (nbytes < 1) {
            return 0;
        }
        long skipped = 0;
        byte[] b = new byte[(int) Math.min(nbytes, 2048L)];
        int x;
        int v;
        while (skipped != nbytes) {
            v = (int) (nbytes - skipped);
            x = in.read(b, 0, v > b.length ? b.length : v);
            if (x == -1) {
                return skipped;
            }
            check.update(b, 0, x);
            skipped += x;
        }
        return skipped;
    }
}
