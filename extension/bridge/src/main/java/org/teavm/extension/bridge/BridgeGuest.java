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
package org.teavm.extension.bridge;

import org.teavm.jso.JSObject;

/**
 * Exposes extensions, contained in guest module, to compiler. Extensions of the same kind
 * are combined into one: methods of this interface aggregate results of all extensions of given kind.
 */
public interface BridgeGuest extends JSObject {
    boolean hasReflectionPolicies();

    boolean hasResourcesPolicies();

    /**
     * Calls {@code initialize} of every {@link org.teavm.extension.spi.reflection.ReflectionPolicy}.
     */
    void initializeReflectionPolicies();

    BridgeMember[] classAccessibleMembers(BridgeClass cls);

    boolean isClassFoundByName(BridgeClass cls);

    /**
     * Requests interfaces for proxies from all reflection policies.
     *
     * @return whether some policy wants to be notified about classes, see {@link #onClassAdded(BridgeClass)}.
     */
    boolean getProxyInterfaces(BridgeProxyConsumer consumer);

    void onClassAdded(BridgeClass cls);

    /**
     * Calls {@code initialize} and {@code supplyResources} of every
     * {@link org.teavm.extension.spi.resources.ResourcesPolicy}.
     */
    String[] supplyResources(String[] availableClassNames);
}
