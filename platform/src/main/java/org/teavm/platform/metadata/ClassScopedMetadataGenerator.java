/*
 *  Copyright 2015 Alexey Andreev.
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

import java.util.Collection;
import java.util.Map;
import org.teavm.model.MethodReference;
import org.teavm.platform.PlatformClass;

/**
 * <p>Behaviour of this class is similar to {@link MetadataGenerator}. The difference is that method, marked with
 * {@link ClassScopedMetadataProvider} must take one argument of type {@link PlatformClass}. It will
 * return different resource for each given class, corresponding to map entries, produced by
 * {@link #generateMetadata(MetadataGeneratorContext, Collection, MethodReference)}.
 *
 * @see ClassScopedMetadataProvider
 * @see MetadataGenerator
 *
 * @author Alexey Andreev
 */
public interface ClassScopedMetadataGenerator {
    Map<String, Resource> generateMetadata(MetadataGeneratorContext context, Collection<? extends String> classNames,
            MethodReference method);
}
