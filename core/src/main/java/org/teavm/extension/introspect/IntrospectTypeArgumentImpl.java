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
package org.teavm.extension.introspect;

import org.teavm.model.GenericValueType;
import org.teavm.model.ValueType;

public class IntrospectTypeArgumentImpl implements IntrospectTypeArgument {
    private final GenericValueType.Argument argument;
    private final IntrospectClassImpl<?> declaringClass;
    private final IntrospectMethodImpl declaringMethod;
    private final Introspection introspection;
    private IntrospectType type;

    IntrospectTypeArgumentImpl(GenericValueType.Argument argument, IntrospectClassImpl<?> declaringClass,
            IntrospectMethodImpl declaringMethod, Introspection introspection) {
        this.argument = argument;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.introspection = introspection;
    }

    @Override
    public IntrospectProjection projection() {
        switch (argument.getKind()) {
            case INVARIANT:
                return IntrospectProjection.EXACT;
            case COVARIANT:
            case ANY:
                return IntrospectProjection.EXTENDS;
            case CONTRAVARIANT:
                return IntrospectProjection.SUPER;
            default:
                throw new AssertionError("Unknown kind: " + argument.getKind());
        }
    }

    @Override
    public String toString() {
        if (argument.getValue() == null) {
            return "?";
        }
        switch (projection()) {
            case EXACT:
                return type().toString();
            case EXTENDS:
                return "? extends " + type();
            case SUPER:
                return "? super " + type();
            default:
                throw new AssertionError("Unknown projection: " + projection());
        }
    }

    @Override
    public IntrospectType type() {
        if (type == null) {
            if (argument.getValue() == null) {
                type = introspection.getClass(ValueType.object("java.lang.Object"));
            } else {
                type = introspection.convertGenericType(argument.getValue(), declaringClass, declaringMethod);
            }
        }
        return type;
    }
}
