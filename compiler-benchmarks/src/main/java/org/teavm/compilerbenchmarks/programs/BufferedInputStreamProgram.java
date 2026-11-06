/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compilerbenchmarks.programs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Program that is compiled by benchmarks, inspired by {@code BufferedInputStreamTest#test_read}.
 */
public final class BufferedInputStreamProgram {
    private BufferedInputStreamProgram() {
    }

    public static void main(String[] args) throws IOException {
        var isFile = new ByteArrayInputStream(new byte[10000]);
        var is = new BufferedInputStream(isFile, 5);
        var isr = new InputStreamReader(is);
        int c = isr.read();
        check(c == 0, "read returned incorrect char");

        var bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes), 12);
        check(in.read() == 0, "Wrong initial byte");
        var buf = new byte[14];
        in.read(buf, 0, 14);
        check(new String(buf, 0, 14).equals(new String(bytes, 1, 14)), "Wrong block read data");
        check(in.read() == 15, "Wrong bytes");
        System.out.println("OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
