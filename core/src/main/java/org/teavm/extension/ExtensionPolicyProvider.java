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
 * Supplies extension policies (like {@link org.teavm.extension.spi.reflection.ReflectionPolicy}) in addition
 * to ones found by {@link java.util.ServiceLoader}. Useful in environments where policies can't be loaded
 * from class loader, for example, when compiler itself is compiled by TeaVM.
 */
public interface ExtensionPolicyProvider {
    /**
     * Creates new instances of policies of given type. Called every time compiler needs to obtain a list
     * of policies, in the same manner as compiler would call {@link java.util.ServiceLoader#load(Class)}.
     *
     * @param env environment of current compilation, the same that is later passed to policies
     *            in {@code initialize} method.
     */
    <T> List<? extends T> createPolicies(ExtensionEnvironment env, Class<T> policyType);
}
