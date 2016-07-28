/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.vm;

import java.util.HashMap;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

/**
 * <p>An entry point to a generated VM that is used to enter the VM from a JavaScript code.
 * The entry point is added by {@link TeaVM#entryPoint(String, MethodReference)}.
 * Use {@link #withValue(int, String)} to specify actual types that are passed to the entry point.</p>
 *
 * <p>In the simple case of static method without arguments you won't deal with this class. But
 * sometimes you have to. Consider the following example:</p>
 *
 * <pre>{@code
 *static void entryPoint(Map<Object, Object> map) {
 *    for (Map.Entry<Object, Object> entry : map.entrySet()) {
 *        System.out.println(entry.getKey() + " => " + entry.getValue());
 *    }
 *}}</pre>
 *
 * <p>Now you want to call this method from JavaScript, and you pass a {@link HashMap} to this method.
 * Let's see how you achieve it:</p>
 *
 * <pre>{@code
 *vm.exportType("JavaHashMap", "java.util.HashMap");
 *vm.entryPoint("initJavaHashMap", new MethodReference("java.util.HashMap",
 *        "<init>", ValueType.VOID));
 *vm.entryPoint("putValueIntoJavaMap", new MethodReference(
 *        "java.util.Map", "put",
 *        ValueType.object("java.lang.Object"), ValueType.object("java.lang.Object"),
 *        ValueType.object("java.lang.Object")))
 *        .withValue(0, "java.util.HashMap")
 *        .withValue(1, "java.lang.String")
 *        .withValue(2, "java.lang.String");
 *vm.entryPoint("entryPoint", new MethodReference(
 *        "fully.qualified.ClassName", "entryPoint",
 *        ValueType.object("java.util.Map"), ValueType.VOID))
 *        .withValue(1, "java.util.HashMap")
 *}</pre>
 *
 * <p>And in JavaScript you would do the following:</p>
 *
 * <pre>{@code
 *var map = new JavaHashMap();
 *initJavaHashMap(map);
 *putValueIntoJavaMap(map, $rt_str("foo"), $rt_str("bar"));
 *entryPoint(map);
 *}</pre>
 *
 * <p>If you didn't call <code>.withValue(1, "java.util.HashMap")</code>, TeaVM could not know,
 * what implementation of <code>entrySet</code> method to include.</p>
 *
 * @author Alexey Andreev
 */
public class TeaVMEntryPoint {
    private String publicName;
    MethodReference reference;
    private MethodDependency method;
    private boolean async;

    TeaVMEntryPoint(String publicName, MethodReference reference, MethodDependency method) {
        this.publicName = publicName;
        this.reference = reference;
        this.method = method;
        method.use();
    }

    public MethodReference getReference() {
        return reference;
    }

    public String getPublicName() {
        return publicName;
    }

    boolean isAsync() {
        return async;
    }

    public TeaVMEntryPoint withValue(int argument, String type) {
        if (argument > reference.parameterCount()) {
            throw new IllegalArgumentException("Illegal argument #" + argument + " of " + reference.parameterCount());
        }
        method.getVariable(argument).propagate(method.getDependencyAgent().getType(type));
        return this;
    }

    public TeaVMEntryPoint withArrayValue(int argument, String type) {
        if (argument > reference.parameterCount()) {
            throw new IllegalArgumentException("Illegal argument #" + argument + " of " + reference.parameterCount());
        }
        method.getVariable(argument).getArrayItem().propagate(method.getDependencyAgent().getType(type));
        return this;
    }

    public TeaVMEntryPoint async() {
        this.async = true;
        return this;
    }
}
