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
import org.teavm.platform.metadata.builders.ResourceBuilder;

public interface MetadataGenerator {
    /**
     * <p>Generates resources, that will be available at runtime.</p>
     *
     * @param context context that contains useful compile-time information.
     * @param method method which will be used to access the generated resources at run time.
     * @return resource to be attached to method at run time.
     */
    ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method);
}
