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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import org.teavm.classlib.java.lang.TSystem;
import org.teavm.classlib.java.nio.file.attribute.TBasicFileAttributes;
import org.teavm.classlib.java.nio.file.attribute.TFileAttribute;
import org.teavm.classlib.java.nio.file.attribute.TFileTime;

public final class TFiles {
    private TFiles() {
    }

    public static InputStream newInputStream(TPath path, TOpenOption... options) throws IOException {
        return path.getFileSystem().provider().newInputStream(path, options);
    }

    public static OutputStream newOutputStream(TPath path, TOpenOption... options) throws IOException {
        return path.getFileSystem().provider().newOutputStream(path, options);
    }

    public static TDirectoryStream<TPath> newDirectoryStream(TPath dir) throws IOException {
        return newDirectoryStream(dir, f -> true);
    }

    public static TDirectoryStream<TPath> newDirectoryStream(TPath dir, TDirectoryStream.Filter<? super TPath> filter)
            throws IOException {
        return dir.getFileSystem().provider().newDirectoryStream(dir, filter);
    }

    public static TPath createFile(TPath file, TFileAttribute<?>... attrs) throws IOException {
        file.getFileSystem().provider().newOutputStream(file, TStandardOpenOption.CREATE_NEW,
                TStandardOpenOption.WRITE).close();
        return file;
    }

    public static TPath createDirectory(TPath dir, TFileAttribute<?>... attrs) throws IOException {
        dir.getFileSystem().provider().createDirectory(dir, attrs);
        return dir;
    }

    public static TPath createDirectories(TPath dir, TFileAttribute<?>... attrs) throws IOException {
        if (dir.getNameCount() <= 1) {
            return createDirectory(dir, attrs);
        }

        var index = dir.getNameCount() + 1;
        for (; index > 1; --index) {
            if (exists(dir.subpath(0, index - 1))) {
                break;
            }
        }
        if (index > dir.getNameCount() && !isDirectory(dir)) {
            throw new FileSystemException(dir.toString());
        }
        while (index <= dir.getNameCount()) {
            createDirectory(dir.subpath(0, index++));
        }
        return dir;
    }

    public static TPath createTempFile(TPath dir, String prefix, String suffix, TFileAttribute<?>... attrs)
            throws IOException {
        if (prefix.length() < 3) {
            throw new IllegalArgumentException();
        }

        var sb = new StringBuilder();
        sb.append(prefix);
        generateRandomName(sb);
        sb.append(suffix != null ? suffix : ".tmp");
        return createFile(dir.resolve(sb.toString()), attrs);
    }

    public static TPath createTempFile(String prefix, String suffix, TFileAttribute<?>... attrs)
            throws IOException {
        return createTempFile(TPath.of(TSystem.getTempDir()), prefix, suffix, attrs);
    }

