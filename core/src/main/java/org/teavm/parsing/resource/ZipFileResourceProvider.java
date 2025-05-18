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
package org.teavm.parsing.resource;

import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileResourceProvider implements ResourceProvider {
    private File file;
    private ZipFile zipFile;
    private Map<String, Optional<ZipEntry>> entryCache = new HashMap<>();

    public ZipFileResourceProvider(File file) {
        this.file = Objects.requireNonNull(file);
        try {
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ZipFileResourceProvider(String fileName) {
        this(new File(fileName));
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var entry = getEntry(name);
        if (entry == null) {
            return Collections.emptyIterator();
        }
        var resource = new Resource() {
            @Override
            public InputStream open() {
                try {
                    return zipFile.getInputStream(entry);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Date getModificationDate() {
                var date = entry.getTime();
                if (date < 0) {
                    date = file.lastModified();
                }
                return new Date(date);
            }
        };
        return Collections.<Resource>singleton(resource).iterator();
    }

    private ZipEntry getEntry(String name) {
        return entryCache.computeIfAbsent(name, n -> Optional.ofNullable(zipFile.getEntry(name))).orElse(null);
    }

    @Override
    public void close() {
        try {
            zipFile.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
