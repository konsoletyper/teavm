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
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileAccessor;

public class VirtualFileImpl implements VirtualFile {
    private InMemoryVirtualFileSystem fs;
    private String path;

    public VirtualFileImpl(InMemoryVirtualFileSystem fs, String path) {
        this.fs = fs;
        this.path = path;
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public boolean isDirectory() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.isDirectory();
    }

    @Override
    public boolean isFile() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.isFile();
    }

    @Override
    public String[] listFiles() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null ? inMemory.listFiles() : null;
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null ? inMemory.createAccessor(readable, writable, append) : null;
    }

    @Override
    public boolean createFile(String fileName) throws IOException {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        if (inMemory == null) {
            throw new IOException("Directory does not exist");
        }
        return inMemory.createFile(fileName) != null;
    }

    @Override
    public boolean createDirectory(String fileName) {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.createDirectory(fileName) != null;
    }

    @Override
    public boolean delete() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.delete();
    }

    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        if (inMemory == null) {
            return false;
        }
        AbstractInMemoryVirtualFile fileInMemory = ((VirtualFileImpl) file).findInMemory();
        if (fileInMemory == null) {
            return false;
        }
        return inMemory.adopt(fileInMemory, fileName);
    }

    @Override
    public boolean canRead() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.canRead();
    }

    @Override
    public boolean canWrite() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.canWrite();
    }

    @Override
    public long lastModified() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null ? inMemory.lastModified() : 0;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.setLastModified(lastModified);
    }

    @Override
    public boolean setReadOnly(boolean readOnly) {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null && inMemory.setReadOnly(readOnly);
    }

    @Override
    public int length() {
        AbstractInMemoryVirtualFile inMemory = findInMemory();
        return inMemory != null ? inMemory.length() : 0;
    }

    AbstractInMemoryVirtualFile findInMemory() {
        AbstractInMemoryVirtualFile file = fs.root;
        int i = 0;
        if (path.startsWith("/")) {
            i++;
        }

        while (i < path.length()) {
            int next = path.indexOf('/', i);
            if (next < 0) {
                next = path.length();
            }

            file = file.getChildFile(path.substring(i, next));
            if (file == null) {
                break;
            }

            i = next + 1;
        }

        return file;
    }
}
