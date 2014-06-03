/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.lang.reflect.Proxy;
import java.util.Properties;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

/**
 *
 * @author Alexey Andreev
 */
class DefaultMetadataGeneratorContext implements MetadataGeneratorContext {
    private ListableClassReaderSource classSource;
    private ClassLoader classLoader;
    private Properties properties;
    private BuildTimeResourceProxyBuilder proxyBuilder = new BuildTimeResourceProxyBuilder();

    private DefaultMetadataGeneratorContext(ListableClassReaderSource classSource, ClassLoader classLoader,
            Properties properties) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.properties = properties;
    }

    @Override
    public ListableClassReaderSource getClassSource() {
        return classSource;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Properties getProperties() {
        return new Properties(properties);
    }

    @Override
    public <T> T createResource(Class<T> resourceType) {
        return resourceType.cast(Proxy.newProxyInstance(classLoader, new Class<?>[] { resourceType },
                proxyBuilder.buildProxy(resourceType)));
    }

    @Override
    public <T> ResourceArray<T> createResourceArray() {
        return new BuildTimeResourceArray<>();
    }

    @Override
    public <T> ResourceMap<T> createResourceMap() {
        return new BuildTimeResourceMap<>();
    }
}
