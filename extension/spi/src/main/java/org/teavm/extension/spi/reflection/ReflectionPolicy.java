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
package org.teavm.extension.spi.reflection;

import java.util.Collection;
import java.util.Collections;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMember;

public interface ReflectionPolicy {
    default void initialize(ExtensionEnvironment env) {
    }
    
    default Collection<IntrospectMember> classAccessibleMembers(IntrospectClass<?> cls) {
        return Collections.emptyList();
    }

    default boolean isClassFoundByName(IntrospectClass<?> cls) {
        return false;
    }

    default ProxyListener getProxyInterfaces(ProxyInterfaceConsumer consumer) {
        return null;
    }
}
