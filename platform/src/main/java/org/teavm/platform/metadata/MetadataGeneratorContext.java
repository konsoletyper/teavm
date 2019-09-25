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
package org.teavm.platform.metadata;

import java.util.Properties;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.platform.Platform;
import org.teavm.platform.plugin.ResourceTypeDescriptor;
import org.teavm.vm.TeaVM;

/**
 * <p>Represents context with compile-time information, that is useful for {@link MetadataGenerator}.
 * This context is provided by the compiler infrastructure.</p>
 *
 * @author Alexey Andreev
 */
public interface MetadataGeneratorContext extends ServiceRepository {
    /**
     * Gets the collection of all classes that were reached by the dependency analyzer.
     *
     * @return class source.
     */
    ClassReaderSource getClassSource();

    /**
     * Gets the class loader that is used by the compiler.
     * @return class loader.
     */
    ClassLoader getClassLoader();

    /**
     * Gets properties that were specified to {@link TeaVM}.
     *
     * @return properties.
     */
    Properties getProperties();

    /**
     * Creates a new resource of the given type. The description of valid resources
     * is available in documentation for {@link Resource}.
     *
     * @param resourceType type of resource to create.
     * @return a new resource
     */
    <T extends Resource> T createResource(Class<T> resourceType);

    /**
     * Creates a new resource that represents static field. Client code then may use
     * {@link Platform#objectFromResource(StaticFieldResource)} to get actual field value.
     *
     * @param field field for which to create resource.
     * @return a new resource.
     */
    StaticFieldResource createFieldResource(FieldReference field);

    /**
     * Creates a new resource array.
     *
     * @return a new resource.
     */
    <T extends Resource> ResourceArray<T> createResourceArray();

    /**
     * Creates a new resource map.
     *
     * @return a new resource.
     */
    <T extends Resource> ResourceMap<T> createResourceMap();

    ResourceTypeDescriptor getTypeDescriptor(Class<? extends Resource> type);
}
