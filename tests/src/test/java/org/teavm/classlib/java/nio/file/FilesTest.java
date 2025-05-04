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
package org.teavm.classlib.java.nio.file;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform(TestPlatform.WASI)
public class FilesTest {
    @Test
    public void inputStream() throws IOException {
        var file = new File("test-file");
        var dir = new File("test-dir");
        try {
            try (var output = new FileOutputStream(file)) {
                output.write(1);
                output.write(2);
                output.write(3);
            }

            dir.mkdirs();
            
            try (var input = Files.newInputStream(Path.of("test-file"))) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }

            assertThrows(NoSuchFileException.class, () -> Files.newInputStream(Path.of("no-such-file")));
            assertThrows(UnsupportedOperationException.class, () -> Files.newInputStream(Path.of("test-file"),
                    StandardOpenOption.APPEND));
        } finally {
            file.delete();
            dir.delete();
            new File("test-file-2").delete();
        }
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void outputStream() throws IOException {
        try {
            try (var output = Files.newOutputStream(Path.of("test-file-1"))) {
                output.write(1);
                output.write(2);
                output.write(3);
            }

            try (var input = new FileInputStream("test-file-1")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }

            try (var output = Files.newOutputStream(Path.of("test-file-1"), StandardOpenOption.APPEND)) {
                output.write(4);
            }
            try (var input = new FileInputStream("test-file-1")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3, 4 }, bytes);
            }

