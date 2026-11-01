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
package org.teavm.compiler.bootstrap.junit;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

@Autoregistered
public class TestReflectionPolicy extends SimpleReflectionPolicy {
    private static final String SUPPORT_PACKAGE = "org.teavm.classlib.support.";
    private static final String REFLECTABLE = SUPPORT_PACKAGE + "Reflectable";
    private static final String PROXIABLE = SUPPORT_PACKAGE + "Proxiable";
    private static final String PROXY_CONFIGURATION = SUPPORT_PACKAGE + "ProxyConfiguration";

    @Override
    protected void setup() {
        var env = environment();
        if (env.findClass(REFLECTABLE) == null) {
            return;
        }

        allClasses()
                .reflectableMembers(withAnnotation(REFLECTABLE))
                .reflectableMethods(withReturnType("java.lang.invoke.SerializedLambda")
                        .and(named("writeReplace")));

        selectClass("org.teavm.classlib.java.lang.TestObject").foundByName();

        IntrospectClass<Annotation> proxyConfiguration = env.findClass(PROXY_CONFIGURATION).asSubclassUnchecked();
        selectClasses(withAnnotation(PROXIABLE))
                .proxyable()
                .proxyableWith(cls ->
                    cls.annotations(proxyConfiguration).stream()
                            .map(a -> List.of((IntrospectClass<?>[]) a.value("value")))
                            .collect(Collectors.toList())
                );
    }
}
