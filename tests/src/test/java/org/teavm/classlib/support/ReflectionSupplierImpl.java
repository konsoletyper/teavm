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
package org.teavm.classlib.support;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.classlib.ProxyInterfaceConsumer;
import org.teavm.classlib.ProxyListener;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

public class ReflectionSupplierImpl implements ReflectionSupplier {
    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return Collections.emptyList();
        }
        Set<String> fields = new HashSet<>();
        for (FieldReader field : cls.getFields()) {
            if (field.getAnnotations().get(Reflectable.class.getName()) != null) {
                fields.add(field.getName());
            }
        }
        return fields;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return Collections.emptyList();
        }
        Set<MethodDescriptor> methods = new HashSet<>();
        for (MethodReader method : cls.getMethods()) {
            if (method.getAnnotations().get(Reflectable.class.getName()) != null) {
                methods.add(method.getDescriptor());
            } else if ("writeReplace".equals(method.getName())
                    && method.getResultType().isObject(SerializedLambda.class)) {
                //Required by org.teavm.classlib.java.lang.invoke.SerializedLambdaTest.
                methods.add(method.getDescriptor());
            }
        }
        return methods;
    }

    @Override
    public boolean isClassFoundByName(ReflectionContext context, String name) {
        return name.equals("org.teavm.classlib.java.lang.TestObject");
    }

    @Override
    public ProxyListener getProxyInterfaces(ReflectionContext context, ProxyInterfaceConsumer consumer) {
        return className -> {
            var cls = context.getClassSource().get(className);
            if (cls != null && cls.getAnnotations().get(Proxiable.class.getName()) != null) {
                consumer.accept(List.of(className));
                for (var annot : cls.getAnnotations().all()) {
                    if (annot.getType().equals(ProxyConfiguration.class.getName())) {
                        var otherList = annot.getValue("value").getList();
                        var classNames = new ArrayList<String>();
                        classNames.add(className);
                        for (var other : otherList) {
                            classNames.add(((ValueType.Object) other.getJavaClass()).getClassName());
                        }
                        consumer.accept(classNames);
                    }
                }
            }
        };
    }
}
