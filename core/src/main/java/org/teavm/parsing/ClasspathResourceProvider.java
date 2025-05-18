/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.parsing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import org.teavm.parsing.resource.Resource;
import org.teavm.parsing.resource.ResourceProvider;

public class ClasspathResourceProvider implements ResourceProvider {
    private ClassLoader classLoader;

    public ClasspathResourceProvider(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        try {
            var enumeration = classLoader.getResources(name);
            if (enumeration == null) {
                return Collections.emptyIterator();
            }
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return enumeration.hasMoreElements();
                }

                @Override
                public Resource next() {
                    var elem = enumeration.nextElement();
                    return new Resource() {
                        @Override
                        public InputStream open() {
                            try {
                                return elem.openStream();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }

                        @Override
                        public Date getModificationDate() {
                            if (classLoader == null) {
                                return null;
                            }
                            URL url = classLoader.getResource(name);
                            if (url == null) {
                                return null;
                            }
                            if (url.getProtocol().equals("file")) {
                                try {
                                    File file = new File(url.toURI());
                                    return file.exists() ? new Date(file.lastModified()) : null;
                                } catch (URISyntaxException e) {
                                    // If URI is invalid, we just report that class should be reparsed
                                    return null;
                                }
                            } else if (url.getProtocol().equals("jar") && url.getPath().startsWith("file:")) {
                                int exclIndex = url.getPath().indexOf('!');
                                String jarFileName = exclIndex >= 0 ? url.getPath().substring(0, exclIndex)
                                        : url.getPath();
                                File file = new File(jarFileName.substring("file:".length()));
                                return file.exists() ? new Date(file.lastModified()) : null;
                            } else {
                                return null;
                            }
                        }
                    };
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
    }
}
