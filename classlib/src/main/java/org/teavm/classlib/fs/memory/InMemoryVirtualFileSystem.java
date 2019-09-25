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

import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileSystem;

public class InMemoryVirtualFileSystem implements VirtualFileSystem {
    InMemoryVirtualDirectory root = new InMemoryVirtualDirectory("");
    private String userDir = "/";

    @Override
    public VirtualFile getFile(String path) {
        return new VirtualFileImpl(this, path);
    }

    @Override
    public String getUserDir() {
        return userDir;
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        return path;
    }
}
