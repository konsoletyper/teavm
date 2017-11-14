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

public abstract class AbstractInMemoryVirtualFile implements VirtualFile {
    String name;
    InMemoryVirtualDirectory parent;
    long lastModified = System.currentTimeMillis();
    boolean readOnly;

    AbstractInMemoryVirtualFile(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void delete() {
        parent.children.remove(name);
        parent.modify();
        parent = null;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return !readOnly;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    void modify() {
        lastModified = System.currentTimeMillis();
    }
}
