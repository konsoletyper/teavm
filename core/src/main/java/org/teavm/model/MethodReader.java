/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model;

public interface MethodReader extends MemberReader {
    ValueType getResultType();

    GenericTypeParameter[] getTypeParameters();

    GenericValueType getGenericResultType();

    int parameterCount();

    ValueType[] getSignature();

    ValueType parameterType(int index);

    int genericParameterCount();

    GenericValueType genericParameterType(int index);

    ValueType[] getParameterTypes();

    AnnotationContainerReader parameterAnnotation(int index);

    AnnotationContainerReader[] getParameterAnnotations();

    MethodDescriptor getDescriptor();

    MethodReference getReference();

    ProgramReader getProgram();

    AnnotationValue getAnnotationDefault();
}
