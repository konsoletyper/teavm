/*
 *  Copyright 2025 Alexey Andreev.
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
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class BufferedOutputStreamTest {
    public String fileString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_BufferedOutputStream\n"
            + "Test_java_io_ByteArrayInputStream\nTest_java_io_ByteArrayOutputStream\nTest_java_io_DataInputStream\n"
            + "Test_java_io_File\nTest_java_io_FileDescriptor\nTest_java_io_FileInputStream\n"
            + "Test_java_io_FileNotFoundException\nTest_java_io_FileOutputStream\nTest_java_io_FilterInputStream\n"
            + "Test_java_io_FilterOutputStream\nTest_java_io_InputStream\nTest_java_io_IOException\n"
            + "Test_java_io_OutputStream\nTest_java_io_PrintStream\nTest_java_io_RandomAccessFile\n"
            + "Test_java_io_SyncFailedException\nTest_java_lang_AbstractMethodError\n"
            + "Test_java_lang_ArithmeticException\nTest_java_lang_ArrayIndexOutOfBoundsException\n"
            + "Test_java_lang_ArrayStoreException\nTest_java_lang_Boolean\nTest_java_lang_Byte\n"
            + "Test_java_lang_Character\nTest_java_lang_Class\nTest_java_lang_ClassCastException\n"
            + "Test_java_lang_ClassCircularityError\nTest_java_lang_ClassFormatError\n"
            + "Test_java_lang_ClassLoader\nTest_java_lang_ClassNotFoundException\n"
            + "Test_java_lang_CloneNotSupportedException\nTest_java_lang_Double\n"
            + "Test_java_lang_Error\nTest_java_lang_Exception\nTest_java_lang_ExceptionInInitializerError\n"
            + "Test_java_lang_Float\nTest_java_lang_IllegalAccessError\nTest_java_lang_IllegalAccessException\n"
            + "Test_java_lang_IllegalArgumentException\nTest_java_lang_IllegalMonitorStateException\n"
            + "Test_java_lang_IllegalThreadStateException\nTest_java_lang_IncompatibleClassChangeError\n"
            + "Test_java_lang_IndexOutOfBoundsException\nTest_java_lang_InstantiationError\n"
            + "Test_java_lang_InstantiationException\nTest_java_lang_Integer\nTest_java_lang_InternalError\n"
            + "Test_java_lang_InterruptedException\nTest_java_lang_LinkageError\nTest_java_lang_Long\n"
            + "Test_java_lang_Math\nTest_java_lang_NegativeArraySizeException\nTest_java_lang_NoClassDefFoundError\n"
            + "Test_java_lang_NoSuchFieldError\nTest_java_lang_NoSuchMethodError\n"
            + "Test_java_lang_NullPointerException\nTest_java_lang_Number\n"
            + "Test_java_lang_NumberFormatException\nTest_java_lang_Object\nTest_java_lang_OutOfMemoryError\n"
            + "Test_java_lang_RuntimeException\nTest_java_lang_SecurityManager\nTest_java_lang_Short\n"
            + "Test_java_lang_StackOverflowError\nTest_java_lang_String\nTest_java_lang_StringBuffer\n"
            + "Test_java_lang_StringIndexOutOfBoundsException\nTest_java_lang_System\nTest_java_lang_Thread\n"
            + "Test_java_lang_ThreadDeath\nTest_java_lang_ThreadGroup\nTest_java_lang_Throwable\n"
            + "Test_java_lang_UnknownError\nTest_java_lang_UnsatisfiedLinkError\nTest_java_lang_VerifyError\n"
            + "Test_java_lang_VirtualMachineError\nTest_java_lang_vm_Image\nTest_java_lang_vm_MemorySegment\n"
            + "Test_java_lang_vm_ROMStoreException\nTest_java_lang_vm_VM\nTest_java_lang_Void\n"
            + "Test_java_net_BindException\nTest_java_net_ConnectException\nTest_java_net_DatagramPacket\n"
            + "Test_java_net_DatagramSocket\nTest_java_net_DatagramSocketImpl\nTest_java_net_InetAddress\n"
            + "Test_java_net_NoRouteToHostException\nTest_java_net_PlainDatagramSocketImpl\n"
            + "Test_java_net_PlainSocketImpl\nTest_java_net_Socket\nTest_java_net_SocketException\n"
            + "Test_java_net_SocketImpl\nTest_java_net_SocketInputStream\nTest_java_net_SocketOutputStream\n"
            + "Test_java_net_UnknownHostException\nTest_java_util_ArrayEnumerator\nTest_java_util_Date\n"
            + "Test_java_util_EventObject\nTest_java_util_HashEnumerator\nTest_java_util_Hashtable\n"
            + "Test_java_util_Properties\nTest_java_util_ResourceBundle\nTest_java_util_tm\nTest_java_util_Vector\n";

    @Test
    public void test_flush() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream os = new BufferedOutputStream(baos, 600);
        os.write(fileString.getBytes(), 0, 500);
        os.flush();
        assertEquals("Bytes not written after flush", 500,
                ((ByteArrayOutputStream) baos).size());
    }

    private static class MockOutputStream extends OutputStream {
        byte[] written;
        int count;

        public MockOutputStream(int size) {
            written = new byte[size];
            count = 0;
        }

        public void write(int b) {
            written[count++] = (byte) b;
        }

        public String getWritten() {
            return new String(written, 0, count);
        }
    }

    @Test
    public void test_write$BII() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream os = new BufferedOutputStream(baos, 512);
        os.write(fileString.getBytes(), 0, 500);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertEquals("Bytes written, not buffered", 0, bais.available());
        os.flush();
        bais = new ByteArrayInputStream(baos.toByteArray());
        assertEquals("Bytes not written after flush", 500, bais.available());
        os.write(fileString.getBytes(), 500, 513);
        bais = new ByteArrayInputStream(baos.toByteArray());
        assertTrue("Bytes not written when buffer full",
                bais.available() >= 1000);
        byte[] wbytes = new byte[1013];
        bais.read(wbytes, 0, 1013);
        assertEquals("Incorrect bytes written", new String(wbytes, 0,
                wbytes.length), fileString.substring(0, 1013));

        // regression test for HARMONY-4177
        MockOutputStream mos = new MockOutputStream(5);
        BufferedOutputStream bos = new BufferedOutputStream(mos, 3);
        bos.write("a".getBytes());
        bos.write("bcde".getBytes());
        assertEquals("Large data should be written directly", "abcde", mos
                .getWritten());
        mos = new MockOutputStream(4);
        bos = new BufferedOutputStream(mos, 3);
        bos.write("ab".getBytes());
        bos.write("cd".getBytes());
        assertEquals("Should flush before write", "ab", mos.getWritten());
    }

    @Test
    public void test_write_$BII_Exception() throws IOException {
        OutputStream bos = new BufferedOutputStream(new ByteArrayOutputStream());
        byte[] byteArray = new byte[10];

        try {
            bos.write(byteArray, -1, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, -1, 0);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, -1, 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, 0, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, 0, byteArray.length + 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, 1, byteArray.length);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, -1, byteArray.length);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, byteArray.length, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
        bos.write(byteArray, byteArray.length, 0);
        try {
            bos.write(byteArray, byteArray.length, 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        bos.write(byteArray, 0, 0);
        bos.write(byteArray, 0, 1);
        bos.write(byteArray, 1, byteArray.length - 1);
        bos.write(byteArray, 0, byteArray.length);

        try {
            bos.write(byteArray, 1, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        bos.write(byteArray, 1, 0);
        bos.write(byteArray, 1, 1);

        bos.write(byteArray, byteArray.length, 0);

        try {
            bos.write(byteArray, byteArray.length + 1, 0);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, byteArray.length + 1, 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        bos.close();

        try {
            bos.write(byteArray, -1, -1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // expected IndexOutOfBoundsException
        }
    }

    @Test
    public void test_write_$BII_NullStream() throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(null);
        byte[] byteArray = new byte[10];

        try {
            bos.write(byteArray, -1, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, 0, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, 1, -1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, -1, 0);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        bos.write(byteArray, 0, 0);

        bos.write(byteArray, 1, 0);

        bos.write(byteArray, byteArray.length, 0);

        try {
            bos.write(byteArray, byteArray.length + 1, 0);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            bos.write(byteArray, -1, 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        bos.write(byteArray, 0, 1);
        bos.write(byteArray, 1, 1);

        bos.write(byteArray, 0, byteArray.length);

        try {
            bos.write(byteArray, byteArray.length + 1, 1);
            fail("should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void test_writeI() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream  os = new BufferedOutputStream(baos);
        os.write('t');
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertEquals("Byte written, not buffered", 0, bais.available());
        os.flush();
        bais = new ByteArrayInputStream(baos.toByteArray());
        assertEquals("Byte not written after flush", 1, bais.available());
        byte[] wbytes = new byte[1];
        bais.read(wbytes, 0, 1);
        assertEquals("Incorrect byte written", 't', wbytes[0]);
    }

    @Test
    public void test_write_Scenario1() throws IOException {
        ByteArrayOutputStream byteArrayos = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayis = null;
        byte[] buffer = "1234567890".getBytes("UTF-8");

        BufferedOutputStream buffos = new BufferedOutputStream(byteArrayos, 10);
        buffos.write(buffer, 0, 10);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 10, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 10, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }

        buffos.write(buffer, 0, 10);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 20, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 20, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }

        buffos.write(buffer, 0, 10);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 30, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 30, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
    }

    @Test
    public void test_write_Scenario2() throws IOException {
        ByteArrayOutputStream byteArrayos = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayis = null;
        byte[] buffer = "1234567890".getBytes("UTF-8");

        BufferedOutputStream buffos = new BufferedOutputStream(byteArrayos, 20);
        buffos.write(buffer, 0, 10);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 0, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 10, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }

        byte[] buffer2 = new byte[] { 'a', 'b', 'c', 'd' };
        buffos.write(buffer2, 0, 4);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 10, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 14, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 4; i++) {
            assertEquals(buffer2[i], byteArrayis.read());
        }

        byte[] buffer3 = new byte[] { 'e', 'f', 'g', 'h', 'i' };
        buffos.write(buffer3, 0, 5);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 14, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 19, byteArrayis
                .available());
        for (int i = 0; i < 10; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 4; i++) {
            assertEquals(buffer2[i], byteArrayis.read());
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(buffer3[i], byteArrayis.read());
        }

        buffos.write(new byte[] { 'j', 'k' });
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 19, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 21, byteArrayis
                .available());

        buffos.close();
    }

    @Test
    public void test_write_Scenario3() throws IOException {
        ByteArrayOutputStream byteArrayos = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayis = null;
        byte[] buffer = "1234567890".getBytes("UTF-8");

        BufferedOutputStream buffos = new BufferedOutputStream(byteArrayos, 5);
        buffos.write(buffer, 0, 4);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 0, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 4, byteArrayis
                .available());
        for (int i = 0; i < 4; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }

        buffos.write(buffer, 0, 5);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 9, byteArrayis.available());
        buffos.flush();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 9, byteArrayis
                .available());
        for (int i = 0; i < 4; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
    }

    @Test
    public void test_write_Scenario4() throws IOException {
        ByteArrayOutputStream byteArrayos = new ByteArrayOutputStream();
        ByteArrayInputStream byteArrayis = null;
        byte[] buffer = "1234567890".getBytes("UTF-8");

        BufferedOutputStream buffos = new BufferedOutputStream(byteArrayos, 5);
        buffos.write(buffer, 0, 4);
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes written, not buffered", 0, byteArrayis.available());
        buffos.close();
        byteArrayis = new ByteArrayInputStream(byteArrayos.toByteArray());
        assertEquals("Bytes not written after flush", 4, byteArrayis
                .available());
        for (int i = 0; i < 4; i++) {
            assertEquals(buffer[i], byteArrayis.read());
        }
    }

}
