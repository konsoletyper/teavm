/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.runtime.fs;

import org.teavm.runtime.fs.memory.InMemoryVirtualFileSystem;

public final class VirtualFileSystemProvider {
    private static VirtualFileSystem instance;

    private VirtualFileSystemProvider() {
    }

    public static VirtualFileSystem getInstance() {
        if (instance == null) {
            instance = create();
        }
        return instance;
    }

    private static VirtualFileSystem create() {
        return new InMemoryVirtualFileSystem();
    }

    public static void setInstance(VirtualFileSystem instance) {
        VirtualFileSystemProvider.instance = instance;
    }
}
