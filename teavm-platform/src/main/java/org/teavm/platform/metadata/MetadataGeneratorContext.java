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
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.platform.Platform;
import org.teavm.vm.TeaVM;

/**
 * <p>Represents context with compile-time information, that is useful for {@link MetadataGenerator}.
 * This context is provided by the compiler infrastructure.</p>
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface MetadataGeneratorContext extends ServiceRepository {
    /**
     * Gets the collection of all classes that were achieved by the dependency checker.
     */
    ListableClassReaderSource getClassSource();

    /**
     * Gets the class loader that is used by the compiler.
     */
    ClassLoader getClassLoader();

    /**
     * Gets properties that were specified to {@link TeaVM}.
     */
    Properties getProperties();

    /**
     * Creates a new resource of the given type. The description of valid resources
     * is available in documentation for {@link Resource}.
     */
    <T extends Resource> T createResource(Class<T> resourceType);

    /**
     * Creates a new resource that represents class literal. Client code then may use
     * {@link Platform#classFromResource(ClassResource)} to get actual class.
     */
    ClassResource createClassResource(String className);

    /**
     * Creates a new resource that represents static field. Client code then may use
     * {@link Platform#objectFromResource(StaticFieldResource)} to get actual field value.
     */
    StaticFieldResource createFieldResource(FieldReference field);

    /**
     * Creates a new resource array.
     */
    <T extends Resource> ResourceArray<T> createResourceArray();

    /**
     * Creates a new resource map.
     */
    <T extends Resource> ResourceMap<T> createResourceMap();
}
