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
package org.teavm.extension.bridge.guest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeGuest;
import org.teavm.extension.bridge.BridgeMember;
import org.teavm.extension.bridge.BridgeProxyConsumer;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.spi.reflection.ProxyListener;
import org.teavm.extension.spi.reflection.ReflectionPolicy;
import org.teavm.extension.spi.resources.ResourcesPolicy;

class GuestBridge implements BridgeGuest {
    private GuestEnvironment env;
    private List<ReflectionPolicy> reflectionPolicies;
    private List<ResourcesPolicy> resourcesPolicies;
    private List<ProxyListener> proxyListeners = new ArrayList<>();

    GuestBridge(GuestEnvironment env, List<ReflectionPolicy> reflectionPolicies,
            List<ResourcesPolicy> resourcesPolicies) {
        this.env = env;
        this.reflectionPolicies = reflectionPolicies;
        this.resourcesPolicies = resourcesPolicies;
    }

    @Override
    public boolean hasReflectionPolicies() {
        return !reflectionPolicies.isEmpty();
    }

    @Override
    public boolean hasResourcesPolicies() {
        return !resourcesPolicies.isEmpty();
    }

    @Override
    public void initializeReflectionPolicies() {
        for (var policy : reflectionPolicies) {
            policy.initialize(env);
        }
    }

    @Override
    public BridgeMember[] classAccessibleMembers(BridgeClass cls) {
        var introspectClass = (IntrospectClass<?>) env.element(cls);
        var members = new LinkedHashSet<Object>();
        for (var policy : reflectionPolicies) {
            var policyMembers = policy.classAccessibleMembers(introspectClass);
            if (policyMembers != null) {
                members.addAll(policyMembers);
            }
        }
        return env.remoteMembers(new ArrayList<>(members));
    }

    @Override
    public boolean isClassFoundByName(BridgeClass cls) {
        var introspectClass = (IntrospectClass<?>) env.element(cls);
        for (var policy : reflectionPolicies) {
            if (policy.isClassFoundByName(introspectClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean getProxyInterfaces(BridgeProxyConsumer consumer) {
        proxyListeners.clear();
        for (var policy : reflectionPolicies) {
            var listener = policy.getProxyInterfaces(interfaces -> consumer.accept(env.remoteClasses(
                    interfaces.toArray(new IntrospectClass<?>[0]))));
            if (listener != null) {
                proxyListeners.add(listener);
            }
        }
        return !proxyListeners.isEmpty();
    }

    @Override
    public void onClassAdded(BridgeClass cls) {
        var introspectClass = (IntrospectClass<?>) env.element(cls);
        for (var listener : proxyListeners) {
            listener.onClassAdded(introspectClass);
        }
    }

    @Override
    public String[] supplyResources(String[] availableClassNames) {
        var classNames = List.of(availableClassNames);
        var result = new LinkedHashSet<String>();
        for (var policy : resourcesPolicies) {
            policy.initialize(env);
            var resources = policy.supplyResources(classNames);
            if (resources != null) {
                result.addAll(List.of(resources));
            }
        }
        return result.toArray(new String[0]);
    }
}
