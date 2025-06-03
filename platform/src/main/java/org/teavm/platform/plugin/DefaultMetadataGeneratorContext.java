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

import java.util.Properties;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.platform.metadata.MetadataGeneratorContext;

public class DefaultMetadataGeneratorContext implements MetadataGeneratorContext {
    private ClassReaderSource classSource;
    private ResourceProvider resourceProvider;
    private ClassLoader classLoader;
    private Properties properties;
    private ServiceRepository services;

    public DefaultMetadataGeneratorContext(ClassReaderSource classSource, ResourceProvider resourceProvider,
            ClassLoader classLoader, Properties properties, ServiceRepository services) {
        this.classSource = classSource;
        this.resourceProvider = resourceProvider;
        this.classLoader = classLoader;
        this.properties = properties;
        this.services = services;
    }

    @Override
    public ResourceProvider getResourceProvider() {
        return resourceProvider;
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
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }
}
