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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.teavm.interop.PlatformMarker;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class FileTest {
    private static String platformId = "JDK" + System.getProperty("java.vm.version").replace('.', '-');
    
    private static void deleteTempFolder(File dir) {
        String[] files = dir.list();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                if (f.isDirectory()) {
                    deleteTempFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
    
    private static String addTrailingSlash(String path) {
        if (File.separatorChar == path.charAt(path.length() - 1)) {
            return path;
        }
        return path + File.separator;
    }
    
    /** Location to store tests in */
    private File tempDirectory;

    public String fileString = ""
            + "Test_All_Tests\n"
            + "Test_java_io_BufferedInputStream\n"
            + "Test_java_io_BufferedOutputStream\n"
            + "Test_java_io_ByteArrayInputStream\n"
            + "Test_java_io_ByteArrayOutputStream\n"
            + "Test_java_io_DataInputStream\n"
            + "Test_File\n"
            + "Test_FileDescriptor\n"
            + "Test_FileInputStream\n"
            + "Test_FileNotFoundException\n"
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
            + "Test_java_lang_IllegalAccessError\n"
            + "Test_java_lang_IllegalAccessException\n"
            + "Test_java_lang_IllegalArgumentException\n"
            + "Test_java_lang_IllegalMonitorStateException\n"
            + "Test_java_lang_IllegalThreadStateException\n"
            + "Test_java_lang_IncompatibleClassChangeError\n"
            + "Test_java_lang_IndexOutOfBoundsException\n"
            + "Test_java_lang_InstantiationError\n"
            + "Test_java_lang_InstantiationException\n"
            + "Test_java_lang_Integer\n"
            + "Test_java_lang_InternalError\n"
            + "Test_java_lang_InterruptedException\n"
            + "Test_java_lang_LinkageError\n"
            + "Test_java_lang_Long\n"
            + "Test_java_lang_Math\n"
            + "Test_java_lang_NegativeArraySizeException\n"
            + "Test_java_lang_NoClassDefFoundError\n"
            + "Test_java_lang_NoSuchFieldError\n"
            + "Test_java_lang_NoSuchMethodError\n"
            + "Test_java_lang_NullPointerException\n"
            + "Test_java_lang_Number\n"
            + "Test_java_lang_NumberFormatException\n"
            + "Test_java_lang_Object\n"
            + "Test_java_lang_OutOfMemoryError\n"
            + "Test_java_lang_RuntimeException\n"
            + "Test_java_lang_SecurityManager\n"
            + "Test_java_lang_Short\n"
            + "Test_java_lang_StackOverflowError\n"
            + "Test_java_lang_String\n"
            + "Test_java_lang_StringBuffer\n"
            + "Test_java_lang_StringIndexOutOfBoundsException\n"
            + "Test_java_lang_System\n"
            + "Test_java_lang_Thread\n"
            + "Test_java_lang_ThreadDeath\n"
            + "Test_java_lang_ThreadGroup\n"
            + "Test_java_lang_Throwable\n"
            + "Test_java_lang_UnknownError\n"
            + "Test_java_lang_UnsatisfiedLinkError\n"
            + "Test_java_lang_VerifyError\n"
            + "Test_java_lang_VirtualMachineError\n"
            + "Test_java_lang_vm_Image\n"
            + "Test_java_lang_vm_MemorySegment\n"
            + "Test_java_lang_vm_ROMStoreException\n"
            + "Test_java_lang_vm_VM\n"
            + "Test_java_lang_Void\n"
            + "Test_java_net_BindException\n"
            + "Test_java_net_ConnectException\n"
            + "Test_java_net_DatagramPacket\n"
            + "Test_java_net_DatagramSocket\n"
            + "Test_java_net_DatagramSocketImpl\n"
            + "Test_java_net_InetAddress\n"
            + "Test_java_net_NoRouteToHostException\n"
            + "Test_java_net_PlainDatagramSocketImpl\n"
            + "Test_java_net_PlainSocketImpl\n"
            + "Test_java_net_Socket\n"
            + "Test_java_net_SocketException\n"
            + "Test_java_net_SocketImpl\n"
            + "Test_java_net_SocketInputStream\n"
            + "Test_java_net_SocketOutputStream\n"
            + "Test_java_net_UnknownHostException\n"
            + "Test_java_util_ArrayEnumerator\n"
            + "Test_java_util_Date\n"
            + "Test_java_util_EventObject\n"
            + "Test_java_util_HashEnumerator\n"
            + "Test_java_util_Hashtable\n"
            + "Test_java_util_Properties\n"
            + "Test_java_util_ResourceBundle\n"
            + "Test_java_util_tm\n"
            + "Test_java_util_Vector\n";

    public FileTest() {
        /* Setup the temporary directory */
        tempDirectory = new File(addTrailingSlash(System.getProperty("java.io.tmpdir")) + "harmony-test-"
                + getClass().getSimpleName() + File.separator);
        tempDirectory.mkdirs();
    }

    @AfterEach
    public void tearDown() {
        if (tempDirectory != null) {
            deleteTempFolder(tempDirectory);
            tempDirectory = null;
        }
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String0() {
        File f = new File(tempDirectory.getPath(), "input.tst");
        assertEquals(addTrailingSlash(tempDirectory.getPath()) + "input.tst", f.getPath(), "Created Incorrect File ");
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String1() {
        try {
            new File(tempDirectory, null);
            fail("NullPointerException Not Thrown.");
        } catch (NullPointerException e) {
            // As expected
        }
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String2() throws IOException {
        File f = new File((File) null, "input.tst");
        assertEquals(new File("input.tst").getAbsolutePath(), f.getAbsolutePath(), "Created Incorrect File");
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String3() {
        File f = new File("/abc");
        File d = new File((File) null, "/abc");
        assertEquals(d.getAbsolutePath(), f.getAbsolutePath(), "Test3: Created Incorrect File");
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String4() {
        File path = new File("/dir/file");
        File root = new File("/");
        File file = new File(root, "/dir/file");
        assertEquals(path.getPath(), file.getPath(), "Assert 1: wrong path result ");
        if (File.separatorChar == '\\') {
            assertTrue(new File("c:\\\\\\a\b").isAbsolute(), "Assert 1.1: path not absolute ");
        } else {
            assertFalse(new File("\\\\\\a\b").isAbsolute(), "Assert 1.1: path absolute ");
        }
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String5() {
        // Test data used in a few places below
        String dirName = tempDirectory.getPath();
        String fileName = "input.tst";

        // Check filename is preserved correctly
        File d = new File(dirName);
        File f = new File(d, fileName);
        dirName = addTrailingSlash(dirName);
        dirName += fileName;
        assertEquals(dirName, f.getPath(), "Assert 1: Created incorrect file ");

        // Check null argument is handled
        try {
            f = new File(d, null);
            fail("Assert 2: NullPointerException not thrown.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    @Test
    public void constructorLjava_io_FileLjava_lang_String6() {
        File f1 = new File("a");
        File f2 = new File("a/");
        assertEquals(f1, f2, "Trailing slash file name is incorrect");
    }

    @Test
    public void constructorLjava_lang_String() {
        String fileName = null;
        try {
            new File(fileName);
            fail("NullPointerException Not Thrown.");
        } catch (NullPointerException e) {
            // Expected
        }

        fileName = addTrailingSlash(tempDirectory.getPath());
        fileName += "input.tst";

        File f = new File(fileName);
        assertEquals(fileName, f.getPath(), "Created incorrect File");
    }

    @Test
    public void constructorLjava_lang_StringLjava_lang_String() throws IOException {
        String dirName = null;
        String fileName = "input.tst";
        File f = new File(dirName, fileName);
        assertEquals(new File("input.tst").getAbsolutePath(), f.getAbsolutePath(), "Test 1: Created Incorrect File");

        dirName = tempDirectory.getPath();
        fileName = null;
        try {
            f = new File(dirName, fileName);
            fail("NullPointerException Not Thrown.");
        } catch (NullPointerException e) {
            // Expected
        }

        fileName = "input.tst";
        f = new File(dirName, fileName);
        assertEquals(addTrailingSlash(tempDirectory.getPath()) + "input.tst", f.getPath(),
                "Test 2: Created Incorrect File");

        // Regression test for HARMONY-382
        String s = null;
        f = new File("/abc");
        File d = new File(s, "/abc");
        assertEquals(d.getAbsolutePath(), f.getAbsolutePath(), "Test3: Created Incorrect File");
    }

    @Test
    public void constructor_String_String_112270() {
        File ref1 = new File("/dir1/file1");

        File file1 = new File("/", "/dir1/file1");
        assertEquals(ref1.getPath(), file1.getPath(), "wrong result 1");
        File file2 = new File("/", "//dir1/file1");
        assertEquals(ref1.getPath(), file2.getPath(), "wrong result 2");

        if (File.separatorChar == '\\') {
            File file3 = new File("\\", "\\dir1\\file1");
            assertEquals(ref1.getPath(), file3.getPath(), "wrong result 3");
            File file4 = new File("\\", "\\\\dir1\\file1");
            assertEquals(ref1.getPath(), file4.getPath(), "wrong result 4");
        }

        File ref2 = new File("/lib/content-types.properties");
        File file5 = new File("/", "lib/content-types.properties");
        assertEquals(ref2.getPath(), file5.getPath(), "wrong result 5");
    }

    @Test
    public void constructor_File_String_112270() {
        File ref1 = new File("/dir1/file1");

        File root = new File("/");
        File file1 = new File(root, "/dir1/file1");
        assertEquals(ref1.getPath(), file1.getPath(), "wrong result 1");
        File file2 = new File(root, "//dir1/file1");
        assertEquals(ref1.getPath(), file2.getPath(), "wrong result 2");

        if (File.separatorChar == '\\') {
            File file3 = new File(root, "\\dir1\\file1");
            assertEquals(ref1.getPath(), file3.getPath(), "wrong result 3");
            File file4 = new File(root, "\\\\dir1\\file1");
            assertEquals(ref1.getPath(), file4.getPath(), "wrong result 4");
        }

        File ref2 = new File("/lib/content-types.properties");
        File file5 = new File(root, "lib/content-types.properties");
        assertEquals(ref2.getPath(), file5.getPath(), "wrong result 5");
    }

    @Test
    public void constructorLjava_net_URI() throws URISyntaxException {
        URI uri;

        // invalid file URIs
        String[] uris = new String[] { "mailto:user@domain.com", // not
                // hierarchical
                "ftp:///path", // not file scheme
                "//host/path/", // not absolute
                "file://host/path", // non empty authority
                "file:///path?query", // non empty query
                "file:///path#fragment", // non empty fragment
                "file:///path?", "file:///path#" };

        for (int i = 0; i < uris.length; i++) {
            uri = new URI(uris[i]);
            try {
                new File(uri);
                fail("Expected IllegalArgumentException for new File(" + uri + ")");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }

        // a valid File URI
        File f = new File(new URI("file:///pa%20th/another\u20ac/pa%25th"));
        assertTrue(f.getPath().equals(
                File.separator + "pa th" + File.separator + "another\u20ac" + File.separator + "pa%th"),
                "Created incorrect File " + f.getPath());
    }

    @Test
    public void canRead() throws IOException {
        // canRead only returns if the file exists so cannot be fully tested.
        File f = new File(tempDirectory, platformId + "canRead.tst");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue(f.canRead(), "canRead returned false");
        } finally {
            f.delete();
        }
    }

    @Test
    public void canWrite() throws IOException {
        // canWrite only returns if the file exists so cannot be fully tested.
        File f = new File(tempDirectory, platformId + "canWrite.tst");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.close();
            assertTrue(f.canWrite(), "canWrite returned false");
        } finally {
            f.delete();
        }
    }

    @Test
    public void compareToLjava_io_File() {
        File f1 = new File("thisFile.file");
        File f2 = new File("thisFile.file");
        File f3 = new File("thatFile.file");
        assertEquals(0, f1.compareTo(f2), "Equal files did not answer zero for compareTo");
        assertTrue(f3.compareTo(f1) < 0, "f3.compareTo(f1) did not result in value < 0");
        assertTrue(f1.compareTo(f3) > 0, "f1.compareTo(f3) did not result in value > 0");
    }

    @Test
    public void createNewFile_EmptyString() {
        File f = new File("");
        try {
            f.createNewFile();
            fail("should throw IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void createNewFile() throws IOException {
        String base = tempDirectory.getPath();
        boolean dirExists = true;
        int numDir = 1;
        File dir = new File(base, String.valueOf(numDir));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number
            // (making it a new directory name.)
            if (dir.exists()) {
                numDir++;
                dir = new File(base, String.valueOf(numDir));
            } else {
                dirExists = false;
            }
        }

        // Test for trying to create a file in a directory that does not
        // exist.
        try {
            // Try to create a file in a directory that does not exist
            File f1 = new File(dir, "tempfile.tst");
            f1.createNewFile();
            fail("IOException not thrown");
        } catch (IOException e) {
            // Expected
        }

        dir.mkdir();

        File f1 = new File(dir, "tempfile.tst");
        File f2 = new File(dir, "tempfile.tst");
        f1.deleteOnExit();
        f2.deleteOnExit();
        dir.deleteOnExit();
        assertFalse(f1.isFile(), "File Should Not Exist");
        f1.createNewFile();
        assertTrue(f1.isFile(), "File Should Exist.");
        assertTrue(f2.isFile(), "File Should Exist.");
        String dirName = f1.getParent();
        if (!dirName.endsWith(File.separator)) {
            dirName += File.separator;
        }
        assertEquals(dir.getPath() + File.separator, dirName, "File Saved To Wrong Directory.");
        assertEquals("tempfile.tst", f1.getName(), "File Saved With Incorrect Name.");

        // Test for creating a file that already exists.
        assertFalse(f2.createNewFile(), "File Already Exists, createNewFile Should Return False.");
    }

    @Test
    public void createTempFileLjava_lang_StringLjava_lang_String() throws IOException {
        // Error protection against using a suffix without a "."?
        File f1 = null;
        File f2 = null;
        try {
            f1 = File.createTempFile("harmony-test-FileTest_tempFile_abc", ".tmp");
            f2 = File.createTempFile("harmony-test-FileTest_tempFile_tf", null);

            String fileLocation = addTrailingSlash(f1.getParent());
            String tempDir = addTrailingSlash(System.getProperty("java.io.tmpdir"));
            
            assertEquals(tempDir, fileLocation, "File did not save to the default temporary-file location.");

            // Test to see if correct suffix was used to create the tempfile.
            File currentFile;
            String fileName;
            // Testing two files, one with suffix ".tmp" and one with null
            for (int i = 0; i < 2; i++) {
                currentFile = i == 0 ? f1 : f2;
                fileName = currentFile.getPath();
                assertTrue(fileName.endsWith(".tmp"), "File Created With Incorrect Suffix.");
            }

            // Tests to see if the correct prefix was used to create the
            // tempfiles.
            fileName = f1.getName();
            assertTrue(fileName.startsWith("harmony-test-FileTest_tempFile_abc"),
                    "Test 1: File Created With Incorrect Prefix.");
            fileName = f2.getName();
            assertTrue(fileName.startsWith("harmony-test-FileTest_tempFile_tf"),
                    "Test 2: File Created With Incorrect Prefix.");

            // Tests for creating a tempfile with a filename shorter than 3
            // characters.
            try {
                File f3 = File.createTempFile("ab", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                File f3 = File.createTempFile("a", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                File f3 = File.createTempFile("", ".tst");
                f3.delete();
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        } finally {
            if (f1 != null) {
                f1.delete();
            }
            if (f2 != null) {
                f2.delete();
            }
        }
    }

    @Test
    public void createTempFileLjava_lang_StringLjava_lang_StringLjava_io_File() throws IOException {
        File f1 = null;
        File f2 = null;
        String base = System.getProperty("java.io.tmpdir");
        try {
            // Test to make sure that the tempfile was saved in the correct
            // location and with the correct prefix/suffix.
            f1 = File.createTempFile("harmony-test-FileTest_tempFile2_tf", null, null);
            File dir = new File(base);
            f2 = File.createTempFile("harmony-test-FileTest_tempFile2_tf", ".tmp", dir);
            File currentFile;
            String fileLocation;
            String fileName;
            for (int i = 0; i < 2; i++) {
                currentFile = i == 0 ? f1 : f2;
                fileLocation = addTrailingSlash(currentFile.getParent());
                base = addTrailingSlash(base);
                assertEquals(base, fileLocation, "File not created in the default temporary-file location.");
                fileName = currentFile.getName();
                assertTrue(fileName.endsWith(".tmp"), "File created with incorrect suffix.");
                assertTrue(fileName.startsWith("harmony-test-FileTest_tempFile2_tf"),
                        "File created with incorrect prefix.");
                currentFile.delete();
            }

            // Test for creating a tempfile in a directory that does not exist.
            int dirNumber = 1;
            boolean dirExists = true;
            // Set dir to a non-existent directory inside the temporary
            // directory
            dir = new File(base, String.valueOf(dirNumber));
            // Making sure that the directory does not exist.
            while (dirExists) {
                // If the directory exists, add one to the directory number
                // (making it
                // a new directory name.)
                if (dir.exists()) {
                    dirNumber++;
                    dir = new File(base, String.valueOf(dirNumber));
                } else {
                    dirExists = false;
                }
            }

            try {
                // Try to create a file in a directory that does not exist
                File f3 = File.createTempFile("harmony-test-FileTest_tempFile2_tf", null, dir);
                f3.delete();
                fail("IOException not thrown");
            } catch (IOException e) {
                // Expected
            }
            dir.delete();

            // Tests for creating a tempfile with a filename shorter than 3
            // characters.
            try {
                File f4 = File.createTempFile("ab", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                File f4 = File.createTempFile("a", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
            try {
                File f4 = File.createTempFile("", null, null);
                f4.delete();
                fail("IllegalArgumentException not thrown.");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        } finally {
            if (f1 != null) {
                f1.delete();
            }
            if (f2 != null) {
                f1.delete();
            }
        }
    }

    @Test
    public void delete() throws IOException {
        File dir = new File(tempDirectory, platformId + "filechk");
        dir.mkdir();
        assertTrue(dir.exists(), "Directory does not exist");
        assertTrue(dir.isDirectory(), "Directory is not directory");
        File f = new File(dir, "filechk.tst");
        FileOutputStream fos = new FileOutputStream(f);
        fos.close();
        assertTrue(f.exists(), "Error Creating File For Delete Test");
        dir.delete();
        assertTrue(dir.exists(), "Directory Should Not Have Been Deleted.");
        f.delete();
        assertTrue(!f.exists(), "File Was Not Deleted");
        dir.delete();
        assertTrue(!dir.exists(), "Directory Was Not Deleted");
    }

    @Test
    public void equalsLjava_lang_Object() throws IOException {
        File f1 = new File("filechk.tst");
        File f2 = new File("filechk.tst");
        File f3 = new File("xxxx");

        assertTrue(f1.equals(f2), "Equality test failed");
        assertTrue(!f1.equals(f3), "Files Should Not Return Equal.");

        f3 = new File("FiLeChK.tst");
        boolean onWindows = File.separatorChar == '\\';
        boolean onUnix = File.separatorChar == '/';
        if (onWindows) {
            assertTrue(f1.equals(f3), "Files Should Return Equal.");
        } else if (onUnix) {
            // We can't test it right now, since there are OS with `/` separator but with case-insensitive 
            // file names. Perhaps, we need to support other ways to detect OS.
            //assertTrue("Files Should NOT Return Equal.", !f1.equals(f3));
        }

        f1 = new File(tempDirectory, "casetest.tmp");
        f2 = new File(tempDirectory, "CaseTest.tmp");
        new FileOutputStream(f1).close(); // create the file
        if (f1.equals(f2)) {
            try {
                FileInputStream fis = new FileInputStream(f2);
                fis.close();
            } catch (IOException e) {
                fail("File system is case sensitive");
            }
        } else {
            boolean exception = false;
            try {
                FileInputStream fis = new FileInputStream(f2);
                fis.close();
            } catch (IOException e) {
                exception = true;
            }
            assertTrue(exception, "File system is case insensitive");
        }
        f1.delete();
    }

    @Test
    public void exists() throws IOException {
        File f = new File(tempDirectory, platformId + "exists.tst");
        assertTrue(!f.exists(), "Exists returned true for non-existent file");
        FileOutputStream fos = new FileOutputStream(f);
        fos.close();
        assertTrue(f.exists(), "Exists returned false file");
        f.delete();
    }

    @Test
    public void getAbsoluteFile() {
        String base = addTrailingSlash(tempDirectory.getPath());
        File f = new File(base, "temp.tst");
        File f2 = f.getAbsoluteFile();
        assertEquals(0, f2.compareTo(f.getAbsoluteFile()), "Test 1: Incorrect File Returned.");
        f = new File(base + "Temp" + File.separator + File.separator + "temp.tst");
        f2 = f.getAbsoluteFile();
        assertEquals(0, f2.compareTo(f.getAbsoluteFile()), "Test 2: Incorrect File Returned.");
        f = new File(base + File.separator + ".." + File.separator + "temp.tst");
        f2 = f.getAbsoluteFile();
        assertEquals(0, f2.compareTo(f.getAbsoluteFile()), "Test 3: Incorrect File Returned.");
        f.delete();
        f2.delete();
    }

    @Test
    public void getAbsolutePath() {
        String base = addTrailingSlash(tempDirectory.getPath());
        File f = new File(base, "temp.tst");
        assertEquals(base + "temp.tst", f.getAbsolutePath(), "Test 1: Incorrect Path Returned.");

        f = new File(base + "Temp" + File.separator + File.separator + File.separator + "Testing" + File.separator
                + "temp.tst");
        assertEquals(base + "Temp" + File.separator + "Testing" + File.separator + "temp.tst", f.getAbsolutePath(),
                "Test 2: Incorrect Path Returned.");

        f = new File(base + "a" + File.separator + File.separator + ".." + File.separator + "temp.tst");
        assertEquals(base + "a" + File.separator + ".." + File.separator + "temp.tst", f.getAbsolutePath(),
                "Test 3: Incorrect Path Returned.");
        f.delete();
    }

    @Test
    public void getCanonicalFile() throws IOException {
        String base = addTrailingSlash(tempDirectory.getPath());
        File f = new File(base, "temp.tst");
        File f2 = f.getCanonicalFile();
        assertEquals(0, f2.getCanonicalFile().compareTo(f.getCanonicalFile()), "Test 1: Incorrect File Returned.");
        f = new File(base + "Temp" + File.separator + File.separator + "temp.tst");
        f2 = f.getCanonicalFile();
        assertEquals(0, f2.getCanonicalFile().compareTo(f.getCanonicalFile()), "Test 2: Incorrect File Returned.");
        f = new File(base + "Temp" + File.separator + File.separator + ".." + File.separator + "temp.tst");
        f2 = f.getCanonicalFile();
        assertEquals(0, f2.getCanonicalFile().compareTo(f.getCanonicalFile()), "Test 3: Incorrect File Returned.");

        // Test for when long directory/file names in Windows
        boolean onWindows = File.separatorChar == '\\';
        if (onWindows) {
            File testdir = new File(base, "long-" + platformId);
            testdir.mkdir();
            File dir = new File(testdir, "longdirectory" + platformId);
            try {
                dir.mkdir();
                f = new File(dir, "longfilename.tst");
                f2 = f.getCanonicalFile();
                assertEquals(0, f2.getCanonicalFile().compareTo(f.getCanonicalFile()),
                        "Test 4: Incorrect File Returned.");
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
                f2 = new File(testdir + File.separator + "longdi~1" + File.separator + "longfi~1.tst");
                File canonicalf2 = f2.getCanonicalFile();
                /*
                 * If the "short file name" doesn't exist, then assume that the
                 * 8.3 file name compatibility is disabled.
                 */
                if (canonicalf2.exists()) {
                    assertTrue(canonicalf2.compareTo(f.getCanonicalFile()) == 0,
                            "Test 5: Incorrect File Returned: " + canonicalf2);
                }
            } finally {
                f.delete();
                f2.delete();
                dir.delete();
                testdir.delete();
            }
        }
    }

    @Test
    public void getCanonicalPath() throws IOException {
        // Should work for Unix/Windows.
        String dots = "..";
        String base = tempDirectory.getCanonicalPath();
        base = addTrailingSlash(base);
        File f = new File(base, "temp.tst");
        assertEquals(base + "temp.tst", f.getCanonicalPath(), "Test 1: Incorrect Path Returned.");
        f = new File(base + "Temp" + File.separator + dots + File.separator + "temp.tst");
        assertEquals(base + "temp.tst", f.getCanonicalPath(), "Test 2: Incorrect Path Returned.");

        // Finding a non-existent directory for tests 3 and 4
        // This is necessary because getCanonicalPath is case sensitive and
        // could cause a failure in the test if the directory exists but with
        // different case letters (e.g "Temp" and "temp")
        int dirNumber = 1;
        boolean dirExists = true;
        File dir1 = new File(base, String.valueOf(dirNumber));
        while (dirExists) {
            if (dir1.exists()) {
                dirNumber++;
                dir1 = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }
        f = new File(base + dirNumber + File.separator + dots + File.separator + dirNumber
                + File.separator + "temp.tst");
        assertEquals(base + dirNumber + File.separator + "temp.tst", f.getCanonicalPath(),
                "Test 3: Incorrect Path Returned.");
        f = new File(base + dirNumber + File.separator + "Temp" + File.separator + dots + File.separator
                + "Test" + File.separator + "temp.tst");
        assertEquals(base + dirNumber + File.separator + "Test" + File.separator + "temp.tst", f.getCanonicalPath(),
                "Test 4: Incorrect Path Returned.");

        f = new File(base + "1234.567");
        assertEquals(base + "1234.567", f.getCanonicalPath(), "Test 5: Incorrect Path Returned.");

        // Test for long file names on Windows
        boolean onWindows = File.separatorChar == '\\';
        if (onWindows) {
            File testdir = new File(base, "long-" + platformId);
            testdir.mkdir();
            File f1 = new File(testdir, "longfilename" + platformId + ".tst");
            FileOutputStream fos = new FileOutputStream(f1);
            File f2 = null;
            File f3 = null;
            File dir2 = null;
            try {
                fos.close();
                String dirName1 = f1.getCanonicalPath();
                File f4 = new File(testdir, "longfi~1.tst");
                /*
                 * If the "short file name" doesn't exist, then assume that the
                 * 8.3 file name compatibility is disabled.
                 */
                if (f4.exists()) {
                    String dirName2 = f4.getCanonicalPath();
                    assertEquals(dirName1, dirName2, "Test 6: Incorrect Path Returned.");
                    dir2 = new File(testdir, "longdirectory" + platformId);
                    if (!dir2.exists()) {
                        assertTrue(dir2.mkdir(), "Could not create dir: " + dir2);
                    }
                    f2 = new File(testdir.getPath() + File.separator + "longdirectory"
                            + platformId + File.separator + "Test" + File.separator + dots
                            + File.separator + "longfilename.tst");
                    FileOutputStream fos2 = new FileOutputStream(f2);
                    fos2.close();
                    dirName1 = f2.getCanonicalPath();
                    f3 = new File(testdir.getPath() + File.separator + "longdi~1"
                            + File.separator + "Test" + File.separator + dots + File.separator
                            + "longfi~1.tst");
                    dirName2 = f3.getCanonicalPath();
                    assertEquals(dirName1, dirName2, "Test 7: Incorrect Path Returned.");
                }
            } finally {
                f1.delete();
                if (f2 != null) {
                    f2.delete();
                }
                if (dir2 != null) {
                    dir2.delete();
                }
                testdir.delete();
            }
        }
    }

    @Test
    public void getName() {
        File f = new File("name.tst");
        assertEquals("name.tst", f.getName(), "Test 1: Returned incorrect name");

        f = new File("");
        assertEquals("", f.getName(), "Test 2: Returned incorrect name");

        f.delete();
    }

    @Test
    public void getParent() {
        File f = new File("p.tst");
        assertNull(f.getParent(), "Incorrect path returned");
        f = new File(System.getProperty("user.home"), "p.tst");
        assertEquals(System.getProperty("user.home"), f.getParent(), "Incorrect path returned");
        f.delete();

        File f1 = new File("/directory");
        assertEquals(File.separator, f1.getParent(), "Wrong parent test 1");
        f1 = new File("/directory/file");
        assertEquals(File.separator + "directory", f1.getParent(), "Wrong parent test 2");
        f1 = new File("directory/file");
        assertEquals("directory", f1.getParent(), "Wrong parent test 3");
        f1 = new File("/");
        assertNull(f1.getParent(), "Wrong parent test 4");
        f1 = new File("directory");
        assertNull(f1.getParent(), "Wrong parent test 5");

        if (File.separatorChar == '\\' && new File("d:/").isAbsolute()) {
            f1 = new File("d:/directory");
            assertEquals("d:" + File.separator, f1.getParent(), "Wrong parent test 1a");
            f1 = new File("d:/directory/file");
            assertEquals("d:" + File.separator + "directory", f1.getParent(), "Wrong parent test 2a");
            f1 = new File("d:directory/file");
            assertEquals("d:directory", f1.getParent(), "Wrong parent test 3a");
            f1 = new File("d:/");
            assertNull(f1.getParent(), "Wrong parent test 4a");
        }
    }

    @Test
    public void getParentFile() {
        File f = new File("tempfile.tst");
        assertNull(f.getParentFile(), "Incorrect path returned");
        f = new File(tempDirectory, "tempfile1.tmp");
        File f2 = new File(tempDirectory, "tempfile2.tmp");
        File f3 = new File(tempDirectory, "/a/tempfile.tmp");
        assertEquals(0, f.getParentFile().compareTo(f2.getParentFile()), "Incorrect File Returned");
        assertTrue(f.getParentFile().compareTo(f3.getParentFile()) != 0, "Incorrect File Returned");
        f.delete();
        f2.delete();
        f3.delete();
    }

    @Test
    public void getPath() {
        String base = System.getProperty("user.home");
        String fname;
        File f1;
        if (!base.regionMatches(base.length() - 1, File.separator, 0, 1)) {
            base += File.separator;
        }
        fname = base + "filechk.tst";
        f1 = new File(base, "filechk.tst");
        File f2 = new File("filechk.tst");
        File f3 = new File("c:");
        File f4 = new File(base + "a" + File.separator + File.separator + ".." + File.separator + "filechk.tst");
        assertEquals(fname, f1.getPath(), "getPath returned incorrect path(f1)");
        assertEquals("filechk.tst", f2.getPath(), "getPath returned incorrect path(f2)");
        assertEquals("c:", f3.getPath(), "getPath returned incorrect path(f3)");
        assertEquals(base + "a" + File.separator + ".." + File.separator + "filechk.tst", f4.getPath(),
                "getPath returned incorrect path(f4)");
        f1.delete();
        f2.delete();
        f3.delete();
        f4.delete();

        // Regression for HARMONY-444
        File file;
        String separator = File.separator;

        file = new File((File) null, "x/y/z");
        assertEquals("x" + separator + "y" + separator + "z", file.getPath());

        file = new File((String) null, "x/y/z");
        assertEquals("x" + separator + "y" + separator + "z", file.getPath());

        // Regression for HARMONY-829
        String f1ParentName = "01";
        f1 = new File(f1ParentName, "");
        assertEquals(f1ParentName, f1.getPath());

        String f2ParentName = "0";
        f2 = new File(f2ParentName, "");

        assertEquals(-1, f2.compareTo(f1));
        assertEquals(1, f1.compareTo(f2));

        File parent = tempDirectory;
        f3 = new File(parent, "");

        assertEquals(parent.getPath(), f3.getPath());

        // Regression for HARMONY-3869
        File file1 = new File("", "");
        assertEquals(File.separator, file1.getPath());

        File file2 = new File(new File(""), "");
        assertEquals(File.separator, file2.getPath());
    }

    @Test
    public void test_hashCode() {
        // Regression for HARMONY-53
        File mfile = new File("SoMe FiLeNaMe"); // Mixed case
        File lfile = new File("some filename"); // Lower case

        if (mfile.equals(lfile)) {
            assertTrue(mfile.hashCode() == lfile.hashCode(), "Assert 0: wrong hashcode");
        } else {
            assertFalse(mfile.hashCode() == lfile.hashCode(), "Assert 1: wrong hashcode");
        }
    }

    @Test
    public void isAbsolute() {
        if (File.separatorChar == '\\') {
            File f = new File("c:\\test");
            File f1 = new File("\\test");
            // One or the other should be absolute on Windows or CE
            assertTrue((f.isAbsolute() && !f1.isAbsolute()) || (!f.isAbsolute() && f1.isAbsolute()),
                    "Absolute returned false");

            assertTrue(new File("C:/").isAbsolute());
            assertTrue(new File("f:/").isAbsolute());
            assertTrue(new File("f:\\").isAbsolute());
            assertFalse(new File("f:").isAbsolute());
            assertFalse(new File("K:").isAbsolute());
        } else {
            File f = new File("/test");
            File f1 = new File("\\test");
            assertTrue(f.isAbsolute(), "Absolute returned false");
            assertFalse(f1.isAbsolute(), "Absolute returned true");
            assertTrue(new File("//test").isAbsolute());
            assertFalse(new File("test").isAbsolute());
            assertFalse(new File("c:/").isAbsolute());
            assertFalse(new File("c:\\").isAbsolute());
            assertFalse(new File("c:").isAbsolute());
            assertFalse(new File("\\").isAbsolute());
            assertFalse(new File("\\\\").isAbsolute());
        }
        assertTrue(!new File("../test").isAbsolute(), "Non-Absolute returned true");
    }

    @Test
    public void isDirectory() {
        String base = addTrailingSlash(tempDirectory.getPath());
        File f = new File(base);
        assertTrue(f.isDirectory(), "Test 1: Directory Returned False");
        f = new File(base + "zxzxzxz" + platformId);
        assertTrue(!f.isDirectory(), "Test 2: (Not Created) Directory Returned True.");
        f.mkdir();
        try {
            assertTrue(f.isDirectory(), "Test 3: Directory Returned False.");
        } finally {
            f.delete();
        }
    }

    @Test
    public void isFile() throws IOException {
        String base = tempDirectory.getPath();
        File f = new File(base);
        assertFalse(f.isFile(), "Directory Returned True As Being A File.");
        
        base = addTrailingSlash(base);
        f = new File(base, platformId + "amiafile");
        assertTrue(!f.isFile(), "Non-existent File Returned True");
        FileOutputStream fos = new FileOutputStream(f);
        fos.close();
        assertTrue(f.isFile(), "File returned false");
        f.delete();
    }

    @Test
    public void lastModified() throws IOException {
        File f = new File(System.getProperty("java.io.tmpdir"), platformId + "lModTest.tst");
        f.delete();
        long lastModifiedTime = f.lastModified();
        assertEquals(0, lastModifiedTime, "LastModified Time Should Have Returned 0.");
        FileOutputStream fos = new FileOutputStream(f);
        fos.close();
        f.setLastModified(315550800000L);
        lastModifiedTime = f.lastModified();
        assertEquals(315550800000L, lastModifiedTime, "LastModified Time Incorrect");
        f.delete();
    }

    @Test
    public void length() throws IOException {
        File f = new File(tempDirectory, platformId + "input.tst");
        assertEquals(0, f.length(), "File Length Should Have Returned 0.");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(fileString.getBytes());
        fos.close();
        assertEquals(fileString.length(), f.length(), "Incorrect file length returned");
        f.delete();

        // regression test for HARMONY-1497
        f = File.createTempFile("test", "tmp");
        f.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(f, "rwd");
        raf.write(0x41);
        assertEquals(1, f.length());
    }

    @Test
    public void list() throws IOException {
        String base = tempDirectory.getPath();
        // Old test left behind "garbage files" so this time it creates a
        // directory that is guaranteed not to already exist (and deletes it
        // afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = null;
        dir = new File(base, platformId + String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        String[] flist = dir.list();

        assertNull(flist, "Method list() Should Have Returned null.");

        assertTrue(dir.mkdir(), "Could not create parent directory for list test");

        String[] files = { "mtzz1.xx", "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
        try {
            assertEquals(Collections.emptyList(), Arrays.asList(dir.list()),
                    "Method list() Should Have Returned An Array Of Length 0.");

            File file = new File(dir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(file.list(),
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.");
            } finally {
                file.delete();
            }

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }

            flist = dir.list();
            if (flist.length != files.length) {
                fail("Incorrect list returned");
            }

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++) {
                check[i] = false;
            }
            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].equals(files[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (!check[i]) {
                    checkCount++;
                }
            }
            assertEquals(0, checkCount, "Invalid file returned in listing");

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }

            assertTrue(dir.delete(), "Could not delete parent directory for list test.");
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void listFiles() throws IOException, InterruptedException {
        String base = tempDirectory.getPath();
        // Finding a non-existent directory to create.
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, platformId + String.valueOf(dirNumber));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number
            // (making it a new directory name.)
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }
        // Test for attempting to call listFiles on a non-existent directory.
        assertNull(dir.listFiles(), "listFiles Should Return Null.");

        assertTrue(dir.mkdir(), "Failed To Create Parent Directory.");

        String[] files = { "1.tst", "2.tst", "3.tst", "" };
        try {
            assertEquals(0, dir.listFiles().length, "listFiles Should Return An Array Of Length 0.");

            File file = new File(dir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(file.listFiles(),
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.");
            } finally {
                file.delete();
            }

            for (int i = 0; i < (files.length - 1); i++) {
                File f = new File(dir, files[i]);
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }

            new File(dir, "doesNotExist.tst");
            File[] flist = dir.listFiles();

            // Test to make sure that only the 3 files that were created are
            // listed.
            assertEquals(3, flist.length, "Incorrect Number Of Files Returned.");

            // Test to make sure that listFiles can read hidden files.
            boolean onWindows = File.separatorChar == '\\';
            if (!isTeaVM() && onWindows) {
                files[3] = "4.tst";
                File f = new File(dir, "4.tst");
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
                Runtime r = Runtime.getRuntime();
                Process p = r.exec("attrib +h \"" + f.getPath() + "\"");
                p.waitFor();
            } else {
                files[3] = ".4.tst";
                File f = new File(dir, ".4.tst");
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }
            flist = dir.listFiles();
            assertEquals(4, flist.length, "Incorrect Number Of Files Returned.");

            // Checking to make sure the correct files were are listed in
            // the array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++) {
                check[i] = false;
            }
            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].getName().equals(files[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (!check[i]) {
                    checkCount++;
                }
            }
            assertEquals(0, checkCount, "Invalid file returned in listing");

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            assertTrue(dir.delete(), "Parent Directory Not Deleted.");
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void listFilesLjava_io_FileFilter() throws IOException {
        String base = System.getProperty("java.io.tmpdir");
        // Finding a non-existent directory to create.
        int dirNumber = 1;
        boolean dirExists = true;
        File baseDir = new File(base, platformId + String.valueOf(dirNumber));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number (making
            // it a new directory name.)
            if (baseDir.exists()) {
                dirNumber++;
                baseDir = new File(base, platformId + String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        // Creating a filter that catches directories.
        FileFilter dirFilter = f -> f.isDirectory();

        assertNull(baseDir.listFiles(dirFilter), "listFiles Should Return Null.");

        assertTrue(baseDir.mkdir(), "Failed To Create Parent Directory.");

        File dir1 = null;
        String[] files = { "1.tst", "2.tst", "3.tst" };
        try {
            assertEquals(0, baseDir.listFiles(dirFilter).length, "listFiles Should Return An Array Of Length 0.");

            File file = new File(baseDir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(file.listFiles(dirFilter),
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.");
            } finally {
                file.delete();
            }

            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir, files[i]);
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }
            dir1 = new File(baseDir, "Temp1");
            dir1.mkdir();

            // Creating a filter that catches files.
            FileFilter fileFilter = f -> f.isFile();

            // Test to see if the correct number of directories are returned.
            File[] directories = baseDir.listFiles(dirFilter);
            assertEquals(1, directories.length, "Incorrect Number Of Directories Returned.");

            // Test to see if the directory was saved with the correct name.
            assertEquals(0, directories[0].compareTo(dir1), "Incorrect Directory Returned.");

            // Test to see if the correct number of files are returned.
            File[] flist = baseDir.listFiles(fileFilter);
            assertEquals(files.length, flist.length, "Incorrect Number Of Files Returned.");

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++) {
                check[i] = false;
            }
            for (int i = 0; i < files.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].getName().equals(files[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (!check[i]) {
                    checkCount++;
                }
            }
            assertEquals(0, checkCount, "Invalid file returned in listing");

            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir, files[i]);
                f.delete();
            }
            dir1.delete();
            assertTrue(baseDir.delete(), "Parent Directory Not Deleted.");
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(baseDir, files[i]);
                f.delete();
            }
            if (dir1 != null) {
                dir1.delete();
            }
            baseDir.delete();
        }
    }

    @Test
    public void listFilesLjava_io_FilenameFilter() throws IOException {
        String base = System.getProperty("java.io.tmpdir");
        // Finding a non-existent directory to create.
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, platformId + String.valueOf(dirNumber));
        // Making sure that the directory does not exist.
        while (dirExists) {
            // If the directory exists, add one to the directory number (making
            // it a new directory name.)
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, platformId + String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        // Creating a filter that catches "*.tst" files.
        FilenameFilter tstFilter = (f, fileName) -> fileName.endsWith(".tst");

        assertNull(dir.listFiles(tstFilter), "listFiles Should Return Null.");

        assertTrue(dir.mkdir(), "Failed To Create Parent Directory.");

        String[] files = { "1.tst", "2.tst", "3.tmp" };
        try {
            assertEquals(0, dir.listFiles(tstFilter).length, "listFiles Should Return An Array Of Length 0.");

            File file = new File(dir, "notADir.tst");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                assertNull(file.listFiles(tstFilter),
                        "listFiles Should Have Returned Null When Used On A File Instead Of A Directory.");
            } finally {
                file.delete();
            }

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }

            // Creating a filter that catches "*.tmp" files.
            FilenameFilter tmpFilter = (f, fileName) -> fileName.endsWith(".tmp");

            // Tests to see if the correct number of files were returned.
            File[] flist = dir.listFiles(tstFilter);
            assertEquals(2, flist.length, "Incorrect Number Of Files Passed Through tstFilter.");
            for (int i = 0; i < flist.length; i++) {
                assertTrue(flist[i].getPath().endsWith(".tst"), "File Should Not Have Passed The tstFilter.");
            }

            flist = dir.listFiles(tmpFilter);
            assertEquals(1, flist.length, "Incorrect Number Of Files Passed Through tmpFilter.");
            assertTrue(flist[0].getPath().endsWith(".tmp"), "File Should Not Have Passed The tmpFilter.");

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            assertTrue(dir.delete(), "Parent Directory Not Deleted.");
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void listLjava_io_FilenameFilter() throws IOException {
        String base = tempDirectory.getPath();
        // Old test left behind "garbage files" so this time it creates a
        // directory that is guaranteed not to already exist (and deletes it
        // afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, platformId + String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        FilenameFilter filter = (dir1, name) -> !name.equals("mtzz1.xx");

        String[] flist = dir.list(filter);
        assertNull(flist, "Method list(FilenameFilter) Should Have Returned Null.");

        assertTrue(dir.mkdir(), "Could not create parent directory for test");

        String[] files = { "mtzz1.xx", "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
        try {
            /*
             * Do not return null when trying to use list(Filename Filter) on a
             * file rather than a directory. All other "list" methods return
             * null for this test case.
             */
            /*
             * File file = new File(dir, "notADir.tst"); try { FileOutputStream
             * fos = new FileOutputStream(file); fos.close(); } catch
             * (IOException e) { fail("Unexpected IOException During Test."); }
             * flist = dir.list(filter); assertNull("listFiles Should Have
             * Returned Null When Used On A File Instead Of A Directory.",
             * flist); file.delete();
             */

            flist = dir.list(filter);
            assertEquals(0, flist.length, "Array Of Length 0 Should Have Returned.");

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                FileOutputStream fos = new FileOutputStream(f);
                fos.close();
            }

            flist = dir.list(filter);

            assertEquals(flist.length, files.length - 1, "Incorrect list returned");

            // Checking to make sure the correct files were are listed in the
            // array.
            boolean[] check = new boolean[flist.length];
            for (int i = 0; i < check.length; i++) {
                check[i] = false;
            }
            String[] wantedFiles = { "mtzz2.xx", "mtzz3.yy", "mtzz4.yy" };
            for (int i = 0; i < wantedFiles.length; i++) {
                for (int j = 0; j < flist.length; j++) {
                    if (flist[j].equals(wantedFiles[i])) {
                        check[i] = true;
                        break;
                    }
                }
            }
            int checkCount = 0;
            for (int i = 0; i < check.length; i++) {
                if (!check[i]) {
                    checkCount++;
                }
            }
            assertEquals(0, checkCount, "Invalid file returned in listing");

            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            assertTrue(dir.delete(), "Could not delete parent directory for test.");
        } finally {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                f.delete();
            }
            dir.delete();
        }
    }

    @Test
    public void listRoots() {
        File[] roots = File.listRoots();
        boolean onUnix = File.separatorChar == '/';
        boolean onWindows = File.separatorChar == '\\';
        if (onUnix) {
            assertEquals(1, roots.length, "Incorrect Number Of Root Directories.");
            String fileLoc = roots[0].getPath();
            assertTrue(fileLoc.startsWith(File.separator), "Incorrect Root Directory Returned.");
        } else if (onWindows) {
            // Need better test for Windows
            assertTrue(roots.length > 0, "Incorrect Number Of Root Directories.");
        }
    }

    @Test
    public void mkdir() throws IOException {
        String base = tempDirectory.getPath();
        // Old test left behind "garbage files" so this time it creates a
        // directory that is guaranteed not to already exist (and deletes it
        // afterward.)
        int dirNumber = 1;
        boolean dirExists = true;
        File dir = new File(base, String.valueOf(dirNumber));
        while (dirExists) {
            if (dir.exists()) {
                dirNumber++;
                dir = new File(base, String.valueOf(dirNumber));
            } else {
                dirExists = false;
            }
        }

        assertTrue(dir.mkdir(), "mkdir failed");
        assertTrue(dir.exists(), "mkdir worked but exists check failed");
        dir.deleteOnExit();

        String longDirName = "abcdefghijklmnopqrstuvwx"; // 24 chars
        String newbase = dir + File.separator;
        StringBuilder sb = new StringBuilder(dir + File.separator);
        StringBuilder sb2 = new StringBuilder(dir + File.separator);

        // Test make a long path
        while (dir.getCanonicalPath().length() < 200 - longDirName.length()) {
            sb.append(longDirName + File.separator);
            dir = new File(sb.toString());
            assertTrue(dir.mkdir(), "mkdir failed");
            assertTrue(dir.exists(), "mkdir worked but exists check failed");
            dir.deleteOnExit();
        }

        while (dir.getCanonicalPath().length() < 200) {
            sb.append(0);
            dir = new File(sb.toString());
            assertTrue(dir.mkdir(), "mkdir " + dir.getCanonicalPath() + " failed");
            assertTrue(dir.exists(), "mkdir " + dir.getCanonicalPath().length() + " worked but exists check failed");
            dir.deleteOnExit();
        }
        dir = new File(sb2.toString());
        // Test make many paths
        while (dir.getCanonicalPath().length() < 200) {
            sb2.append(0);
            dir = new File(sb2.toString());
            assertTrue(dir.mkdir(), "mkdir " + dir.getCanonicalPath().length() + " failed");
            assertTrue(dir.exists(), "mkdir " + dir.getCanonicalPath().length() + " worked but exists check failed");
            dir.deleteOnExit();
        }

        // Regression test for HARMONY-3656
        String[] ss = { "dir\u3400", "abc", "abc@123", "!@#$%^&",
                "~\u4E00!\u4E8C@\u4E09$", "\u56DB\u4E94\u516D",
                "\u4E03\u516B\u4E5D" };
        for (int i = 0; i < ss.length; i++) {
            dir = new File(newbase, ss[i]);
            assertTrue(dir.mkdir(), "mkdir " + dir.getCanonicalPath() + " failed");
            assertTrue(dir.exists(), "mkdir " + dir.getCanonicalPath() + " worked but exists check failed");
            dir.deleteOnExit();
        }
    }

    @Test
    public void mkdir_special_unicode() {
        File specialDir = new File(this.tempDirectory, "\u5C73");
        int i = 0;
        while (specialDir.exists()) {
            specialDir = new File("\u5C73" + i);
            ++i;
        }
        assertFalse(specialDir.exists());
        assertTrue(specialDir.mkdir());
        assertTrue(specialDir.exists());
    }

    @Test
    public void test_mkdirs() {
        String userHome = addTrailingSlash(tempDirectory.getPath());
        File f = new File(userHome + "mdtest" + platformId + File.separator + "mdtest2", "p.tst");
        File g = new File(userHome + "mdtest" + platformId + File.separator + "mdtest2");
        File h = new File(userHome + "mdtest" + platformId);
        f.mkdirs();
        try {
            assertTrue(h.exists(), "Base Directory not created");
            assertTrue(g.exists(), "Directories not created");
            assertTrue(f.exists(), "File not created");
        } finally {
            f.delete();
            g.delete();
            h.delete();
        }
    }

    @Test
    public void renameToLjava_io_File() throws IOException {
        String base = tempDirectory.getPath();
        File dir = new File(base, platformId);
        dir.mkdir();
        File f = new File(dir, "xxx.xxx");
        File rfile = new File(dir, "yyy.yyy");
        File f2 = new File(dir, "zzz.zzz");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(fileString.getBytes());
            fos.close();
            long lengthOfFile = f.length();

            rfile.delete(); // in case it already exists

            assertTrue(f.renameTo(rfile), "Test 1: File Rename Failed");
            assertTrue(rfile.exists(), "Test 2: File Rename Failed.");
            assertEquals(lengthOfFile, rfile.length(), "Test 3: Size Of File Changed.");

            fos = new FileOutputStream(rfile);
            fos.close();

            f2.delete(); // in case it already exists
            assertTrue(rfile.renameTo(f2), "Test 4: File Rename Failed");
            assertTrue(f2.exists(), "Test 5: File Rename Failed.");
        } finally {
            f.delete();
            rfile.delete();
            f2.delete();
            dir.delete();
        }
    }

    @Test
    public void setLastModifiedJ() throws IOException {
        File f1 = null;
        try {
            f1 = File.createTempFile("tmp", "tmp");
            long orgTime = f1.lastModified();
            // Subtracting 100 000 milliseconds from the orgTime of File f1
            f1.setLastModified(orgTime - 100000);
            long lastModified = f1.lastModified();
            assertEquals(orgTime - 100000, lastModified, "Test 1: LastModifed time incorrect");
            // Subtracting 10 000 000 milliseconds from the orgTime of File f1
            f1.setLastModified(orgTime - 10000000);
            lastModified = f1.lastModified();
            assertEquals(orgTime - 10000000, lastModified, "Test 2: LastModifed time incorrect");
            // Adding 100 000 milliseconds to the orgTime of File f1
            f1.setLastModified(orgTime + 100000);
            lastModified = f1.lastModified();
            assertEquals(orgTime + 100000, lastModified, "Test 3: LastModifed time incorrect");
            // Adding 10 000 000 milliseconds from the orgTime of File f1
            f1.setLastModified(orgTime + 10000000);
            lastModified = f1.lastModified();
            assertEquals(orgTime + 10000000, lastModified, "Test 4: LastModifed time incorrect");
            // Trying to set time to an exact number
            f1.setLastModified(315550800000L);
            lastModified = f1.lastModified();
            assertEquals(315550800000L, lastModified, "Test 5: LastModified time incorrect");
            String osName = System.getProperty("os.name", "unknown");
            if (osName.equals("Windows 2000") || osName.equals("Windows NT")) {
                // Trying to set time to a large exact number
                boolean result = f1.setLastModified(4354837199000L);
                long next = f1.lastModified();
                // Dec 31 23:59:59 EST 2107 is overflow on FAT file systems, and
                // the call fails
                if (result) {
                    assertEquals(4354837199000L, next, "Test 6: LastModified time incorrect");
                }
            }
            // Trying to set time to a negative number
            try {
                f1.setLastModified(-25);
                fail("IllegalArgumentException Not Thrown.");
            } catch (IllegalArgumentException e) {
                // As expected
            }
        } finally {
            if (f1 != null) {
                f1.delete();
            }
        }
    }

    @Test
    public void setReadOnly() throws IOException {
        File f1 = null;
        File f2 = null;
        try {
            f1 = File.createTempFile("harmony-test-FileTest_setReadOnly", ".tmp");
            f2 = File.createTempFile("harmony-test-FileTest_setReadOnly", ".tmp");
            // Assert is flawed because canWrite does not work.
            // assertTrue("File f1 Is Set To ReadOnly." , f1.canWrite());
            f1.setReadOnly();
            // Assert is flawed because canWrite does not work.
            // assertTrue("File f1 Is Not Set To ReadOnly." , !f1.canWrite());
            try {
                // Attempt to write to a file that is setReadOnly.
                new FileOutputStream(f1);
                fail("IOException not thrown.");
            } catch (IOException e) {
                // Expected
            }

            // Assert is flawed because canWrite does not work.
            // assertTrue("File f2 Is Set To ReadOnly." , f2.canWrite());
            FileOutputStream fos = new FileOutputStream(f2);
            // Write to a file.
            fos.write(fileString.getBytes());
            fos.close();
            f2.setReadOnly();
            // Assert is flawed because canWrite does not work.
            // assertTrue("File f2 Is Not Set To ReadOnly." , !f2.canWrite());
            try {
                // Attempt to write to a file that has previously been written
                // to.
                // and is now set to read only.
                new FileOutputStream(f2);
                fail("IOException not thrown.");
            } catch (IOException e) {
                // Expected
            }

            if (File.separatorChar == '/') {
                f2.setReadOnly();
                assertTrue(f2.delete(), "File f2 Did Not Delete");
                // Similarly, trying to delete a read-only directory should succeed
                f2 = new File(tempDirectory, "deltestdir");
                f2.mkdir();
                f2.setReadOnly();
                assertTrue(f2.delete(), "Directory f2 Did Not Delete");
                assertTrue(!f2.exists(), "Directory f2 Did Not Delete");
            }
        } finally {
            if (f1 != null) {
                f1.delete();
            }
            if (f2 != null) {
                f2.delete();
            }
        }
    }

    @Test
    public void test_toString() {
        String fileName = System.getProperty("user.home") + File.separator + "input.tst";
        if (fileName.startsWith("//")) {
            fileName = fileName.substring(1);
        }
        File f = new File(fileName);
        assertEquals(fileName, f.toString(), "Incorrect string returned");

        if (File.separatorChar == '\\') {
            String result = new File("c:\\").toString();
            assertEquals("c:\\", result, "Removed backslash");
        }
    }

    @Test
    public void toURI() throws URISyntaxException {
        // Need a directory that exists
        File dir = tempDirectory;

        // Test for toURI when the file is a directory.
        String newURIPath = dir.getAbsolutePath();
        newURIPath = newURIPath.replace(File.separatorChar, '/');
        if (!newURIPath.startsWith("/")) {
            newURIPath = "/" + newURIPath;
        }
        if (!newURIPath.endsWith("/")) {
            newURIPath += '/';
        }

        URI uri = dir.toURI();
        assertEquals(dir.getAbsoluteFile(), new File(uri), "Test 1A: Incorrect URI Returned.");
        assertEquals(new URI("file", null, newURIPath, null, null), uri, "Test 1B: Incorrect URI Returned.");

        // Test for toURI with a file name with illegal chars.
        File f = new File(dir, "te% \u20ac st.tst");
        newURIPath = f.getAbsolutePath();
        newURIPath = newURIPath.replace(File.separatorChar, '/');
        if (!newURIPath.startsWith("/")) {
            newURIPath = "/" + newURIPath;
        }

        uri = f.toURI();
        assertEquals(f.getAbsoluteFile(), new File(uri), "Test 2A: Incorrect URI Returned.");
        assertEquals(new URI("file", null, newURIPath, null, null), uri, "Test 2B: Incorrect URI Returned.");

        // Regression test for HARMONY-3207
        dir = new File(""); // current directory
        uri = dir.toURI();
        assertTrue(uri .toString().endsWith("/"), "Test current dir: URI does not end with slash.");
    }

    @Test
    public void toURI2() throws URISyntaxException {
        File f = new File(tempDirectory, "a/b/c/../d/e/./f");

        String path = f.getAbsolutePath();
        path = path.replace(File.separatorChar, '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        URI uri1 = new URI("file", null, path, null);
        URI uri2 = f.toURI();
        assertEquals(uri1, uri2, "uris not equal");
    }

    @Test
    public void list_withUnicodeFileName() {
        File rootDir = new File("P");
        if (!rootDir.exists()) {
            rootDir.mkdir();
            rootDir.deleteOnExit();
        }

        String dirName = "src\u3400";
        File dir = new File(rootDir, dirName);
        if (!dir.exists()) {
            dir.mkdir();
            dir.deleteOnExit();
        }
        boolean exist = false;
        String[] fileNames = rootDir.list();
        for (String fileName : fileNames) {
            if (dirName.equals(fileName)) {
                exist = true;
                break;
            }
        }
        assertTrue(exist);
    }

    @PlatformMarker
    private static boolean isTeaVM() {
        return false;
    }
}
