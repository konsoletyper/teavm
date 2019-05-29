/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.fs.memory;

import java.io.IOException;
import org.teavm.classlib.fs.VirtualFileAccessor;

public abstract class AbstractInMemoryVirtualFile {
    String name;
    InMemoryVirtualDirectory parent;
    long lastModified = System.currentTimeMillis();
    boolean readOnly;

    AbstractInMemoryVirtualFile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean delete() {
        if (parent == null || (isDirectory() && listFiles().length > 0)) {
            return false;
        }

        if (parent != null && !parent.canWrite()) {
            return false;
        }

        parent.children.remove(name);
        parent.modify();
        parent = null;
        return true;
    }

    public abstract boolean isDirectory();

    public abstract boolean isFile();

    public abstract String[] listFiles();

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return !readOnly;
    }

    public long lastModified() {
        return lastModified;
    }

    public boolean setLastModified(long lastModified) {
        if (readOnly) {
            return false;
        }
        this.lastModified = lastModified;
        return true;
    }

    public boolean setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return true;
    }

    void modify() {
        lastModified = System.currentTimeMillis();
    }

    public abstract AbstractInMemoryVirtualFile getChildFile(String fileName);

    public abstract VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append);

    public abstract InMemoryVirtualFile createFile(String fileName) throws IOException;

    public abstract InMemoryVirtualDirectory createDirectory(String fileName);

    public abstract boolean adopt(AbstractInMemoryVirtualFile file, String fileName);

    public int length() {
        return 0;
    }
}