            try (var output = Files.newOutputStream(Path.of("test-file-1"), StandardOpenOption.WRITE)) {
                output.write(5);
            }
            try (var input = new FileInputStream("test-file-1")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 5, 2, 3, 4 }, bytes);
            }

            try (var output = Files.newOutputStream(Path.of("test-file-1"), StandardOpenOption.TRUNCATE_EXISTING)) {
                output.write(6);
                output.write(7);
            }
            try (var input = new FileInputStream("test-file-1")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 6, 7 }, bytes);
            }

            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.newOutputStream(Path.of("test-file-1"), StandardOpenOption.CREATE_NEW);
            });

            assertThrows(NoSuchFileException.class, () -> {
                Files.newOutputStream(Path.of("test-file-2"), StandardOpenOption.WRITE);
            });

            try (var output = Files.newOutputStream(Path.of("test-file-2"), StandardOpenOption.CREATE_NEW)) {
                output.write(1);
                output.write(2);
            }
            try (var input = new FileInputStream("test-file-2")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2 }, bytes);
            }
        } finally {
            new File("test-file-1").delete();
            new File("test-file-2").delete();
        }
    }
    
    @Test
    public void directoryStream() throws IOException {
        var dir = new File("test-dir1");
        var file2 = new File(dir, "test-file2");
        var file1 = new File(dir, "test-file1");
        try {
            dir.mkdirs();
            try (var out = new FileOutputStream(file1)) {
                out.write(1);
            }
            file2.mkdirs();

            var dirPath = Path.of("test-dir1");
            var entries = new LinkedHashSet<String>();
            try (var dirStream = Files.newDirectoryStream(dirPath)) {
                for (var entry : dirStream) {
                    entries.add(dirPath.relativize(entry).toString());
                }
            }
            assertEquals(Set.of("test-file1", "test-file2"), entries);

            entries.clear();
            try (var dirStream = Files.newDirectoryStream(Path.of("test-dir1"), f -> f.toString().endsWith("1"))) {
                for (var entry : dirStream) {
                    entries.add(dirPath.relativize(entry).toString());
                }
            }
            assertEquals(Set.of("test-file1"), entries);

            assertThrows(NoSuchFileException.class, () -> Files.newDirectoryStream(Path.of("test-dir-not-exists")));
            assertThrows(NotDirectoryException.class, () -> Files.newDirectoryStream(dirPath.resolve("test-file1")));
            assertThrows(DirectoryIteratorException.class, () -> {
                try (var dirStream = Files.newDirectoryStream(dirPath, f -> {
                    throw new IOException("test exception");
                })) {
                    dirStream.iterator().next();
                }
            });
        } finally {
            file2.delete();
            file1.delete();
            dir.delete();
        }
    }

    @Test
    public void createDir() throws IOException {
        var file = new File("test-dir");
        try {
            assertFalse(file.exists());
            var createdDir = Files.createDirectory(Path.of("test-dir"));
            assertEquals("test-dir", createdDir.toString());
            assertTrue(file.isDirectory());

            Files.createDirectory(Path.of("test-dir", "new-subdir"));
            assertTrue(new File(file, "new-subdir").isDirectory());
            
            new File(file, "subdir").mkdirs();
            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.createDirectory(Path.of("test-dir", "subdir"));
            });
            
            new File(file, "subfile").createNewFile();
            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.createDirectory(Path.of("test-dir", "subfile"));
            });
            
            assertThrows(FileSystemException.class, () -> {
                Files.createDirectory(Path.of("test-dir", "subfile", "subdir"));
            });

            assertThrows(FileSystemException.class, () -> {
                Files.createDirectory(Path.of("test-dir", "no-such-dir", "subdir"));
            });
        } finally {
            new File(file, "subfile").delete();
            new File(file, "subdir").delete();
            new File(file, "new-subdir").delete();
            file.delete();
        }
    }

    @Test
    public void createDirs() throws IOException {
        try {
            Files.createDirectories(Path.of("test-dir", "subdir1", "subdir2"));
            assertTrue(new File("test-dir/subdir1/subdir2").isDirectory());

            Files.createDirectories(Path.of("test-dir", "subdir1", "subdir2"));
            
            Files.createDirectories(Path.of("test-dir", "subdir1", "subdir3"));
            assertTrue(new File("test-dir/subdir1/subdir3").isDirectory());
            
            Files.createFile(Path.of("test-dir", "file"));
            Assert.assertThrows(FileSystemException.class, () -> {
                Files.createDirectories(Path.of("test-dir", "file"));
            });
            Assert.assertThrows(FileSystemException.class, () -> {
                Files.createDirectories(Path.of("test-dir", "file", "dir"));
            });
        } finally {
            new File("test-dir/subdir1/subdir2").delete();
            new File("test-dir/subdir1/subdir3").delete();
            new File("test-dir/subdir1").delete();
            new File("test-dir/file").delete();
            new File("test-dir").delete();
        }
    }

    @Test
    public void delete() throws IOException {
        try {
            new File("test-dir").mkdirs();

            Files.delete(Path.of("test-dir"));
            assertFalse(new File("test-dir").exists());

            Assert.assertThrows(NoSuchFileException.class, () -> Files.delete(Path.of("test-dir")));

            new File("test-dir/subdir").mkdirs();
            Assert.assertThrows(DirectoryNotEmptyException.class, () -> Files.delete(Path.of("test-dir")));

            new File("test-file").createNewFile();
            Files.delete(Path.of("test-file"));
            assertFalse(new File("test-file").exists());

            Assert.assertThrows(NoSuchFileException.class, () -> Files.delete(Path.of("test-file")));
        } finally {
            new File("test-dir/subdir").delete();
            new File("test-dir").delete();
            new File("test-file").delete();
        }
    }

    @Test
    public void copy() throws IOException {
        try {
            assertThrows(NoSuchFileException.class, () -> {
                Files.copy(Path.of("no-such-file"), Path.of("no-such-file-copy"));
            });
            
            try (var output = new FileOutputStream("test-file-1")) {
                output.write(new byte[] { 1, 2, 3 });
            }
            
            Files.copy(Path.of("test-file-1"), Path.of("test-file-2"));
            assertTrue(new File("test-file-2").exists());
            try (var input = new FileInputStream("test-file-2")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }

            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.copy(Path.of("test-file-1"), Path.of("test-file-2"));
            });
            
            new File("test-file-3").createNewFile();
            Files.copy(Path.of("test-file-1"), Path.of("test-file-3"), StandardCopyOption.REPLACE_EXISTING);
            assertTrue(new File("test-file-3").exists());
            try (var input = new FileInputStream("test-file-3")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }
        } finally {
            new File("test-file-1").delete();
            new File("test-file-2").delete();
            new File("test-file-3").delete();
        }
    }
    
    @Test
    public void move() throws IOException {
        try {
            assertThrows(NoSuchFileException.class, () -> {
                Files.move(Path.of("no-such-file"), Path.of("no-such-file-copy"));
            });
            
            try (var output = new FileOutputStream("test-file-1")) {
                output.write(new byte[] { 1, 2, 3 });
            }

            Files.move(Path.of("test-file-1"), Path.of("test-file-2"));
            assertTrue(new File("test-file-2").exists());
            assertFalse(new File("test-file-1").exists());
            try (var input = new FileInputStream("test-file-2")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }
            
            new File("test-file-1").createNewFile();

            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.move(Path.of("test-file-1"), Path.of("test-file-2"));
            });

            new File("test-file-3").createNewFile();
            Files.move(Path.of("test-file-2"), Path.of("test-file-3"), StandardCopyOption.REPLACE_EXISTING);
            assertTrue(new File("test-file-3").exists());
            assertFalse(new File("test-file-2").exists());
            try (var input = new FileInputStream("test-file-3")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }
        } finally {
            new File("test-file-1").delete();
            new File("test-file-2").delete();
            new File("test-file-3").delete();
        }
    }

    @Test
    public void isSameFile() throws IOException {
        new File("test-dir").mkdirs();
        new File("test-dir/a").mkdirs();
        new File("test-dir/a/b").createNewFile();
        new File("test-dir/a/c").createNewFile();
        try {
            assertTrue(Files.isSameFile(Path.of("test-dir", "a", "b"), Path.of("test-dir", "a/b")));
            assertFalse(Files.isSameFile(Path.of("test-dir", "a", "b"), Path.of("test-dir", "a", "c")));
        } finally {
            new File("test-dir/a/b").delete();
            new File("test-dir/a/c").delete();
            new File("test-dir/a").delete();
            new File("test-dir").delete();
        }
    }

    @Test
    public void mismatch() throws IOException {
        try {
            try (var output = new FileOutputStream("test-file-1")) {
                output.write(new byte[] { 1, 2, 3 });
            }
            try (var output = new FileOutputStream("test-file-2")) {
                output.write(new byte[] { 1, 2, 3, 4 });
            }
            try (var output = new FileOutputStream("test-file-3")) {
                output.write(new byte[] { 1, 3, 3 });
            }
            
            assertEquals(-1L, Files.mismatch(Path.of("test-file-1"), Path.of("test-file-1")));
            assertEquals(3, Files.mismatch(Path.of("test-file-1"), Path.of("test-file-2")));
            assertEquals(1, Files.mismatch(Path.of("test-file-1"), Path.of("test-file-3")));
            
            assertThrows(IOException.class, () -> Files.mismatch(Path.of("test-file-1"), Path.of("no-such-file")));
        } finally {
            new File("test-file-1").delete();
            new File("test-file-2").delete();
            new File("test-file-3").delete();
        }
    }

    @Test
    public void readAttributes() throws IOException {
        new File("test-dir").mkdir();
        try (var out = new FileOutputStream("test-file")) {
            out.write(new byte[] { 1, 2, 3 });
        }
        try {
            var attrs = Files.readAttributes(Path.of("test-dir"), BasicFileAttributes.class);
            assertTrue(attrs.isDirectory());
            assertFalse(attrs.isRegularFile());
            assertFalse(attrs.isSymbolicLink());

            attrs = Files.readAttributes(Path.of("test-file"), BasicFileAttributes.class);
            assertFalse(attrs.isDirectory());
            assertTrue(attrs.isRegularFile());
            assertFalse(attrs.isSymbolicLink());
            assertEquals(3, attrs.size());

            assertThrows(NoSuchFileException.class, () -> Files.readAttributes(Path.of("no-such-file"),
                    BasicFileAttributes.class));
        } finally {
            new File("test-dir").delete();
            new File("test-file").delete();
        }
    }

    @Test
    public void isDirectory() throws IOException {
        new File("test-dir").mkdir();
        new File("test-file").createNewFile();
        try {
            assertTrue(Files.isDirectory(Path.of("test-dir")));
            assertFalse(Files.isDirectory(Path.of("test-file")));
            assertFalse(Files.isDirectory(Path.of("no-such-file")));
        } finally {
            new File("test-dir").delete();
            new File("test-file").delete();
        }
    }

    @Test
    public void isRegularFile() throws IOException {
        new File("test-dir").mkdir();
        new File("test-file").createNewFile();
        try {
            assertFalse(Files.isRegularFile(Path.of("test-dir")));
            assertTrue(Files.isRegularFile(Path.of("test-file")));
            assertFalse(Files.isRegularFile(Path.of("no-such-file")));
        } finally {
            new File("test-dir").delete();
            new File("test-file").delete();
        }
    }

    @Test
    public void size() throws IOException {
        try (var out = new FileOutputStream("test-file")) {
            out.write(new byte[] { 1, 2, 3 });
        }
        try {
            assertEquals(3, Files.size(Path.of("test-file")));
            assertThrows(NoSuchFileException.class, () -> Files.size(Path.of("no-such-file")));
        } finally {
            new File("test-file").delete();
        }
    }

    @Test
    public void walkFileTree() throws IOException {
        new File("test-dir").mkdir();
        new File("test-dir/a").createNewFile();
        new File("test-dir/b").mkdir();
        new File("test-dir/b/1").createNewFile();
        new File("test-dir/b/2").createNewFile();
        new File("test-dir/b/3").mkdir();
        new File("test-dir/b/3/x").createNewFile();
        try {
            var visitedFiles = new HashSet<Path>();
            var visitedDirs = new HashSet<Path>();
            var preVisitedDirs = new HashSet<Path>();
            var visitFailed = new HashSet<Path>();
            var visitor = new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    preVisitedDirs.add(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    visitedFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    visitFailed.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    visitedDirs.add(dir);
                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(Path.of("test-dir"), visitor);
            assertEquals(Set.of(Path.of("test-dir"), Path.of("test-dir/b"), Path.of("test-dir/b/3")), visitedDirs);
            assertEquals(Set.of(Path.of("test-dir"), Path.of("test-dir/b"), Path.of("test-dir/b/3")), preVisitedDirs);
            assertEquals(Set.of(Path.of("test-dir/a"), Path.of("test-dir/b/1"), Path.of("test-dir/b/2"),
                    Path.of("test-dir/b/3/x")), visitedFiles);
            assertEquals(Set.of(), visitFailed);
            
            visitedDirs.clear();
            visitedFiles.clear();
            preVisitedDirs.clear();
            Files.walkFileTree(Path.of("test-dir"), Set.of(), 1, visitor);
            assertEquals(Set.of(Path.of("test-dir")), visitedDirs);
            assertEquals(Set.of(Path.of("test-dir")), preVisitedDirs);
            assertEquals(Set.of(Path.of("test-dir/a"), Path.of("test-dir/b")), visitedFiles);
            assertEquals(Set.of(), visitFailed);

            visitedDirs.clear();
            visitedFiles.clear();
            Files.walkFileTree(Path.of("test-dir"), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)  {
                    preVisitedDirs.add(dir);
                    if (dir.equals(Path.of("test-dir/b/3"))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)  {
                    visitedFiles.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    visitedDirs.add(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            assertEquals(Set.of(Path.of("test-dir"), Path.of("test-dir/b")), visitedDirs);
            assertEquals(Set.of(Path.of("test-dir/a"), Path.of("test-dir/b/1"), Path.of("test-dir/b/2")), 
                    visitedFiles);
        } finally {
            new File("test-dir/a").delete();
            new File("test-dir/b/1").delete();
            new File("test-dir/b/2").delete();
            new File("test-dir/b/3/x").delete();
            new File("test-dir/b/3").delete();
            new File("test-dir/b").delete();
            new File("test-dir").delete();
        }
    }

    @Test
    public void walk() throws IOException {
        new File("test-dir").mkdir();
        new File("test-dir/a").createNewFile();
        new File("test-dir/b").mkdir();
        new File("test-dir/b/1").createNewFile();
        new File("test-dir/b/2").createNewFile();
        new File("test-dir/b/3").mkdir();
        new File("test-dir/b/3/x").createNewFile();
        try {
            var visitedFiles = new HashSet<Path>();
            try (var stream = Files.walk(Path.of("test-dir"))) {
                stream.forEach(visitedFiles::add);
            }
            assertEquals(Set.of(Path.of("test-dir"), Path.of("test-dir/a"), Path.of("test-dir/b"),
                    Path.of("test-dir/b/1"), Path.of("test-dir/b/2"), Path.of("test-dir/b/3"),
                    Path.of("test-dir/b/3/x")), visitedFiles);
        } finally {
            new File("test-dir/a").delete();
            new File("test-dir/b/1").delete();
            new File("test-dir/b/2").delete();
            new File("test-dir/b/3/x").delete();
            new File("test-dir/b/3").delete();
            new File("test-dir/b").delete();
            new File("test-dir").delete();
        }
    }

    @Test
    public void copyFromInputStream() throws IOException {
        try {
            Files.copy(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), Path.of("test-file-2"));
            assertTrue(new File("test-file-2").exists());
            try (var input = new FileInputStream("test-file-2")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }

            assertThrows(FileAlreadyExistsException.class, () -> {
                Files.copy(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), Path.of("test-file-2"));
            });

            new File("test-file-3").createNewFile();
            Files.copy(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), Path.of("test-file-3"),
                    StandardCopyOption.REPLACE_EXISTING);
            assertTrue(new File("test-file-3").exists());
            try (var input = new FileInputStream("test-file-3")) {
                var bytes = input.readAllBytes();
                assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
            }
        } finally {
            new File("test-file-1").delete();
            new File("test-file-2").delete();
            new File("test-file-3").delete();
        }
    }
}
