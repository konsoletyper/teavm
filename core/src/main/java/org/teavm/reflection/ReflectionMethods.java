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
package org.teavm.reflection;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public final class ReflectionMethods {
    public static final String TYPE_VAR_IMPL = "java.lang.reflect.TypeVariableImpl";
    public static final ValueType.Object TYPE_VAR_IMPL_TYPE = ValueType.object(TYPE_VAR_IMPL);
    public static final MethodReference TYPE_VAR_GET_BOUNDS = new MethodReference(
            TYPE_VAR_IMPL, "getBounds", ValueType.parse(Type[].class));

    public static final String PARAM_TYPE_IMPL = "java.lang.reflect.ParameterizedTypeImpl";

    public static final String WILDCARD_TYPE_IMPL = "java.lang.reflect.WildcardTypeImpl";

    public static final String GENERIC_ARRAY_TYPE_IMPL = "java.lang.reflect.GenericArrayTypeImpl";

    public static final MethodReference CLASS_GET_TYPE_PARAMS = new MethodReference(Class.class, "getTypeParameters",
            TypeVariable[].class);
    public static final MethodReference FIELD_GET_GENERIC_TYPE = new MethodReference(Field.class,
            "getGenericType", Type.class);
    public static final MethodReference EXECUTABLE_GET_PARAMETER_TYPES = new MethodReference(Executable.class,
            "getParameterTypes", Class[].class);
    public static final MethodReference EXECUTABLE_GET_GENERIC_PARAMETER_TYPES = new MethodReference(Executable.class,
            "getGenericParameterTypes", Type[].class);
    public static final MethodReference EXECUTABLE_GET_TYPE_PARAMS = new MethodReference(Executable.class,
            "getTypeParameters", TypeVariable[].class);

    public static final MethodReference METHOD_GET_RETURN_TYPE = new MethodReference(Method.class, "getReturnType",
            Class.class);
    public static final MethodReference METHOD_GET_GENERIC_RETURN_TYPE = new MethodReference(Method.class,
            "getGenericReturnType", Type.class);

    private ReflectionMethods() {
    }
}
