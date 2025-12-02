/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import org.teavm.classlib.java.lang.TClass;

public class TTypeVariableStub implements TType {
    public final int level;
    public final int index;

    private TTypeVariableStub(int level, int index) {
        this.level = level;
        this.index = index;
    }

    static TTypeVariableStub create(int index, int level) {
        return new TTypeVariableStub(level, index);
    }

    static TTypeVariableStub create(int index) {
        return create(index, 0);
    }

    static TType resolve(TType type, TGenericDeclaration declaration) {
        if (type instanceof TTypeVariableStub) {
            var stub = (TTypeVariableStub) type;
            if (stub.level == 0) {
                type = declaration.getTypeParameters()[stub.index];
            } else {
                var declaringClass = declaration instanceof TMember
                        ? ((TMember) declaration).getDeclaringClass()
                        : ((TClass<?>) declaration).getDeclaringClass();
                type = declaringClass.getTypeParameters()[stub.index];
            }
        }
        if (type instanceof TLazyResolvedType) {
            ((TLazyResolvedType) type).resolve(declaration);
        }
        return type;
    }
}
