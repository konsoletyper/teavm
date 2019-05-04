/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.metaprogramming.impl.reflect;

import org.teavm.metaprogramming.reflect.ReflectParameterizedMember;
import org.teavm.metaprogramming.reflect.ReflectType;
import org.teavm.metaprogramming.reflect.ReflectTypeVariable;
import org.teavm.model.GenericValueType;

public class ReflectTypeVariableImpl implements ReflectTypeVariable {
    private ReflectContext context;
    private ReflectParameterizedMemberImpl owner;
    private String name;
    private GenericValueType.Reference[] innerBounds;
    private ReflectType[] bounds;

    public ReflectTypeVariableImpl(ReflectContext context, ReflectParameterizedMemberImpl owner, String name,
            GenericValueType.Reference[] innerBounds) {
        this.context = context;
        this.owner = owner;
        this.name = name;
        this.innerBounds = innerBounds;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ReflectType[] getBounds() {
        if (bounds == null) {
            if (innerBounds == null) {
                bounds = ReflectContext.EMPTY_TYPES;
            } else {
                bounds = new ReflectType[innerBounds.length];
                for (int i = 0; i < bounds.length; ++i) {
                    bounds[i] = context.getGenericType(innerBounds[i], owner.getTypeVariableMap());
                }
            }
        }
        return bounds.length == 0 ? bounds : bounds.clone();
    }

    @Override
    public ReflectParameterizedMember getOwner() {
        return owner;
    }
}
