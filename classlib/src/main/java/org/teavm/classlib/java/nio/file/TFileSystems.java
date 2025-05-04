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

import java.nio.file.FileSystemNotFoundException;
import org.teavm.classlib.java.net.TURI;
import org.teavm.classlib.java.nio.file.impl.TDefaultFileSystem;
import org.teavm.classlib.java.nio.file.spi.TFileSystemProvider;

public final class TFileSystems {
    private TFileSystems() {
    }

    public static TFileSystem getDefault() {
        return TDefaultFileSystem.INSTANCE;
    }

    public static TFileSystem getFileSystem(TURI uri) {
        for (var provider : TFileSystemProvider.installedProviders()) {
            if (provider.getScheme().equals(uri.getScheme())) {
                return provider.getFileSystem(uri);
            }
        }
        throw new FileSystemNotFoundException();
    }
}
