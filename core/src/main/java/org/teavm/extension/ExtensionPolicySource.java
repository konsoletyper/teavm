/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension;

import java.util.List;

/**
 * Creates instances of extension policies (like {@link org.teavm.extension.spi.reflection.ReflectionPolicy})
 * available in current compilation.
 */
public interface ExtensionPolicySource {
    /**
     * Creates new instances of all available policies of given type. Policies are not initialized.
     */
    <T> List<T> createPolicies(Class<T> policyType);
}
