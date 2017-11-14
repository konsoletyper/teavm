/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.classlib.fs;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryVirtualDirectory extends AbstractInMemoryVirtualFile {
    final Map<String, VirtualFile> children = new LinkedHashMap<>();

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
    public VirtualFile[] listFiles() {
        return children.values().toArray(new VirtualFile[0]);
    }

    @Override
    public VirtualFile getChildFile(String fileName) {
        return children.get(fileName);
    }

    @Override
    public VirtualFileAccessor createAccessor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile createFile(String fileName) {
        InMemoryVirtualFile file = new InMemoryVirtualFile(fileName);
        adoptFile(file);
        return file;
    }

    @Override
    public VirtualFile createDirectory(String fileName) {
        InMemoryVirtualDirectory file = new InMemoryVirtualDirectory(fileName);
        adoptFile(file);
        return file;
    }

    @Override
    public void adopt(VirtualFile file, String fileName) {
        AbstractInMemoryVirtualFile typedFile = (AbstractInMemoryVirtualFile) file;
        typedFile.parent.children.remove(typedFile.name);
        typedFile.parent = this;
        children.put(fileName, typedFile);
        typedFile.name = fileName;
    }

    @Override
    public int length() {
        throw new UnsupportedOperationException();
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
