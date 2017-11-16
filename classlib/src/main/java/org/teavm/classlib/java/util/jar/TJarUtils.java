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
package org.teavm.classlib.java.util.jar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

final class TJarUtils {
    private TJarUtils() {
    }

    static boolean asciiEqualsIgnoreCase(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; ++i) {
            if (Character.toLowerCase((char) a[i]) != Character.toLowerCase((char) b[i])) {
                return false;
            }
        }

        return true;
    }

    static byte[] readFullyAndClose(InputStream input) throws IOException {
        ByteBuffer result = ByteBuffer.wrap(new byte[Math.min(512, input.available())]);
        while (true) {
            if (result.remaining() == 0) {
                result = ByteBuffer.wrap(Arrays.copyOf(result.array(), result.capacity() * 2));
            }
            int actuallyRead = input.read(result.array(), result.position(), result.remaining());
            if (actuallyRead == -1) {
                break;
            }
            result.position(result.position() + actuallyRead);
        }

        byte[] b = Arrays.copyOf(result.array(), result.position());
        input.close();
        return b;
    }
}
