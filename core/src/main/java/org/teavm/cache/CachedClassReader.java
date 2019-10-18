/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;

class CachedClassReader extends CachedElement implements ClassReader {
    String parent;
    GenericTypeParameter[] parameters;
    GenericValueType.Object genericParent;
    String owner;
    String declaringClass;
    String simpleName;
    Set<String> interfaces;
    Set<GenericValueType.Object> genericInterfaces;
    Map<MethodDescriptor, CachedMethod> methods;
    Map<String, CachedField> fields;

    @Override
    public GenericTypeParameter[] getGenericParameters() {
        return parameters != null ? parameters.clone() : new GenericTypeParameter[0];
    }

    @Override
    public String getParent() {
        return parent;
    }

    @Override
    public GenericValueType.Object getGenericParent() {
        return genericParent;
    }

    @Override
    public Set<String> getInterfaces() {
        return interfaces;
    }

    @Override
    public Set<GenericValueType.Object> getGenericInterfaces() {
        return genericInterfaces;
    }

    @Override
    public MethodReader getMethod(MethodDescriptor method) {
        return methods.get(method);
    }

    @Override
    public Collection<? extends MethodReader> getMethods() {
        return methods.values();
    }

    @Override
    public FieldReader getField(String name) {
        return fields.get(name);
    }

    @Override
    public Collection<? extends FieldReader> getFields() {
        return fields.values();
    }

    @Override
    public String getOwnerName() {
        return owner;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClass;
    }
}
