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
package org.teavm.classlib.java.nio.file.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.teavm.classlib.java.nio.file.TFileSystem;
import org.teavm.classlib.java.nio.file.TPath;
import org.teavm.runtime.fs.VirtualFileSystem;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

public class TDefaultFileSystem extends TFileSystem {
    public final VirtualFileSystem vfs;
    public static final TDefaultFileSystem DEFAULT = new TDefaultFileSystem();

    private TDefaultFileSystem() {
        this.vfs = VirtualFileSystemProvider.getInstance();
    }

    @Override
    public TDefaultPath getPath(String first, String... more) {
        var sb = new StringBuilder();
        var separator = getSeparatorChar();
        appendSegment(sb, separator, first);
        for (var part : more) {
            if (!part.isEmpty()) {
                appendSegment(sb, separator, part);
            }
        }
        return new TDefaultPath(this, sb.toString());
    }

    private void appendSegment(StringBuilder sb, char separator, String part) {
        if (part.isEmpty()) {
            return;
        }
        var last = 0;
        if (part.charAt(0) == separator && sb.length() == 0) {
            sb.append(separator);
        }
        while (true) {
            var index = part.indexOf(separator, last);
            if (index < 0) {
                break;
            }
            if (index > last) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != separator) {
                    sb.append(separator);
                }
                sb.append(part, last, index);
            }
            while (part.charAt(index) == separator) {
                if (++index >= part.length()) {
                    return;
                }
            }
            last = index;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != separator) {
            sb.append(separator);
        }
        sb.append(part, last, part.length());
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return vfs.isWindows() ? "\\" : "/";
    }

    char getSeparatorChar() {
        return vfs.isWindows() ? '\\' : '/';
    }

    @Override
    public Iterable<TPath> getRootDirectories() {
        return List.of(new TDefaultPath(this, "/"));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of();
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
}
