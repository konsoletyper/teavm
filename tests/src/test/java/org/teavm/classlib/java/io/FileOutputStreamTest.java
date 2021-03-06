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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class FileOutputStreamTest {
    private String fileName;
    private String fileString =
            "Test_All_Tests\n" + "Test_java_io_BufferedInputStream\n"
                    + "Test_java_io_BufferedOutputStream\n"
                    + "Test_java_io_ByteArrayInputStream\n"
                    + "Test_java_io_ByteArrayOutputStream\n"
                    + "Test_java_io_DataInputStream\n"
                    + "Test_java_io_File\n"
                    + "Test_java_io_FileDescriptor\n"
                    + "Test_java_io_FileInputStream\n"
                    + "Test_java_io_FileNotFoundException\n"
                    + "Test_FileOutputStream\n"
                    + "Test_java_io_FilterInputStream\n"
                    + "Test_java_io_FilterOutputStream\n"
                    + "Test_java_io_InputStream\n"
                    + "Test_java_io_IOException\n"
                    + "Test_java_io_OutputStream\n"
                    + "Test_java_io_PrintStream\n"
                    + "Test_java_io_RandomAccessFile\n"
                    + "Test_java_io_SyncFailedException\n"
                    + "Test_java_lang_AbstractMethodError\n"
                    + "Test_java_lang_ArithmeticException\n"
                    + "Test_java_lang_ArrayIndexOutOfBoundsException\n"
                    + "Test_java_lang_ArrayStoreException\n"
                    + "Test_java_lang_Boolean\n"
                    + "Test_java_lang_Byte\n"
                    + "Test_java_lang_Character\n"
                    + "Test_java_lang_Class\n"
                    + "Test_java_lang_ClassCastException\n"
                    + "Test_java_lang_ClassCircularityError\n"
                    + "Test_java_lang_ClassFormatError\n"
                    + "Test_java_lang_ClassLoader\n"
                    + "Test_java_lang_ClassNotFoundException\n"
                    + "Test_java_lang_CloneNotSupportedException\n"
                    + "Test_java_lang_Double\n"
                    + "Test_java_lang_Error\n"
                    + "Test_java_lang_Exception\n"
                    + "Test_java_lang_ExceptionInInitializerError\n"
                    + "Test_java_lang_Float\n"
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
    private FileOutputStream fos;
    private FileInputStream fis;
    private File f;
    private byte[] bytes;

    public FileOutputStreamTest() {
        bytes = new byte[10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
    }

    @Test
    public void constructorLjava_io_File() throws IOException {
        fileName = System.getProperty("user.home");
        f = new File(fileName, "fos.tst");
        fos = new FileOutputStream(f);
    }

    @Test
    public void test_ConstructorLjava_lang_String() throws IOException {
        fileName = System.getProperty("user.home");
        f = new File(fileName, "fos.tst");
        fileName = f.getAbsolutePath();
        fos = new FileOutputStream(fileName);

        // Regression test for HARMONY-4012
        new FileOutputStream("nul");
    }

    @Test
    public void constructorLjava_lang_StringZ() throws IOException {
        f = new File(System.getProperty("user.home"), "fos.tst");
        fos = new FileOutputStream(f.getPath(), false);
        fos.write("HI".getBytes(), 0, 2);
        fos.close();
        fos = new FileOutputStream(f.getPath(), true);
        fos.write(fileString.getBytes());
        fos.close();
        byte[] buf = new byte[fileString.length() + 2];
        fis = new FileInputStream(f.getPath());
        fis.read(buf, 0, buf.length);
        assertTrue("Failed to create appending stream", new String(buf, 0, buf.length).equals("HI" + fileString));
    }

    @Test
    public void constructorLjava_lang_String_I() throws IOException {
        try {
            fos = new FileOutputStream("");
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        try {
            fos = new FileOutputStream(new File(""));
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Test
    public void close() throws IOException {
        f = new File(System.getProperty("user.home"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.close();

        try {
            fos.write(fileString.getBytes());
            fail("Close test failed - wrote to closed stream");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void write$B() throws IOException {
        f = new File(System.getProperty("user.home"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.write(fileString.getBytes());
        fos.close();
        fis = new FileInputStream(f.getPath());
        byte[] rbytes = new byte[4000];
        fis.read(rbytes, 0, fileString.length());
        assertEquals("Incorrect string returned", fileString, new String(rbytes, 0, fileString.length()));
    }

    @Test
    public void write$BII() throws IOException {
        f = new File(System.getProperty("user.home"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        fis = new FileInputStream(f.getPath());
        byte[] rbytes = new byte[4000];
        fis.read(rbytes, 0, fileString.length());
        assertEquals("Incorrect bytes written", fileString, new String(rbytes, 0, fileString.length()));

        // Regression test for HARMONY-285
        File file = new File("FileOutputStream.tmp");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(null, 0, 0);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } finally {
            file.delete();
        }
    }

    @Test
    public void writeI() throws IOException {
        f = new File(System.getProperty("user.home"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.write('t');
        fos.close();
        fis = new FileInputStream(f.getPath());
        assertEquals("Incorrect char written", 't', fis.read());
    }

    @Test
    public void write$BII2() throws IOException {
        // Regression for HARMONY-437
        f = new File(System.getProperty("user.home"), "output.tst");
        fos = new FileOutputStream(f.getPath());

        try {
            fos.write(null, 1, 1);
            fail("NullPointerException must be thrown");
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            fos.write(new byte[1], -1, 1);
            fail("IndexOutOfBoundsException must be thrown if off <0");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fos.write(new byte[1], 0, -1);
            fail("IndexOutOfBoundsException must be thrown if len <0");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fos.write(new byte[1], 0, 5);
            fail("IndexOutOfBoundsException must be thrown if off+len > b.length");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fos.write(new byte[10], Integer.MAX_VALUE, 5);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fos.write(new byte[10], 5, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        fos.close();
    }

    @Test
    public void repeatedWrite() throws IOException {
        f = new File(System.getProperty("user.home"), "test.txt");
        fos = new FileOutputStream(f);
        fos.write("A very long test string for purposes of testing.".getBytes());
        fos.close();

        fos = new FileOutputStream(f);
        fos.write("A short string.".getBytes());
        fos.close();

        int length = (int) f.length();
        byte[] bytes = new byte[length];
        fis = new FileInputStream(f);
        fis.read(bytes, 0, length);
        String str = new String(bytes);

        assertEquals("A short string.", str);
    }

    @After
    public void tearDown() throws Exception {
        if (f != null) {
            f.delete();
        }
        if (fis != null) {
            fis.close();
        }
        if (fos != null) {
            fos.close();
        }
    }
}
