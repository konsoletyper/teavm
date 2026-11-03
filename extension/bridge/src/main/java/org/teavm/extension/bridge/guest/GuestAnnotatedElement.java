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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.extension.bridge.BridgeAnnotated;
import org.teavm.extension.introspect.IntrospectAnnotatedElement;
import org.teavm.extension.introspect.IntrospectAnnotation;
import org.teavm.extension.introspect.IntrospectClass;

abstract class GuestAnnotatedElement extends GuestElement implements IntrospectAnnotatedElement {
    private final BridgeAnnotated remoteAnnotated;
    private List<? extends IntrospectAnnotation<?>> allAnnotations;

    GuestAnnotatedElement(GuestEnvironment env, BridgeAnnotated remote) {
        super(env, remote);
        remoteAnnotated = remote;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> IntrospectAnnotation<A> annotation(IntrospectClass<A> type) {
        for (var annotation : allAnnotations()) {
            if (annotation.type() == type) {
                return (IntrospectAnnotation<A>) annotation;
            }
        }
        return null;
    }

    @Override
    public <A extends Annotation> IntrospectAnnotation<A> annotation(Class<A> type) {
        var cls = env.findClass(type);
        return cls != null ? annotation(cls) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(IntrospectClass<A> type) {
        List<IntrospectAnnotation<A>> result = null;
        for (var annotation : allAnnotations()) {
            if (annotation.type() == type) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add((IntrospectAnnotation<A>) annotation);
            }
        }
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    @Override
    public <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(Class<A> type) {
        var cls = env.findClass(type);
        return cls != null ? annotations(cls) : Collections.emptyList();
    }

    @Override
    public List<? extends IntrospectAnnotation<?>> allAnnotations() {
        if (allAnnotations == null) {
            allAnnotations = env.elements(remoteAnnotated.annotations());
        }
        return allAnnotations;
    }
}
