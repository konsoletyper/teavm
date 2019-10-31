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
package org.teavm.backend.javascript.codegen;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class DefaultNamingStrategy implements NamingStrategy {
    private static final byte NO_CLASSIFIER = 0;
    private static final byte INIT_CLASSIFIER = 1;

    private final AliasProvider aliasProvider;
    private final ClassReaderSource classSource;
    private final Map<MethodDescriptor, String> aliases = new HashMap<>();
    private final Map<Key, ScopedName> privateAliases = new HashMap<>();
    private final Map<String, ScopedName> classAliases = new HashMap<>();
    private final Map<FieldReference, String> fieldAliases = new HashMap<>();
    private final Map<FieldReference, ScopedName> staticFieldAliases = new HashMap<>();
    private final Map<String, String> functionAliases = new HashMap<>();
    private final Map<String, ScopedName> classInitAliases = new HashMap<>();
    private String scopeName;

    public DefaultNamingStrategy(AliasProvider aliasProvider, ClassReaderSource classSource) {
        this.aliasProvider = aliasProvider;
        this.classSource = classSource;
    }

    @Override
    public ScopedName getNameFor(String cls) {
        return classAliases.computeIfAbsent(cls, key -> aliasProvider.getClassAlias(cls));
    }

    @Override
    public String getNameFor(MethodDescriptor method) {
        String alias = aliases.get(method);
        if (alias == null) {
            alias = aliasProvider.getMethodAlias(method);
            aliases.put(method, alias);
        }
        return alias;
    }

    @Override
    public ScopedName getFullNameFor(MethodReference method) {
        return getFullNameFor(method, NO_CLASSIFIER);
    }

    @Override
    public ScopedName getNameForInit(MethodReference method) {
        return getFullNameFor(method, INIT_CLASSIFIER);
    }

    private ScopedName getFullNameFor(MethodReference method, byte classifier) {
        MethodReference originalMethod = method;
        method = getRealMethod(method);
        if (method == null) {
            method = originalMethod;
        }

        return privateAliases.computeIfAbsent(new Key(classifier, method),
                key -> aliasProvider.getStaticMethodAlias(key.data));
    }

    @Override
    public String getNameFor(FieldReference field) {
        String alias = fieldAliases.get(field);
        if (alias == null) {
            FieldReference realField = getRealField(field);
            if (realField.equals(field)) {
                alias = aliasProvider.getFieldAlias(realField);
            } else {
                alias = getNameFor(realField);
            }
            fieldAliases.put(field, alias);
        }
        return alias;
    }

    @Override
    public ScopedName getFullNameFor(FieldReference field) {
        ScopedName alias = staticFieldAliases.get(field);
        if (alias == null) {
            FieldReference realField = getRealField(field);
            if (realField.equals(field)) {
                alias = aliasProvider.getStaticFieldAlias(realField);
            } else {
                alias = getFullNameFor(realField);
            }
            staticFieldAliases.put(field, alias);
        }
        return alias;
    }

    @Override
    public String getNameForFunction(String name) {
        return functionAliases.computeIfAbsent(name, key -> aliasProvider.getFunctionAlias(key));
    }

    @Override
    public ScopedName getNameForClassInit(String className) {
        return classInitAliases.computeIfAbsent(className, key -> aliasProvider.getClassInitAlias(key));
    }

    @Override
    public String getScopeName() {
        if (scopeName == null) {
            scopeName = aliasProvider.getScopeAlias();
        }
        return scopeName;
    }

    private MethodReference getRealMethod(MethodReference methodRef) {
        String className = methodRef.getClassName();
        while (className != null) {
            ClassReader cls = classSource.get(className);
            if (cls == null) {
                return null;
            }
            MethodReader method = cls.getMethod(methodRef.getDescriptor());
            if (method != null) {
                if (method.getLevel() == AccessLevel.PRIVATE && !className.equals(methodRef.getClassName())) {
                    return null;
                }
                return method.getReference();
            }
            className = cls.getParent();
        }
        return null;
    }

    private FieldReference getRealField(FieldReference fieldRef) {
        String cls = fieldRef.getClassName();
        while (cls != null) {
            ClassReader clsReader = classSource.get(cls);
            if (clsReader == null) {
                break;
            }
            FieldReader fieldReader = clsReader.getField(fieldRef.getFieldName());
            if (fieldReader != null) {
                return fieldReader.getReference();
            }
            cls = clsReader.getParent();
        }
        return fieldRef;
    }

    static final class Key {
        final MethodReference data;
        int hash;
        final byte classifier;

        Key(byte classifier, MethodReference data) {
            this.classifier = classifier;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return classifier == key.classifier && data.equals(key.data);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = (classifier * 31 + data.hashCode()) * 17;
                if (hash == 0) {
                    hash++;
                }
            }
            return hash;
        }
    }
}
