/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c;

import java.util.Properties;
import org.teavm.backend.c.generators.GeneratorFactoryContext;
import org.teavm.backend.c.intrinsic.IntrinsicFactoryContext;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;

class IntrinsicFactoryContextImpl implements IntrinsicFactoryContext, GeneratorFactoryContext {
    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;

    IntrinsicFactoryContextImpl(ClassReaderSource classSource, ClassLoader classLoader, ServiceRepository services,
            Properties properties) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.services = services;
        this.properties = properties;
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
    public ServiceRepository getServices() {
        return services;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
