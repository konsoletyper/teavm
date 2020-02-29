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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@SuppressWarnings("resource")
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class BufferedInputStreamTest {
    @Test
    public void test_ConstructorLjava_io_InputStream() {
        try {
            BufferedInputStream str = new BufferedInputStream(null);
            str.read();
            fail("Expected an IOException");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void test_available() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[]{2, 3, 5, 7, 11});
        BufferedInputStream is = new BufferedInputStream(isFile);
        assertTrue("Returned incorrect number of available bytes", is.available() == 5);

        // Test that a closed stream throws an IOE for available()
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(new byte[]{'h', 'e', 'l', 'l',
                'o', ' ', 't', 'i', 'm'}));
        int available = bis.available();
        bis.close();
        assertTrue(available != 0);

        try {
            bis.available();
            fail("Expected test to throw IOE.");
        } catch (IOException ex) {
            // expected
        }
    }

    @Test
    public void test_close() throws IOException {
        new BufferedInputStream(new ByteArrayInputStream(new byte[100])).close();

        // regression for HARMONY-667
        BufferedInputStream buf = new BufferedInputStream(null, 5);
        buf.close();
    }

    @Test
    public void test_markI() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[10000]);
        BufferedInputStream is = new BufferedInputStream(isFile, 5);
        byte[] buf1 = new byte[100];
        byte[] buf2 = new byte[100];
        is.skip(3000);
        is.mark(1000);
        is.read(buf1, 0, buf1.length);
        is.reset();
        is.read(buf2, 0, buf2.length);
        is.reset();
        assertTrue("Failed to mark correct position",
                new String(buf1, 0, buf1.length).equals(new String(buf2, 0, buf2.length)));

        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes), 12);
        in.skip(6);
        in.mark(14);
        in.read(new byte[14], 0, 14);
        in.reset();
        assertTrue("Wrong bytes", in.read() == 6 && in.read() == 7);

        in = new BufferedInputStream(new ByteArrayInputStream(bytes), 12);
        in.skip(6);
        in.mark(8);
        in.skip(7);
        in.reset();
        assertTrue("Wrong bytes 2", in.read() == 6 && in.read() == 7);

        BufferedInputStream buf = new BufferedInputStream(new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4}), 2);
        buf.mark(3);
        bytes = new byte[3];
        int result = buf.read(bytes);
        assertEquals(3, result);
        assertEquals("Assert 0:", 0, bytes[0]);
        assertEquals("Assert 1:", 1, bytes[1]);
        assertEquals("Assert 2:", 2, bytes[2]);
        assertEquals("Assert 3:", 3, buf.read());

        buf = new BufferedInputStream(new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4}), 2);
        buf.mark(3);
        bytes = new byte[4];
        result = buf.read(bytes);
        assertEquals(4, result);
        assertEquals("Assert 4:", 0, bytes[0]);
        assertEquals("Assert 5:", 1, bytes[1]);
        assertEquals("Assert 6:", 2, bytes[2]);
        assertEquals("Assert 7:", 3, bytes[3]);
        assertEquals("Assert 8:", 4, buf.read());
        assertEquals("Assert 9:", -1, buf.read());

        buf = new BufferedInputStream(new ByteArrayInputStream(new byte[]{0, 1, 2, 3, 4}), 2);
        buf.mark(Integer.MAX_VALUE);
        buf.read();
        buf.close();
    }

    @Test
    public void test_read() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[10000]);
        BufferedInputStream is = new BufferedInputStream(isFile, 5);
        InputStreamReader isr = new InputStreamReader(is);
        int c = isr.read();
        assertTrue("read returned incorrect char", c == 0);

        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes), 12);
        assertEquals("Wrong initial byte", 0, in.read()); // Fill the
        // buffer
        byte[] buf = new byte[14];
        in.read(buf, 0, 14); // Read greater than the buffer
        assertTrue("Wrong block read data", new String(buf, 0, 14).equals(new String(bytes, 1, 14)));
        assertEquals("Wrong bytes", 15, in.read()); // Check next byte
    }

    @Test
    public void test_read$BII_Exception() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(null);

        try {
            bis.read(new byte[0], -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            bis.read(new byte[0], 1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            bis.read(new byte[0], 1, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        bis.close();

        try {
            bis.read(null, -1, -1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void test_read$BII() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[10000]);
        BufferedInputStream is = new BufferedInputStream(isFile);
        byte[] buf1 = new byte[100];
        is.skip(3000);
        is.mark(1000);
        is.read(buf1, 0, buf1.length);

        BufferedInputStream bufin = new BufferedInputStream(new InputStream() {
            int size = 2;
            int pos;

            byte[] contents = new byte[size];

            @Override
            public int read() throws IOException {
                if (pos >= size) {
                    throw new IOException("Read past end of data");
                }
                return contents[pos++];
            }

            @Override
            public int read(byte[] buf, int off, int len) throws IOException {
                if (pos >= size) {
                    throw new IOException("Read past end of data");
                }
                int toRead = len;
                if (toRead > available()) {
                    toRead = available();
                }
                System.arraycopy(contents, pos, buf, off, toRead);
                pos += toRead;
                return toRead;
            }

            @Override
            public int available() {
                return size - pos;
            }
        });
        bufin.read();
        int result = bufin.read(new byte[2], 0, 2);
        assertTrue("Incorrect result: " + result, result == 1);
    }

    @Test
    public void test_reset() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[10000]);
        BufferedInputStream is = new BufferedInputStream(isFile);
        byte[] buf1 = new byte[10];
        byte[] buf2 = new byte[10];
        is.mark(2000);
        is.read(buf1, 0, 10);
        is.reset();
        is.read(buf2, 0, 10);
        is.reset();
        assertTrue("Reset failed", new String(buf1, 0, buf1.length).equals(new String(buf2, 0, buf2.length)));

        BufferedInputStream bIn = new BufferedInputStream(new ByteArrayInputStream("1234567890".getBytes()));
        bIn.mark(10);
        for (int i = 0; i < 11; i++) {
            bIn.read();
        }
        bIn.reset();
    }

    @Test
    public void test_reset_Exception() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(null);

        // throws IOException with message "Mark has been invalidated"
        try {
            bis.reset();
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }

        // does not throw IOException
        bis.mark(1);
        bis.reset();

        bis.close();

        // throws IOException with message "stream is closed"
        try {
            bis.reset();
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void test_reset_scenario1() throws IOException {
        byte[] input = "12345678900".getBytes();
        BufferedInputStream buffis = new BufferedInputStream(new ByteArrayInputStream(input));
        buffis.read();
        buffis.mark(5);
        buffis.skip(5);
        buffis.reset();
    }

    @Test
    public void test_reset_scenario2() throws IOException {
        byte[] input = "12345678900".getBytes();
        BufferedInputStream buffis = new BufferedInputStream(new ByteArrayInputStream(input));
        buffis.mark(5);
        buffis.skip(6);
        buffis.reset();
    }

    @Test
    public void test_skipJ() throws IOException {
        ByteArrayInputStream isFile = new ByteArrayInputStream(new byte[10000]);
        BufferedInputStream is = new BufferedInputStream(isFile);

        byte[] buf1 = new byte[10];
        is.mark(2000);
        is.skip(1000);
        is.read(buf1, 0, buf1.length);
        is.reset();

        // regression for HARMONY-667
        try {
            BufferedInputStream buf = new BufferedInputStream(null, 5);
            buf.skip(10);
            fail("Should throw IOException");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void test_skip_NullInputStream() throws IOException {
        BufferedInputStream buf = new BufferedInputStream(null, 5);
        assertEquals(0, buf.skip(0));
    }
}
