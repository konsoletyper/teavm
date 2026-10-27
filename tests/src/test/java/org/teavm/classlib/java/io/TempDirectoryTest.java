/*
 *  Copyright 2026 Ethan Resnick.
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.runtime.fs.VirtualFileSystemProvider;
import org.teavm.runtime.fs.memory.InMemoryVirtualFileSystem;

// File.createTempFile: "If the directory argument is null then the system-dependent default temporary-file directory
// will be used. The default temporary-file directory is specified by the system property java.io.tmpdir."
// Files.createTempFile(String, String, FileAttribute...) and Files.createTempDirectory(String, FileAttribute...)
// create their file "in the default temporary-file directory".
@TeaVMTest
// The JDK reads java.io.tmpdir once, at startup, so System.setProperty does not move its temporary files.
@SkipJVM
@EachTestCompiledSeparately
public class TempDirectoryTest {
    // The default file system keeps the VirtualFileSystem that it finds first, and the system properties take
    // their defaults once. So this test installs its file system before any use of either, in its own program.
    @Test
    public void tempFilesGoToJavaIoTmpdir() throws IOException {
        VirtualFileSystemProvider.setInstance(new InMemoryVirtualFileSystem() {
            @Override
            public String tempDirectory() {
                return "/fake-temp";
            }
        });
        Files.createDirectories(Path.of("/fake-temp"));
        Files.createDirectories(Path.of("/other-temp"));

        assertEquals("/fake-temp", System.getProperty("java.io.tmpdir"));
        assertTempFilesIn("/fake-temp");

        System.setProperty("java.io.tmpdir", "/other-temp");
        assertTempFilesIn("/other-temp");

        // The default shows through again once the program's own value is gone.
        System.clearProperty("java.io.tmpdir");
        assertEquals("/fake-temp", System.getProperty("java.io.tmpdir"));
        assertTempFilesIn("/fake-temp");
    }

    // Programmatic changes to java.io.tmpdir "are not guaranteed to have any effect" in the JDK. A TeaVM program has
    // no command line to set the property on, so TeaVM applies them, to every later call.
    @Test
    public void tempFilesFollowJavaIoTmpdir() throws IOException {
        var defaultDir = System.getProperty("java.io.tmpdir");
        // The in-memory file system of JS and Wasm GC starts empty. On C the directory exists, and on macOS /tmp is
        // a symbolic link, which createDirectories does not accept there.
        if (!Files.isDirectory(Path.of(defaultDir))) {
            Files.createDirectories(Path.of(defaultDir));
        }
        var base = Files.createTempDirectory("teavm-tmpdir-test").toString();
        var dirs = List.of(base, base + "/nested/deeper", base + "/with space", base + "/trailing/", base);
        try {
            for (var dir : dirs) {
                Files.createDirectories(Path.of(dir));
                System.setProperty("java.io.tmpdir", dir);
                assertTempFilesIn(dir);
            }

            // A directory given explicitly wins over the property.
            var explicit = new File(base, "explicit");
            explicit.mkdir();
            System.setProperty("java.io.tmpdir", base + "/nested");
            var file = File.createTempFile("abc", null, explicit);
            var path = Files.createTempFile(Path.of(explicit.getPath()), "abc", null);
            var tempDir = Files.createTempDirectory(Path.of(explicit.getPath()), "abc");
            assertEquals(explicit.getPath(), file.getParent());
            assertEquals(Path.of(explicit.getPath()), path.getParent());
            assertEquals(Path.of(explicit.getPath()), tempDir.getParent());
        } finally {
            System.setProperty("java.io.tmpdir", defaultDir);
            deleteTree(new File(base));
        }
        assertTempFilesIn(defaultDir);
    }

    private static void assertTempFilesIn(String dir) throws IOException {
        var file = File.createTempFile("abc", ".txt");
        var path = Files.createTempFile("abc", null);
        var tempDir = Files.createTempDirectory("abc");
        try {
            assertEquals(new File(dir).getPath(), file.getParent());
            assertEquals(Path.of(dir), path.getParent());
            assertEquals(Path.of(dir), tempDir.getParent());
        } finally {
            file.delete();
            Files.delete(path);
            Files.delete(tempDir);
        }
    }

    private static void deleteTree(File file) {
        var children = file.listFiles();
        if (children != null) {
            for (var child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
