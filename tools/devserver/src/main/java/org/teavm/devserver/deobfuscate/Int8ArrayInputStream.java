/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.devserver.deobfuscate;

import java.io.InputStream;
import org.teavm.jso.typedarrays.Int8Array;

public class Int8ArrayInputStream extends InputStream {
    private Int8Array array;
    private int pos;

    public Int8ArrayInputStream(Int8Array array) {
        this.array = array;
    }

    @Override
    public int read() {
        if (pos >= array.getLength()) {
            return -1;
        }
        return array.get(pos++) & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (pos >= array.getLength()) {
            return -1;
        }
        int count = Math.min(len, array.getLength() - pos);
        for (int i = 0; i < count; ++i) {
            b[off++] = array.get(pos++);
        }
        return count;
    }
}
