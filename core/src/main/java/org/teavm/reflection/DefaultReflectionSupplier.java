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
package org.teavm.reflection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.classlib.ProxyInterfaceConsumer;
import org.teavm.classlib.ProxyListener;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.extension.ExtensionEnvironmentImpl;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectFieldImpl;
import org.teavm.extension.introspect.IntrospectMethodImpl;
import org.teavm.extension.spi.reflection.ReflectionPolicy;
import org.teavm.model.MethodDescriptor;

public class DefaultReflectionSupplier implements ReflectionSupplier {
    private ExtensionEnvironmentImpl env;
    private List<ReflectionPolicy> policies;
    private Map<String, Set<String>> pendingAccessibleFields = new HashMap<>();
    private Map<String, Set<MethodDescriptor>> pendingAccessibleMethods = new HashMap<>();

    public DefaultReflectionSupplier(ExtensionEnvironmentImpl env) {
        this.env = env;
        policies = ServiceLoader.load(ReflectionPolicy.class, env.classLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        var fields = pendingAccessibleFields.remove(className);
        if (fields != null) {
            return fields;
        }
        fields = new LinkedHashSet<>();
        var methods = new LinkedHashSet<MethodDescriptor>();
        pendingAccessibleMethods.put(className, methods);
        fillMembers(className, fields, methods);
        return fields;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        var methods = pendingAccessibleMethods.remove(className);
        if (methods != null) {
            return methods;
        }
        methods = new LinkedHashSet<>();
        var fields = new LinkedHashSet<String>();
        pendingAccessibleFields.put(className, fields);
        fillMembers(className, fields, methods);
        return methods;
    }

    @Override
    public boolean isClassFoundByName(ReflectionContext context, String name) {
        var introspectClass = env.findClass(name);
        for (var policy : policies) {
            if (policy.isClassFoundByName(env, introspectClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ProxyListener getProxyInterfaces(ReflectionContext context, ProxyInterfaceConsumer consumer) {
        var policyListeners = new ArrayList<org.teavm.extension.spi.reflection.ProxyListener>();
        for (var policy : policies) {
            var listener = policy.getProxyInterfaces(env, itfList -> {
                consumer.accept(itfList.stream().map(IntrospectClass::name).collect(Collectors.toList()));
            });
            if (listener != null) {
                policyListeners.add(listener);
            }
        }
        return policyListeners.isEmpty() ? null : className -> {
            for (var listener : policyListeners) {
                listener.onClassAdded(env.findClass(className));
            }
        };
    }

    private void fillMembers(String className, Collection<String> fields, Collection<MethodDescriptor> methods) {
        var introspectClass = env.findClass(className);
        for (var policy : policies) {
            var members = policy.classAccessibleMembers(env, introspectClass);
            for (var member : members) {
                if (member instanceof IntrospectFieldImpl) {
                    fields.add(member.name());
                } else if (member instanceof IntrospectMethodImpl) {
                    methods.add(((IntrospectMethodImpl) member).method.getDescriptor());
                }
            }
        }
    }
}
