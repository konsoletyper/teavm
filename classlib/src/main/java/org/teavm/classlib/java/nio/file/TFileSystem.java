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

import java.io.Closeable;
import java.util.Set;
import org.teavm.classlib.java.nio.file.spi.TFileSystemProvider;

public abstract class TFileSystem implements Closeable {
    public abstract TPath getPath(String first, String... more);

    public abstract boolean isOpen();

    public abstract boolean isReadOnly();

    public abstract String getSeparator();

    public abstract Iterable<TPath> getRootDirectories();

    public abstract Set<String> supportedFileAttributeViews();

    public abstract TFileSystemProvider provider();
}
