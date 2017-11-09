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

import com.jcraft.jzlib.CRC32;

public class TCRC32 implements TChecksum {
    private CRC32 impl = new CRC32();
    long tbytes;

    @Override
    public long getValue() {
        return impl.getValue();
    }

    @Override
    public void reset() {
        impl.reset();
        tbytes = 0;
    }

    @Override
    public void update(int val) {
        impl.update(new byte[] { (byte) val }, 0, 1);
    }

    public void update(byte[] buf) {
        update(buf, 0, buf.length);
    }

    @Override
    public void update(byte[] buf, int off, int nbytes) {
        // avoid int overflow, check null buf
        if (off <= buf.length && nbytes >= 0 && off >= 0 && buf.length - off >= nbytes) {
            impl.update(buf, off, nbytes);
            tbytes += nbytes;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
