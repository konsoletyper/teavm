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
package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class RandomAccessFileTest {
    @Test
    public void readByte() throws IOException {
        var file = new File("test-randomAccess.bin");
        try {
            try (var writer = new FileOutputStream(file)) {
                writer.write(new byte[] { 0, 1, (byte) 254, (byte) 255 });
            }
            try (var raf = new RandomAccessFile(file, "r")) {
                assertEquals(0, raf.read());
                assertEquals(1, raf.read());
                assertEquals(254, raf.read());
                assertEquals(255, raf.read());
                assertEquals(-1, raf.read());
            }
        } finally {
            file.delete();
        }
    }
}
