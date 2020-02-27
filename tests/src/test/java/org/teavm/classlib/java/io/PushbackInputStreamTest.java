/*
 *  Copyright 2015 Alexey Andreev.
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@SuppressWarnings("resource")
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class PushbackInputStreamTest {
    PushbackInputStream pis;

    public PushbackInputStreamTest() {
        byte[] array = new byte[1000];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) i;
        }
        pis = new PushbackInputStream(new ByteArrayInputStream(array), array.length);
    }

    @Test
    public void test_reset() {
        PushbackInputStream pb = new PushbackInputStream(new ByteArrayInputStream(new byte[] { 0 }), 2);
        try {
            pb.reset();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void test_mark() {
        PushbackInputStream pb = new PushbackInputStream(new ByteArrayInputStream(new byte[] { 0 }), 2);
        pb.mark(Integer.MAX_VALUE);
        pb.mark(0);
        pb.mark(-1);
        pb.mark(Integer.MIN_VALUE);
    }

    @Test
    public void test_ConstructorLjava_io_InputStream() {
        try {
            PushbackInputStream str = new PushbackInputStream(null);
            str.read();
            fail("Expected IOException");
        } catch (IOException e) {
            // Expected
        }

        try {
            pis = new PushbackInputStream(new ByteArrayInputStream("Hello".getBytes()));
            pis.unread("He".getBytes());
            fail("Failed to throw exception on unread when buffer full");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void test_ConstructorLjava_io_InputStreamI() {
        try {
            pis = new PushbackInputStream(new ByteArrayInputStream("Hello".getBytes()), 5);
            pis.unread("Hellos".getBytes());
        } catch (IOException e) {
            // Correct
            // Pushback buffer should be full
            return;

        }
        fail("Failed to throw exception on unread when buffer full");
    }

    @Test
    public void test_ConstructorLjava_io_InputStreamL() {
        try {
            PushbackInputStream str = new PushbackInputStream(null, 1);
            str.read();
            fail("Expected IOException");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void test_available() {
        try {
            assertEquals("Available returned incorrect number of bytes", 1000, pis.available());
        } catch (IOException e) {
            fail("Exception during available test: " + e.toString());
        }
    }

    @Test
    public void test_markSupported() {
        assertTrue("markSupported returned true", !pis.markSupported());
    }

    @Test
    public void test_read() {
        try {
            assertEquals("Incorrect byte read", 0, pis.read());
        } catch (IOException e) {
            fail("Exception during read test : " + e.getMessage());
        }
    }

    @Test
    public void test_read$BII() {
        try {
            byte[] buf = new byte[100];
            pis.read(buf, 0, buf.length);
            assertEquals(0, buf[0]);
            assertEquals(99, buf[99]);
        } catch (IOException e) {
            fail("Exception during read test : " + e.getMessage());
        }
    }

    @Test
    public void test_skipJ() throws Exception {
        byte[] buf = new byte[50];
        pis.skip(50);
        pis.read(buf, 0, buf.length);
        assertEquals(50, buf[0]);
        assertEquals(99, buf[49]);
        pis.unread(buf);
        pis.skip(25);
        byte[] buf2 = new byte[25];
        pis.read(buf2, 0, buf2.length);
        assertEquals(75, buf2[0]);
        assertEquals(99, buf2[24]);
    }

    @Test
    public void test_unread$B() {
        try {
            byte[] buf = new byte[100];
            pis.read(buf, 0, buf.length);
            assertEquals(0, buf[0]);
            assertEquals(99, buf[99]);
            pis.unread(buf);
            pis.read(buf, 0, 50);
            assertEquals(0, buf[0]);
            assertEquals(49, buf[49]);
        } catch (IOException e) {
            fail("IOException during unread test : " + e.getMessage());
        }
    }

    @Test
    public void test_unread$BII() throws IOException {
        byte[] buf = new byte[100];
        pis.read(buf, 0, buf.length);
        assertEquals(0, buf[0]);
        assertEquals(99, buf[99]);
        pis.unread(buf, 50, 50);
        pis.read(buf, 0, 50);
        assertEquals(50, buf[0]);
        assertEquals(99, buf[49]);

        // Regression for HARMONY-49
        try {
            PushbackInputStream pb = new PushbackInputStream(new ByteArrayInputStream(new byte[] { 0 }), 2);
            pb.unread(new byte[1], 0, 5);
            fail("Assert 0: should throw IOE");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void test_unreadI() {
        try {
            int x = pis.read();
            assertEquals("Incorrect byte read", 0, x);
            pis.unread(x);
            assertEquals("Failed to unread", x, pis.read());
        } catch (IOException e) {
            fail("IOException during read test : " + e.getMessage());
        }
    }
}
