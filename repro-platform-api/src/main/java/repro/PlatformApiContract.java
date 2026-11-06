/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;

public final class PlatformApiContract {
    private PlatformApiContract() {
    }

    public static void main(String[] args) {
        Package applicationPackage = PlatformApiContract.class.getPackage();
        check("repro".equals(applicationPackage.getName()), "application package name");
        check(applicationPackage.getSpecificationVersion() == null, "unspecified package version");
        check(int.class.getPackage() == null && void.class.getPackage() == null, "primitives have no package");
        check(String[].class.getPackage() == null, "arrays have no package");
        ConnectException connection = new ConnectException("refused");
        check(connection instanceof SocketException && connection instanceof IOException, "connection hierarchy");
        check("refused".equals(connection.getMessage()), "connection message");
        check(new ConnectException().getMessage() == null, "default connection message");
        check(new SocketException().getMessage() == null, "default socket message");
        check("socket".equals(new SocketException("socket").getMessage()), "socket message");
        Path empty = new File("").toPath();
        check(empty.toString().isEmpty() && !empty.isAbsolute(), "empty relative path");
        File relative = new File("alpha/beta");
        Path path = relative.toPath();
        check(path == relative.toPath(), "cached Path identity");
        check(path.equals(Path.of("alpha", "beta")), "relative segments");
        check(path.getParent().equals(Path.of("alpha")), "parent");
        check(path.getFileName().toString().equals("beta"), "file name");
        check(path.toFile().equals(relative), "File/Path round trip");
        check(new File("alpha//beta/../gamma").toPath().normalize()
                .equals(Path.of("alpha", "gamma")), "normalize independently");
        String[][] normalizationCases = {
                { "../..", "../.." }, { "alpha/../../beta", "../beta" },
                { "./alpha", "alpha" }, { "alpha/..", "" },
                { "alpha/./beta", "alpha/beta" }, { "/alpha/../beta", "/beta" },
                { "/../../alpha", "/alpha" }, { "../alpha/..", ".." }
        };
        for (String[] pair : normalizationCases) {
            check(Path.of(pair[0]).normalize().equals(Path.of(pair[1])), "normalize " + pair[0]);
        }
        check(new File("עברית/العربية").toPath().getFileName().toString().equals("العربية"), "Unicode path");
        String invalid = "alpha/\0/beta";
        try {
            new File(invalid).toPath();
            throw new AssertionError("NUL accepted by File.toPath");
        } catch (InvalidPathException expected) {
            check(expected.getInput().contains("\0"), "invalid path input retained");
        }
        try {
            Path.of("alpha", "\0");
            throw new AssertionError("NUL accepted by Path.of");
        } catch (InvalidPathException expected) {
            check(expected.getIndex() >= 0, "invalid path index retained");
        }
        DosFileAttributes attributes = new TestAttributes();
        check(attributes instanceof BasicFileAttributes, "DOS attribute inheritance");
        check(attributes.isReadOnly() && !attributes.isHidden() && attributes.isSystem()
                && !attributes.isArchive(), "DOS attribute dispatch");
        check(attributes.size() == 12 && attributes.isRegularFile(), "basic attribute dispatch");
        System.out.println("Platform API contract passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestAttributes implements DosFileAttributes {
        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public boolean isSystem() {
            return true;
        }

        @Override
        public boolean isArchive() {
            return false;
        }

        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public FileTime lastAccessTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public FileTime creationTime() {
            return FileTime.fromMillis(0);
        }

        @Override
        public boolean isRegularFile() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return 12;
        }

        @Override
        public Object fileKey() {
            return null;
        }
    }
}
