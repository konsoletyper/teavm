/*
 *  Copyright 2026 Alexey Andreev.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.extension.spi.transformation.ResourceTransformerEnvironment;

public class SPITransformingResourceProvider implements ResourceProvider {
    private ResourceProvider underlying;
    private ClassLoader classLoader;
    private List<ResourceTransformer> transformers;
    private Map<String, List<Resource>> cache = new HashMap<>();
    private Properties properties;

    public SPITransformingResourceProvider(ResourceProvider underlying, ClassLoader classLoader,
            List<ResourceTransformer> transformers, Properties properties) {
        this.underlying = underlying;
        this.classLoader = classLoader;
        this.transformers = transformers;
        this.properties = properties;
    }

    public static ResourceProvider wrap(ClassLoader classLoader, Properties properties,
            ResourceProvider wrapped) {
        var services = new ArrayList<ResourceTransformer>();
        for (var service : ServiceLoader.load(ResourceTransformer.class, classLoader)) {
            services.add(service);
        }
        if (services.isEmpty()) {
            return wrapped;
        }
        return new SPITransformingResourceProvider(wrapped, classLoader, services, properties);
    }

    @Override
    public Iterator<Resource> getResources(String name) {
        var list = cache.get(name);
        if (list == null) {
            list = new ArrayList<>();
            for (var iter = underlying.getResources(name); iter.hasNext();) {
                list.add(new ResourceImpl(iter.next(), name));
            }
            if (list.isEmpty()) {
                cache.put(name, Collections.emptyList());
                return Collections.emptyIterator();
            }
            cache.put(name, list);
        }
        return list.iterator();
    }

    @Override
    public void close() {
        underlying.close();
    }

    private ResourceTransformerEnvironment env = new ResourceTransformerEnvironment() {
        @Override
        public Iterator<org.teavm.extension.resource.Resource> resources(String name) {
            var underlyingIter = underlying.getResources(name);
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return underlyingIter.hasNext();
                }

                @Override
                public org.teavm.extension.resource.Resource next() {
                    var underlyingRes = underlyingIter.next();
                    return underlyingRes::open;
                }
            };
        }

        @Override
        public ClassLoader classLoader() {
            return classLoader;
        }

        @Override
        public String property(String name) {
            return properties.getProperty(name);
        }
    };

    private class ResourceImpl implements Resource, org.teavm.extension.resource.Resource {
        private Resource underlying;
        private String name;
        private byte[] data;
        private boolean dataNotChanged;

        ResourceImpl(Resource underlying, String name) {
            this.underlying = underlying;
            this.name = name;
        }

        @Override
        public InputStream open() {
            if (dataNotChanged) {
                return underlying.open();
            }
            if (data == null) {
                try (var input = underlying.open()) {
                    var data = input.readAllBytes();
                    var changed = false;
                    for (var transformer : transformers) {
                        var newData = transformer.transform(env, name, data);
                        if (newData != null && newData != data && !Arrays.equals(newData, data)) {
                            changed = true;
                            data = newData;
                        }
                    }
                    if (!changed) {
                        dataNotChanged = true;
                        return new ByteArrayInputStream(data);
                    }
                    this.data = data;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return new ByteArrayInputStream(data);
        }

        @Override
        public Date getModificationDate() {
            return underlying.getModificationDate();
        }
    }
}
