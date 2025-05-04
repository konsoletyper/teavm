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
package org.teavm.classlib.java.nio.file.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.teavm.classlib.java.net.TURI;
import org.teavm.classlib.java.nio.file.TAccessMode;
import org.teavm.classlib.java.nio.file.TCopyOption;
import org.teavm.classlib.java.nio.file.TDirectoryStream;
import org.teavm.classlib.java.nio.file.TFileSystem;
import org.teavm.classlib.java.nio.file.TLinkOption;
import org.teavm.classlib.java.nio.file.TNoSuchFileException;
import org.teavm.classlib.java.nio.file.TOpenOption;
import org.teavm.classlib.java.nio.file.TPath;
import org.teavm.classlib.java.nio.file.attribute.TBasicFileAttributes;
import org.teavm.classlib.java.nio.file.attribute.TFileAttribute;
import org.teavm.classlib.java.nio.file.impl.TDefaultFileSystemProvider;

public abstract class TFileSystemProvider {
    private static List<TFileSystemProvider> installedProviders;

    protected TFileSystemProvider() {
    }

    public abstract String getScheme();

    public abstract TFileSystem newFileSystem(TURI uri, Map<String, ?> env) throws IOException;

    public abstract TFileSystem getFileSystem(TURI uri);

    public abstract TPath getPath(TURI uri);

    public abstract TFileSystem newFileSystem(TPath path, Map<String, ?> env) throws IOException;

    public InputStream newInputStream(TPath path, TOpenOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    public
    OutputStream newOutputStream(TPath path, TOpenOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    public abstract TDirectoryStream<TPath> newDirectoryStream(TPath dir,
            TDirectoryStream.Filter<? super TPath> filter) throws IOException;

    public abstract void createDirectory(TPath dir, TFileAttribute<?>... attrs) throws IOException;

    public abstract void delete(TPath path) throws IOException;

    public boolean deleteIfExists(TPath path) throws IOException {
        try {
            delete(path);
            return true;
        } catch (TNoSuchFileException e) {
            return false;
        }
    }

    public TPath readSymbolicLink(TPath link) throws IOException {
        throw new UnsupportedOperationException();
    }


    public abstract void copy(TPath source, TPath target, TCopyOption... options) throws IOException;

    public abstract void move(TPath source, TPath target, TCopyOption... options) throws IOException;

    public abstract boolean isSameFile(TPath path, TPath path2) throws IOException;

    public abstract boolean isHidden(TPath path) throws IOException;

    public abstract void checkAccess(TPath path, TAccessMode... modes) throws IOException;

    public abstract <A extends TBasicFileAttributes> A readAttributes(TPath path, Class<A> type,
            TLinkOption... options) throws IOException;

    public boolean exists(TPath path, TLinkOption... options) {
        try {
            if (Arrays.asList(options).contains(TLinkOption.NOFOLLOW_LINKS)) {
                checkAccess(path);
            } else {
                readAttributes(path, TBasicFileAttributes.class);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public <A extends TBasicFileAttributes> A readAttributesIfExists(TPath path, Class<A> type,
            TLinkOption... options) throws IOException {
        try {
            return readAttributes(path, type, options);
        } catch (TNoSuchFileException e) {
            return null;
        }
    }

    public static List<TFileSystemProvider> installedProviders() {
        if (installedProviders == null) {
            var providers = new ArrayList<TFileSystemProvider>();
            providers.add(TDefaultFileSystemProvider.INSTANCE);
            for (var provider : ServiceLoader.load(TFileSystemProvider.class)) {
                providers.add(provider);
            }
            installedProviders = List.copyOf(providers);
        }
        return installedProviders;
    }
}
