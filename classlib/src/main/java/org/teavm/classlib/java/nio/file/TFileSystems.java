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

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.ProviderNotFoundException;
import java.util.Map;
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

    public static TFileSystem newFileSystem(TURI uri, Map<String, ?> env) throws IOException {
        for (var provider : TFileSystemProvider.installedProviders()) {
            if (provider.getScheme().equals(uri.getScheme())) {
                return provider.newFileSystem(uri, env);
            }
        }
        throw new FileSystemNotFoundException();
    }

    public static TFileSystem newFileSystem(TURI uri, Map<String, ?> env, ClassLoader loader) throws IOException {
        return newFileSystem(uri, env);
    }

    public static TFileSystem newFileSystem(TPath path, ClassLoader loader) throws IOException {
        return newFileSystem(path, Map.of(), loader);
    }

    public static TFileSystem newFileSystem(TPath path, Map<String, ?> env, ClassLoader loader) throws IOException {
        for (var provider : TFileSystemProvider.installedProviders()) {
            try {
                return provider.newFileSystem(path, env);
            } catch (UnsupportedOperationException e) {
                // continue
            }
        }
        throw new ProviderNotFoundException();
    }
}
