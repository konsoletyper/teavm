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
package org.teavm.compiler.bootstrap.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.ExtensionPolicyProvider;
import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeGuest;
import org.teavm.extension.bridge.BridgeGuestModule;
import org.teavm.extension.bridge.BridgeProxyConsumer;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMember;
import org.teavm.extension.spi.reflection.ProxyInterfaceConsumer;
import org.teavm.extension.spi.reflection.ProxyListener;
import org.teavm.extension.spi.reflection.ReflectionPolicy;
import org.teavm.extension.spi.resources.ResourcesPolicy;

/**
 * Supplies policies, which run in guest module, see {@link org.teavm.extension.bridge}.
 * Guest instantiates policies once per compilation, so a new instance of provider should be used for every
 * compilation.
 */
public class GuestPolicyProvider implements ExtensionPolicyProvider {
    private final BridgeGuestModule module;
    private ExtensionEnvironment currentEnv;
    private HostEnvironment host;
    private BridgeGuest guest;

    public GuestPolicyProvider(BridgeGuestModule module) {
        this.module = module;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<? extends T> createPolicies(ExtensionEnvironment env, Class<T> policyType) {
        if (policyType == ReflectionPolicy.class) {
            ensureGuest(env);
            return guest.hasReflectionPolicies()
                    ? List.of((T) new GuestReflectionPolicy())
                    : Collections.emptyList();
        } else if (policyType == ResourcesPolicy.class) {
            ensureGuest(env);
            return guest.hasResourcesPolicies()
                    ? List.of((T) new GuestResourcesPolicy())
                    : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private void ensureGuest(ExtensionEnvironment env) {
        if (currentEnv != env) {
            currentEnv = env;
            host = new HostEnvironment(env);
            guest = module.create(host);
        }
    }

    private class GuestReflectionPolicy implements ReflectionPolicy {
        @Override
        public void initialize(ExtensionEnvironment env) {
            guest.initializeReflectionPolicies();
        }

        @Override
        public Collection<IntrospectMember> classAccessibleMembers(IntrospectClass<?> cls) {
            return Stream.of(guest.classAccessibleMembers(host.cls(cls)))
                    .map(member -> ((HostMember) member).member())
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isClassFoundByName(IntrospectClass<?> cls) {
            return guest.isClassFoundByName(host.cls(cls));
        }

        @Override
        public ProxyListener getProxyInterfaces(ProxyInterfaceConsumer consumer) {
            var hasListener = guest.getProxyInterfaces(new ProxyConsumer(consumer));
            return hasListener ? cls -> guest.onClassAdded(host.cls(cls)) : null;
        }
    }

    private class ProxyConsumer implements BridgeProxyConsumer {
        private ProxyInterfaceConsumer consumer;

        ProxyConsumer(ProxyInterfaceConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(BridgeClass[] interfaces) {
            consumer.accept(List.of(host.unwrapClasses(interfaces)));
        }
    }

    private class GuestResourcesPolicy implements ResourcesPolicy {
        @Override
        public String[] supplyResources(Collection<? extends String> availableClassNames) {
            return guest.supplyResources(availableClassNames.toArray(new String[0]));
        }
    }
}
