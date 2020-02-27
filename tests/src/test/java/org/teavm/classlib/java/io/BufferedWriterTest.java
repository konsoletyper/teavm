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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class BufferedWriterTest {
    private BufferedWriter bw;
    private StringWriter sw;

    private String testString = ""
            + "Test_All_Tests\n" + "Test_java_io_BufferedInputStream\n" + "Test_java_io_BufferedOutputStream\n"
            + "Test_java_io_ByteArrayInputStream\n" + "Test_java_io_ByteArrayOutputStream\n"
            + "Test_java_io_DataInputStream\n" + "Test_java_io_File\n" + "Test_java_io_FileDescriptor\n"
            + "Test_java_io_FileInputStream\n" + "Test_java_io_FileNotFoundException\n"
            + "Test_java_io_FileOutputStream\n" + "Test_java_io_FilterInputStream\n"
            + "Test_java_io_FilterOutputStream\n" + "Test_java_io_InputStream\n" + "Test_java_io_IOException\n"
            + "Test_java_io_OutputStream\n" + "Test_java_io_PrintStream\n" + "Test_java_io_RandomAccessFile\n"
            + "Test_java_io_SyncFailedException\n" + "Test_java_lang_AbstractMethodError\n"
            + "Test_java_lang_ArithmeticException\n" + "Test_java_lang_ArrayIndexOutOfBoundsException\n"
            + "Test_java_lang_ArrayStoreException\n" + "Test_java_lang_Boolean\n" + "Test_java_lang_Byte\n"
            + "Test_java_lang_Character\n" + "Test_java_lang_Class\n" + "Test_java_lang_ClassCastException\n"
            + "Test_java_lang_ClassCircularityError\n" + "Test_java_lang_ClassFormatError\n"
            + "Test_java_lang_ClassLoader\n" + "Test_java_lang_ClassNotFoundException\n"
            + "Test_java_lang_CloneNotSupportedException\n" + "Test_java_lang_Double\n"
            + "Test_java_lang_Error\n" + "Test_java_lang_Exception\n"
            + "Test_java_lang_ExceptionInInitializerError\n" + "Test_java_lang_Float\n"
            + "Test_java_lang_IllegalAccessError\n" + "Test_java_lang_IllegalAccessException\n"
            + "Test_java_lang_IllegalArgumentException\n" + "Test_java_lang_IllegalMonitorStateException\n"
            + "Test_java_lang_IllegalThreadStateException\n" + "Test_java_lang_IncompatibleClassChangeError\n"
            + "Test_java_lang_IndexOutOfBoundsException\n" + "Test_java_lang_InstantiationError\n"
            + "Test_java_lang_InstantiationException\n" + "Test_java_lang_Integer\n"
            + "Test_java_lang_InternalError\n" + "Test_java_lang_InterruptedException\n"
            + "Test_java_lang_LinkageError\n" + "Test_java_lang_Long\n" + "Test_java_lang_Math\n"
            + "Test_java_lang_NegativeArraySizeException\n" + "Test_java_lang_NoClassDefFoundError\n"
            + "Test_java_lang_NoSuchFieldError\n" + "Test_java_lang_NoSuchMethodError\n"
            + "Test_java_lang_NullPointerException\n" + "Test_java_lang_Number\n"
            + "Test_java_lang_NumberFormatException\n" + "Test_java_lang_Object\n"
            + "Test_java_lang_OutOfMemoryError\n" + "Test_java_lang_RuntimeException\n"
            + "Test_java_lang_SecurityManager\n" + "Test_java_lang_Short\n"
            + "Test_java_lang_StackOverflowError\n" + "Test_java_lang_String\n"
            + "Test_java_lang_StringBuffer\n" + "Test_java_lang_StringIndexOutOfBoundsException\n"
            + "Test_java_lang_System\n" + "Test_java_lang_Thread\n" + "Test_java_lang_ThreadDeath\n"
            + "Test_java_lang_ThreadGroup\n" + "Test_java_lang_Throwable\n" + "Test_java_lang_UnknownError\n"
            + "Test_java_lang_UnsatisfiedLinkError\n" + "Test_java_lang_VerifyError\n"
            + "Test_java_lang_VirtualMachineError\n" + "Test_java_lang_vm_Image\n"
            + "Test_java_lang_vm_MemorySegment\n" + "Test_java_lang_vm_ROMStoreException\n"
            + "Test_java_lang_vm_VM\n" + "Test_java_lang_Void\n" + "Test_java_net_BindException\n"
            + "Test_java_net_ConnectException\n" + "Test_java_net_DatagramPacket\n"
            + "Test_java_net_DatagramSocket\n" + "Test_java_net_DatagramSocketImpl\n"
            + "Test_java_net_InetAddress\n" + "Test_java_net_NoRouteToHostException\n"
            + "Test_java_net_PlainDatagramSocketImpl\n" + "Test_java_net_PlainSocketImpl\n"
            + "Test_java_net_Socket\n" + "Test_java_net_SocketException\n" + "Test_java_net_SocketImpl\n"
            + "Test_java_net_SocketInputStream\n" + "Test_java_net_SocketOutputStream\n"
            + "Test_java_net_UnknownHostException\n" + "Test_java_util_ArrayEnumerator\n"
            + "Test_java_util_Date\n" + "Test_java_util_EventObject\n" + "Test_java_util_HashEnumerator\n"
            + "Test_java_util_Hashtable\n" + "Test_java_util_Properties\n" + "Test_java_util_ResourceBundle\n"
            + "Test_java_util_tm\n" + "Test_java_util_Vector\n";

    public BufferedWriterTest() {
        sw = new StringWriter();
        bw = new BufferedWriter(sw, 500);
    }

    @Test
    public void constructorLjava_io_Writer() {
        sw = new StringWriter();
        bw = new BufferedWriter(sw);
        sw.write("Hi");
        assertEquals("Constructor failed", "Hi", sw.toString());
    }

    @Test
    public void constructorLjava_io_WriterI() {
        assertTrue("Used in tests", true);
    }

    static class MockWriter extends Writer {
        StringBuffer sb = new StringBuffer();
        boolean flushCalled;

        @Override
        public void write(char[] buf, int off, int len) {
            for (int i = off; i < off + len; i++) {
                sb.append(buf[i]);
            }
        }

        @Override
        public void close() {
            // Empty
        }

        @Override
        public void flush() {
            flushCalled = true;
        }

        String getWritten() {
            return sb.toString();
        }

        boolean isFlushCalled() {
            return flushCalled;
        }
    }

    @Test
    public void close() throws IOException {
        try {
            bw.close();
            bw.write(testString);
            fail("Writing to a closed stream should throw IOException");
        } catch (IOException e) {
            // Expected
        }
        assertTrue("Write after close", !sw.toString().equals(testString));

        // Regression test for HARMONY-4178
        MockWriter mw = new MockWriter();
        BufferedWriter bw = new BufferedWriter(mw);
        bw.write('a');
        bw.close();

        // flush should not be called on underlying stream
        assertFalse("Flush was called in the underlying stream", mw.isFlushCalled());

        // on the other hand the BufferedWriter itself should flush the
        // buffer
        assertEquals("BufferdWriter do not flush itself before close", "a", mw.getWritten());
    }

    @Test
    public void close2() throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream()));
        bw.close();
    }

    @Test
    public void flush() throws Exception {
        bw.write("This should not cause a flush");
        assertTrue("Bytes written without flush", sw.toString().equals(""));
        bw.flush();
        assertEquals("Bytes not flushed", "This should not cause a flush", sw.toString());
    }

    @Test
    public void newLine() throws Exception {
        String separator = System.getProperty("line.separator");
        bw.write("Hello");
        bw.newLine();
        bw.write("World");
        bw.flush();
        assertTrue("Incorrect string written: " + sw.toString(), sw.toString().equals("Hello" + separator + "World"));
    }

    @Test
    public void write$CII() throws Exception {
        char[] testCharArray = testString.toCharArray();
        bw.write(testCharArray, 500, 1000);
        bw.flush();
        assertTrue("Incorrect string written", sw.toString().equals(testString.substring(500, 1500)));
    }

    @Test
    public void write_$CII_Exception() throws IOException {
        BufferedWriter bWriter = new BufferedWriter(sw);
        char[] nullCharArray = null;

        try {
            bWriter.write(nullCharArray, -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            bWriter.write(nullCharArray, -1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            bWriter.write(nullCharArray, 0, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            bWriter.write(nullCharArray, 0, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        char[] testCharArray = testString.toCharArray();

        bWriter.write(testCharArray, 0, 0);

        bWriter.write(testCharArray, testCharArray.length, 0);

        try {
            bWriter.write(testCharArray, testCharArray.length + 1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        bWriter.close();

        try {
            bWriter.write(nullCharArray, -1, -1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void writeI() throws Exception {
        bw.write('T');
        assertTrue("Char written without flush", sw.toString().equals(""));
        bw.flush();
        assertEquals("Incorrect char written", "T", sw.toString());
    }

    @Test
    public void writeLjava_lang_StringII() throws Exception {
        bw.write(testString);
        bw.flush();
        assertTrue("Incorrect string written", sw.toString().equals(testString));
    }

    @Test
    public void write_LStringII_Exception() throws IOException {
        BufferedWriter bWriter = new BufferedWriter(sw);

        bWriter.write((String) null, -1, -1);
        bWriter.write((String) null, -1, 0);
        bWriter.write((String) null, 0, -1);
        bWriter.write((String) null, 0, 0);

        try {
            bWriter.write((String) null, -1, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        bWriter.write(testString, 0, 0);
        bWriter.write(testString, testString.length(), 0);
        bWriter.write(testString, testString.length() + 1, 0);

        try {
            bWriter.write(testString, testString.length() + 1, 1);
            fail("should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
            // expected
        }

        bWriter.close();

        try {
            bWriter.write((String) null, -1, -1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }

        try {
            bWriter.write((String) null, -1, 1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }

        try {
            bWriter.write(testString, -1, -1);
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @After
    public void tearDown() {
        try {
            bw.close();
        } catch (Exception e) {
            // Do nothing
        }
    }
}
