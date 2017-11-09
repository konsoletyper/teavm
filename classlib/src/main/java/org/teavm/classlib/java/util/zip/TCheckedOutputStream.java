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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TCheckedOutputStream extends FilterOutputStream {
    private final TChecksum check;

    public TCheckedOutputStream(OutputStream os, TChecksum cs) {
        super(os);
        check = cs;
    }

    public TChecksum getChecksum() {
        return check;
    }

    @Override
    public void write(int val) throws IOException {
        out.write(val);
        check.update(val);
    }

    @Override
    public void write(byte[] buf, int off, int nbytes) throws IOException {
        out.write(buf, off, nbytes);
        check.update(buf, off, nbytes);
    }
}
