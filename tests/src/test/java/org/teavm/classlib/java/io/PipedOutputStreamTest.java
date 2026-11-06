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

package org.teavm.classlib.java.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class PipedOutputStreamTest  {
    static class PReader implements Runnable {
        PipedInputStream reader;

        public PipedInputStream getReader() {
            return reader;
        }

        PReader(PipedOutputStream out) {
            try {
                reader = new PipedInputStream(out);
            } catch (Exception e) {
                System.out.println("Couldn't start reader");
            }
        }

        public int available() {
            try {
                return reader.available();
            } catch (Exception e) {
                return -1;
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(1000);
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        public String read(int nbytes) {
            byte[] buf = new byte[nbytes];
            try {
                reader.read(buf, 0, nbytes);
                return new String(buf, "UTF-8");
            } catch (IOException e) {
                System.out.println("Exception reading info");
                return "ERROR";
            }
        }
    }

    private Thread rt;
    private PReader reader;
    private PipedOutputStream out;

    @Test
    public void constructor() {
        // Used in tests
    }

    @Test
    public void constructorLjava_io_PipedInputStream() throws Exception {
        out = new PipedOutputStream(new PipedInputStream());
        out.write('b');
    }

    @Test
    public void close() throws Exception {
        out = new PipedOutputStream();
        reader = new PReader(out);
        rt = new Thread(reader);
        rt.start();
        out.close();
    }

    @Test
    public void connectLjava_io_PipedInputStream_Exception() throws IOException {
        out = new PipedOutputStream();
        out.connect(new PipedInputStream());
        try {
            out.connect(null);
            fail("should throw NullPointerException"); //$NON-NLS-1$
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void connectLjava_io_PipedInputStream() {
        try {
            out = new PipedOutputStream();
            reader = new PReader(out);
            rt = new Thread(reader);
            rt.start();
            out.connect(new PipedInputStream());
            fail("Failed to throw exception attempting connect on already connected stream");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void flush() throws IOException {
        out = new PipedOutputStream();
        reader = new PReader(out);
        rt = new Thread(reader);
        rt.start();
        out.write("HelloWorld".getBytes("UTF-8"), 0, 10);
        assertTrue(reader.available() != 0, "Bytes written before flush");
        out.flush();
        assertEquals("HelloWorld", reader.read(10), "Wrote incorrect bytes");
    }

    @Test
    public void write$BII() throws IOException {
        out = new PipedOutputStream();
        reader = new PReader(out);
        rt = new Thread(reader);
        rt.start();
        out.write("HelloWorld".getBytes("UTF-8"), 0, 10);
        out.flush();
        assertEquals("HelloWorld", reader.read(10), "Wrote incorrect bytes");
    }

    @Test
    public void test_writeI() throws IOException {
        out = new PipedOutputStream();
        reader = new PReader(out);
        rt = new Thread(reader);
        rt.start();
        out.write('c');
        out.flush();
        assertEquals("c", reader.read(1), "Wrote incorrect byte");
    }

    @AfterEach
    public void tearDown() {
        if (rt != null) {
            rt.interrupt();
        }
    }
}
