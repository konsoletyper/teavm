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

import java.io.File;
import java.io.IOException;

public interface TPath extends Comparable<TPath>, Iterable<TPath> {
    static TPath of(String first, String... more) {
        return TFileSystems.getDefault().getPath(first, more);
    }

    TFileSystem getFileSystem();

    boolean isAbsolute();

    TPath getRoot();

    TPath getFileName();

    TPath getParent();

    int getNameCount();

    TPath getName(int index);

    TPath subpath(int beginIndex, int endIndex);

    boolean startsWith(TPath other);

    default boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    boolean endsWith(TPath other);

    default boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    TPath normalize();

    TPath resolve(TPath other);

    default TPath resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    default TPath resolveSibling(TPath other) {
        return getParent() == null ? other : getParent().resolve(other);
    }

    default TPath resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    TPath relativize(TPath other);

    TPath toAbsolutePath();

    TPath toRealPath(TLinkOption... options) throws IOException;

    @Override
    int compareTo(TPath o);

    default File toFile() {
        return new File(toString());
    }
}
