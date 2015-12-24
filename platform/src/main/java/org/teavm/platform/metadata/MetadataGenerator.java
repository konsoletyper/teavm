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

import org.teavm.model.MethodReference;

/**
 * <p>Represents a generator, that produces resources during compilation phase. User must implement this
 * interface and bind this implementation to a method that would read resources at runtime.</p>
 *
 * <p>Here is the full workflow:</p>
 *
 * <ul>
 *   <li>Compiler finds a method that is marked with the {@link MetadataProvider} annotation.
 *       This method must be declared as <code>native</code>, otherwise compiler should throw an exception.</li>
 *   <li>Compiler instantiates the {@link MetadataGenerator} instance with the no-arg constructor
 *       If no such constructor exists, compiler throws exception.</li>
 *   <li>Compiler runs the {@link #generateMetadata(MetadataGeneratorContext, MethodReference)} method
 *       ands gets the produced resource.</li>
 *   <li>Compiler generates implementation of the method marked with {@link MetadataProvider}, that
 *       will return the generated resource in run time.</li>
 * </ul>
 *
 * <p>Therefore, the type of the value, returned by the
 * {@link #generateMetadata(MetadataGeneratorContext, MethodReference)}
 * method must match the returning type of the appropriate method, marked with {@link MetadataProvider}.</p>
 *
 * <p>The valid resource types are the following:</p>
 *
 * <ul>
 *   <li>Valid interfaces, extending the {@link Resource} annotation. Read the description of this interface
 *       for detailed description about valid resources interfaces.</li>
 *   <li>{@link ResourceArray} of valid resources.</li>
 *   <li>{@link ResourceMap} of valid resources.</li>
 *   <li>The <code>null</code> value.</li>
 * </ul>
 *
 * <p>All other types are not considered to be resources and therefore are not accepted.</p>
 *
 * @see ClassScopedMetadataGenerator
 *
 * @author Alexey Andreev
 */
public interface MetadataGenerator {
    /**
     * <p>Generates resources, that will be available at runtime.</p>
     *
     * @param context context that contains useful compile-time information.
     * @param method method which will be used to access the generated resources at run time.
     * @return resource to be attached to method at run time.
     */
    Resource generateMetadata(MetadataGeneratorContext context, MethodReference method);
}
