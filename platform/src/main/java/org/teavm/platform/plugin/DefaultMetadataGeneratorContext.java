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
import org.teavm.common.ServiceRepository;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.platform.metadata.*;

class DefaultMetadataGeneratorContext implements MetadataGeneratorContext {
    private ListableClassReaderSource classSource;
    private ClassLoader classLoader;
    private Properties properties;
    private BuildTimeResourceProxyBuilder proxyBuilder = new BuildTimeResourceProxyBuilder();
    private ServiceRepository services;

    DefaultMetadataGeneratorContext(ListableClassReaderSource classSource, ClassLoader classLoader,
            Properties properties, ServiceRepository services) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.properties = properties;
        this.services = services;
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
    public <T extends Resource> T createResource(Class<T> resourceType) {
        return resourceType.cast(Proxy.newProxyInstance(classLoader,
                new Class<?>[] { resourceType, ResourceWriter.class, ResourceTypeDescriptorProvider.class },
                proxyBuilder.buildProxy(resourceType)));
    }

    @Override
    public <T extends Resource> ResourceArray<T> createResourceArray() {
        return new BuildTimeResourceArray<>();
    }

    @Override
    public ClassResource createClassResource(String className) {
        return new BuildTimeClassResource(className);
    }

    @Override
    public StaticFieldResource createFieldResource(FieldReference field) {
        return new BuildTimeStaticFieldResource(field);
    }

    @Override
    public <T extends Resource> ResourceMap<T> createResourceMap() {
        return new BuildTimeResourceMap<>();
    }

    @Override
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }
}
