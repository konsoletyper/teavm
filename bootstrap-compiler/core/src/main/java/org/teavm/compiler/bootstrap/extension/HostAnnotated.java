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

import org.teavm.extension.bridge.BridgeAnnotated;
import org.teavm.extension.bridge.BridgeAnnotation;
import org.teavm.extension.introspect.IntrospectAnnotatedElement;

class HostAnnotated extends HostElement implements BridgeAnnotated {
    HostAnnotated(HostEnvironment env, Object underlying, int kind) {
        super(env, underlying, kind);
    }

    @Override
    public BridgeAnnotation[] annotations() {
        var underlying = underlying();
        if (!(underlying instanceof IntrospectAnnotatedElement)) {
            return new HostAnnotation[0];
        }
        var annotations = ((IntrospectAnnotatedElement) underlying).allAnnotations();
        var result = new HostAnnotation[annotations.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = env.annotation(annotations.get(i));
        }
        return result;
    }
}
