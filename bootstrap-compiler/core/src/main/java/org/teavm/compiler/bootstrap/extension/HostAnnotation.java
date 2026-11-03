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

import org.teavm.extension.bridge.BridgeAnnotation;
import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeMethod;
import org.teavm.extension.bridge.BridgeValue;
import org.teavm.extension.introspect.IntrospectAnnotation;

class HostAnnotation extends HostElement implements BridgeAnnotation {
    final IntrospectAnnotation<?> annotation;

    HostAnnotation(HostEnvironment env, IntrospectAnnotation<?> annotation) {
        super(env, annotation, BridgeKinds.ANNOTATION);
        this.annotation = annotation;
    }

    @Override
    public BridgeClass type() {
        return env.cls(annotation.type());
    }

    @Override
    public BridgeValue value(BridgeMethod method) {
        return HostValue.of(env, annotation.value(((HostMethod) method).method));
    }
}
