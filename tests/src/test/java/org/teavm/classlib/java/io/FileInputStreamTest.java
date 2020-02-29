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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class FileInputStreamTest {
    public String fileName;
    public String fileString = ""
            + "Test_All_Tests\n" + "Test_java_io_BufferedInputStream\n" + "Test_java_io_BufferedOutputStream\n"
            + "Test_java_io_ByteArrayInputStream\n" + "Test_java_io_ByteArrayOutputStream\n"
            + "Test_java_io_DataInputStream\n" + "Test_java_io_File\n" + "Test_java_io_FileDescriptor\n"
            + "Test_FileInputStream\n" + "Test_java_io_FileNotFoundException\n"
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
    private InputStream is;

    public FileInputStreamTest() throws IOException {
        File file = File.createTempFile("tmp", "tmp");
        OutputStream fos = new FileOutputStream(file);
        fos.write(fileString.getBytes());
        fos.close();
        fileName = file.getPath();
    }

    @Test
    public void constructorLjava_io_File() throws IOException {
        File f = new File(fileName);
        is = new FileInputStream(f);
        is.close();
    }

    @Test
    public void constructorLjava_lang_String() throws IOException {
        is = new FileInputStream(fileName);
        is.close();
    }

    @Test
    public void constructorLjava_lang_String_I() throws IOException {
        try {
            is = new FileInputStream("");
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            if (is != null) {
                is.close();
            }
        }
        try {
            is = new FileInputStream(new File(""));
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void available() throws IOException {
        try {
            is = new FileInputStream(fileName);
            assertTrue("Returned incorrect number of available bytes", is.available() == fileString.length());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Expected
            }
        }
    }

    @Test
    public void close() throws IOException {
        is = new FileInputStream(fileName);
        is.close();

        try {
            is.read();
            fail("Able to read from closed stream");
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    public void read() throws IOException {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName));
        int c = isr.read();
        isr.close();
        assertTrue("read returned incorrect char", c == fileString.charAt(0));
    }

    @Test
    public void read$B() throws IOException {
        byte[] buf1 = new byte[100];
        is = new FileInputStream(fileName);
        is.skip(3000);
        is.read(buf1);
        is.close();
        assertEquals("Failed to read correct data", new String(buf1, 0, buf1.length), fileString.substring(3000, 3100));
    }

    @Test
    public void read$BII() throws IOException {
        byte[] buf1 = new byte[100];
        is = new FileInputStream(fileName);
        is.skip(3000);
        is.read(buf1, 0, buf1.length);
        is.close();
        assertTrue("Failed to read correct data",
                new String(buf1, 0, buf1.length).equals(fileString.substring(3000, 3100)));

        // Regression test for HARMONY-285
        File file = new File("FileInputStream.tmp");
        file.createNewFile();
        file.deleteOnExit();
        FileInputStream in = new FileInputStream(file);
        try {
            in.read(null, 0, 0);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } finally {
            in.close();
            file.delete();
        }
    }

    @Test
    public void read_$BII_IOException() throws IOException {
        byte[] buf = new byte[1000];
        try {
            is = new FileInputStream(fileName);
            is.read(buf, -1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.read(buf, 0, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.read(buf, -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.read(buf, 0, 1001);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.read(buf, 1001, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.read(buf, 500, 501);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.close();
            is.read(buf, 0, 100);
            fail("should throw IOException");
        } catch (IOException e) {
            // Expected
        } finally {
            is.close();
        }

        try {
            is = new FileInputStream(fileName);
            is.close();
            is.read(buf, 0, 0);
        } finally {
            is.close();
        }
    }

    @Test
    public void read_$BII_NullPointerException() throws IOException {
        byte[] buf = null;
        try {
            is = new FileInputStream(fileName);
            is.read(buf, -1, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        } finally {
            is.close();
        }
    }

    @Test
    public void read_$BII_IndexOutOfBoundsException() throws IOException {
        byte[] buf = new byte[1000];
        try {
            is = new FileInputStream(fileName);
            is.close();
            is.read(buf, -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        } finally {
            is.close();
        }
    }

    @Test
    public void skipJ() throws IOException {
        byte[] buf1 = new byte[10];
        is = new FileInputStream(fileName);
        is.skip(1000);
        is.read(buf1, 0, buf1.length);
        is.close();
        assertTrue("Failed to skip to correct position",
                new String(buf1, 0, buf1.length).equals(fileString.substring(1000, 1010)));
    }

    @Test
    public void regressionNNN() throws IOException {
        // Regression for HARMONY-434
        FileInputStream fis = new FileInputStream(fileName);

        try {
            fis.read(new byte[1], -1, 1);
            fail("IndexOutOfBoundsException must be thrown if off <0");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fis.read(new byte[1], 0, -1);
            fail("IndexOutOfBoundsException must be thrown if len <0");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fis.read(new byte[1], 0, 5);
            fail("IndexOutOfBoundsException must be thrown if off+len > b.length");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fis.read(new byte[10], Integer.MAX_VALUE, 5);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        try {
            fis.read(new byte[10], 5, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        fis.close();
    }

    @Test
    public void skipNegativeArgumentJ() throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        try {
            fis.skip(-5);
            fail("IOException must be thrown if number of bytes to skip <0");
        } catch (IOException e) {
            // Expected IOException
        } finally {
            fis.close();
        }
    }

    @After
    public void tearDown() {
        new File(fileName).delete();
    }
}

