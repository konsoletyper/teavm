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
import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.classlib.fs.VirtualFileAccessor;

public class InMemoryVirtualDirectory extends AbstractInMemoryVirtualFile {
    final Map<String, AbstractInMemoryVirtualFile> children = new LinkedHashMap<>();

    InMemoryVirtualDirectory(String name) {
        super(name);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public String[] listFiles() {
        return children.keySet().toArray(new String[0]);
    }

    @Override
    public AbstractInMemoryVirtualFile getChildFile(String fileName) {
        return children.get(fileName);
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        return null;
    }

    @Override
    public InMemoryVirtualFile createFile(String fileName) throws IOException {
        if (!canWrite()) {
            throw new IOException("Directory is read-only");
        }
        if (children.containsKey(fileName)) {
            return null;
        }
        InMemoryVirtualFile file = new InMemoryVirtualFile(fileName);
        adoptFile(file);
        return file;
    }

    @Override
    public InMemoryVirtualDirectory createDirectory(String fileName) {
        if (!canWrite() || getChildFile(fileName) != null) {
            return null;
        }
        InMemoryVirtualDirectory file = new InMemoryVirtualDirectory(fileName);
        adoptFile(file);
        return file;
    }

    @Override
    public boolean adopt(AbstractInMemoryVirtualFile file, String fileName) {
        if (!canWrite()) {
            return false;
        }
        if (!file.parent.canWrite()) {
            return false;
        }
        file.parent.children.remove(file.name);
        file.parent = this;
        children.put(fileName, file);
        file.name = fileName;
        return true;
    }

    @Override
    public int length() {
        return 0;
    }

    private void adoptFile(AbstractInMemoryVirtualFile file) {
        if (children.containsKey(file.name)) {
            throw new IllegalArgumentException("File " + file.getName() + " already exists");
        }
        file.parent = this;
        children.put(file.name, file);
        modify();
    }
}
