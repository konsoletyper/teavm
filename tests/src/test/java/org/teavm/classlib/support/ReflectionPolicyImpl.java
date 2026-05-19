/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.classlib.support;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMember;
import org.teavm.extension.spi.reflection.ProxyInterfaceConsumer;
import org.teavm.extension.spi.reflection.ProxyListener;
import org.teavm.extension.spi.reflection.ReflectionPolicy;

@Autoregistered
public class ReflectionPolicyImpl implements ReflectionPolicy {
    private ExtensionEnvironment env;

    @Override
    public void initialize(ExtensionEnvironment env) {
        this.env = env;
    }

    @Override
    public Collection<IntrospectMember> classAccessibleMembers(IntrospectClass<?> cls) {
        var members = new ArrayList<IntrospectMember>();
        for (var field : cls.declaredFields()) {
            if (field.hasAnnotation(Reflectable.class)) {
                members.add(field);
            }
        }
        for (var method : cls.declaredMethods()) {
            if (method.hasAnnotation(Reflectable.class)) {
                members.add(method);
            } else if (method.returnType().equals(env.findClass(SerializedLambda.class))
                    && method.name().equals("writeReplace")) {
                members.add(method);
            }
        }
        return members;
    }

    @Override
    public boolean isClassFoundByName(IntrospectClass<?> cls) {
        return cls.name().equals("org.teavm.classlib.java.lang.TestObject");
    }

    @Override
    public ProxyListener getProxyInterfaces(ProxyInterfaceConsumer consumer) {
        return cls -> {
            if (cls.hasAnnotation(Proxiable.class)) {
                consumer.accept(List.of(cls));
                for (var proxyConfig : cls.annotations(ProxyConfiguration.class)) {
                    var classes = (IntrospectClass<?>[]) proxyConfig.value("value");
                    var allClasses = new ArrayList<IntrospectClass<?>>();
                    allClasses.add(cls);
                    allClasses.addAll(List.of(classes));
                    consumer.accept(allClasses);
                }
            }
        };
    }
}