    public static TPath createTempDirectory(TPath dir, String prefix, TFileAttribute<?>... attrs)
            throws IOException {
        var sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
        }
        generateRandomName(sb);
        return createDirectory(dir.resolve(sb.toString()), attrs);
    }

    public static TPath createTempDirectory(String prefix, TFileAttribute<?>... attrs) throws IOException {
        return createTempDirectory(TPath.of(TSystem.getTempDir()), prefix, attrs);
    }

    private static void generateRandomName(StringBuilder sb) {
        var random = new Random();
        for (var i = 0; i < 10; ++i) {
            var digit = random.nextInt(62);
            char c;
            if (digit < 10) {
                c = (char) ('0' + digit);
            } else if (digit < 36) {
                c = (char) (digit - 10 + 'A');
            } else {
                c = (char) (digit - 36 + 'a');
            }
            sb.append(c);
        }
    }

    public static void delete(TPath path) throws IOException {
        path.getFileSystem().provider().delete(path);
    }

    public static boolean deleteIfExists(TPath path) throws IOException {
        return path.getFileSystem().provider().deleteIfExists(path);
    }

    public static TPath copy(TPath source, TPath target, TCopyOption... options) throws IOException {
        if (source.getFileSystem().provider() == target.getFileSystem().provider()) {
            source.getFileSystem().provider().copy(source, target, options);
        } else {
            copyFallback(source, target, options);
        }
        return target;
    }

    public static TPath move(TPath source, TPath target, TCopyOption... options) throws IOException {
        if (source.getFileSystem().provider() == target.getFileSystem().provider()) {
            source.getFileSystem().provider().move(source, target, options);
        } else {
            copyFallback(source, target, options);
            delete(source);
        }
        return target;
    }

    private static void copyFallback(TPath source, TPath target, TCopyOption... options) throws IOException {
        var replace = false;
        for (var option : options) {
            if (option instanceof TStandardCopyOption) {
                switch ((TStandardCopyOption) option) {
                    case REPLACE_EXISTING:
                        replace = true;
                        break;
                    case COPY_ATTRIBUTES:
                        break;
                    case ATOMIC_MOVE:
                        throw new UnsupportedOperationException();
                }
            }
        }
        var openOptions = replace
                ? new TOpenOption[] { TStandardOpenOption.CREATE, TStandardOpenOption.TRUNCATE_EXISTING }
                : new TOpenOption[] { TStandardOpenOption.CREATE_NEW };
        try (var input = newInputStream(source);
                var output = newOutputStream(target, openOptions)) {
            input.transferTo(output);
        }
    }

    public static boolean isSameFile(TPath path, TPath path2) throws IOException {
        if (Objects.equals(path, path2)) {
            return true;
        }
        if (path.getFileSystem().provider() != path2.getFileSystem().provider()) {
            return false;
        }
        return path.getFileSystem().provider().isSameFile(path, path2);
    }

    public static long mismatch(TPath path, TPath path2) throws IOException {
        if (isSameFile(path, path2)) {
            return -1;
        }
        var size1 = readAttributes(path, TBasicFileAttributes.class).size();
        var size2 = readAttributes(path2, TBasicFileAttributes.class).size();
        if (size1 != size2) {
            return Math.min(size1, size2);
        }
        var offset = 0L;
        var buf1 = new byte[2048];
        var buf2 = new byte[2048];
        try (var input1 = newInputStream(path);
                var input2 = newInputStream(path2)) {
            while (true) {
                var sz1 = input1.readNBytes(buf1, 0, buf1.length);
                var sz2 = input2.readNBytes(buf2, 0, buf2.length);
                if (sz1 != sz2) {
                    return offset + Math.min(sz1, sz2);
                }
                for (var i = 0; i < buf1.length; ++i) {
                    if (buf1[i] != buf2[i]) {
                        return offset + i;
                    }
                }
                offset += sz1;
                if (sz1 < buf1.length) {
                    break;
                }
            }
        }
        return -1;
    }

    public static boolean isHidden(TPath path) throws IOException {
        return path.getFileSystem().provider().isHidden(path);
    }

    public static <A extends TBasicFileAttributes> A readAttributes(TPath path, Class<A> type,
            TLinkOption... options) throws IOException {
        return path.getFileSystem().provider().readAttributes(path, type, options);
    }

    public static boolean isSymbolicLink(TPath path) {
        try {
            var attr = readAttributes(path, TBasicFileAttributes.class);
            return attr.isSymbolicLink();
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isDirectory(TPath path, TLinkOption... options) {
        try {
            var attr = readAttributes(path, TBasicFileAttributes.class, options);
            return attr.isDirectory();
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isRegularFile(TPath path, TLinkOption... options) {
        try {
            var attr = readAttributes(path, TBasicFileAttributes.class, options);
            return attr.isRegularFile();
        } catch (IOException e) {
            return false;
        }
    }

    public static TFileTime getLastModifiedTime(TPath path, TLinkOption... options) throws IOException {
        return readAttributes(path, TBasicFileAttributes.class, options).lastModifiedTime();
    }

    public static long size(TPath path) throws IOException {
        return readAttributes(path, TBasicFileAttributes.class).size();
    }

    public static boolean exists(TPath path, TLinkOption... options) {
        return path.getFileSystem().provider().exists(path, options);
    }

    public static boolean notExists(TPath path, TLinkOption... options) {
        try {
            if (Arrays.asList(options).contains(TLinkOption.NOFOLLOW_LINKS)) {
                path.getFileSystem().provider().checkAccess(path);
            } else {
                readAttributes(path, TBasicFileAttributes.class);
            }
        } catch (NoSuchFileException e) {
            return true;
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public static BufferedReader newBufferedReader(TPath path, Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path), cs));
    }

    public static BufferedReader newBufferedReader(TPath path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public static BufferedWriter newBufferedWriter(TPath path, Charset cs, TOpenOption... options)
            throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs));
    }

    public static BufferedWriter newBufferedWriter(TPath path, TOpenOption... options) throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }
}
