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
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.platform.metadata.ClassResource;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.StaticFieldResource;

class DefaultMetadataGeneratorContext implements MetadataGeneratorContext {
    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private Properties properties;
    private BuildTimeResourceProxyBuilder proxyBuilder = new BuildTimeResourceProxyBuilder();
    private ServiceRepository services;

    DefaultMetadataGeneratorContext(ClassReaderSource classSource, ClassLoader classLoader,
            Properties properties, ServiceRepository services) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.properties = properties;
        this.services = services;
    }

    @Override
    public ClassReaderSource getClassSource() {
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
    public ResourceTypeDescriptor getTypeDescriptor(Class<? extends Resource> type) {
        return proxyBuilder.getProxyFactory(type).typeDescriptor;
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
