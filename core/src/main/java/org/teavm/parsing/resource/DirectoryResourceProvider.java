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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

public class DirectoryResourceProvider implements ResourceProvider {
    private File dir;

    public DirectoryResourceProvider(File dir) {
        this.dir = dir;
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var file = new File(dir, name);
        if (!file.isFile()) {
            return Collections.emptyIterator();
        }
        var resource = new Resource() {
            @Override
            public InputStream open() {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Date getModificationDate() {
                return new Date(file.lastModified());
            }
        };
        return Collections.<Resource>singleton(resource).iterator();
    }

    @Override
    public void close() {
    }
}
