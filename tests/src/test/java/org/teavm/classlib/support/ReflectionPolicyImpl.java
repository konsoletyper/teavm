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
import java.util.List;
import java.util.stream.Collectors;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

@Autoregistered
public class ReflectionPolicyImpl extends SimpleReflectionPolicy {
    @Override
    protected void setup() {
        allClasses()
                .reflectableMembers(withAnnotation(Reflectable.class))
                .reflectableMethods(withReturnType(SerializedLambda.class).and(named("writeReplace")));

        selectClass("org.teavm.classlib.java.lang.TestObject").foundByName();

        selectClasses(withAnnotation(Proxiable.class))
                .proxyable()
                .proxyableWith(cls ->
                    cls.annotations(ProxyConfiguration.class).stream()
                            .map(a -> List.of((IntrospectClass<?>[]) a.value("value")))
                            .collect(Collectors.toList())
                );
    }
}
