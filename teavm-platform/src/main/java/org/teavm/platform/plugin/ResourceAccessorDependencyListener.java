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
package org.teavm.platform.plugin;

import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class ResourceAccessorDependencyListener implements DependencyListener {
    @Override
    public void started(DependencyAgent agent) {
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className) {
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method) {
        switch (method.getReference().getName()) {
            case "castToIntWrapper":
                castToWrapper(agent, method, Integer.class, int.class);
                break;
            case "castToShortWrapper":
                castToWrapper(agent, method, Short.class, short.class);
                break;
            case "castToByteWrapper":
                castToWrapper(agent, method, Byte.class, byte.class);
                break;
            case "castToBooleanWrapper":
                castToWrapper(agent, method, Boolean.class, boolean.class);
                break;
            case "castToFloatWrapper":
                castToWrapper(agent, method, Float.class, float.class);
                break;
            case "castToDoubleWrapper":
                castToWrapper(agent, method, Double.class, double.class);
                break;
            case "castFromIntegerWrapper":
                castFromWrapper(agent, method, Integer.class, int.class);
                break;
            case "castFromShortWrapper":
                castFromWrapper(agent, method, Short.class, short.class);
                break;
            case "castFromByteWrapper":
                castFromWrapper(agent, method, Byte.class, byte.class);
                break;
            case "castFromBooleanWrapper":
                castFromWrapper(agent, method, Boolean.class, boolean.class);
                break;
            case "castFromFloatWrapper":
                castFromWrapper(agent, method, Float.class, float.class);
                break;
            case "castFromDoubleWrapper":
                castFromWrapper(agent, method, Double.class, double.class);
                break;
            case "castToString":
                method.getResult().propagate("java.lang.String");
                break;
        }
    }

    private void castToWrapper(DependencyAgent agent, MethodDependency method, Class<?> wrapper, Class<?> primitive) {
        method.getResult().propagate(wrapper.getName());
        agent.linkMethod(new MethodReference(wrapper, "valueOf", primitive, wrapper), method.getStack()).use();
    }

    private void castFromWrapper(DependencyAgent agent, MethodDependency method, Class<?> wrapper, Class<?> primitive) {
        String primitiveName = primitive.getName();
        agent.linkMethod(new MethodReference(wrapper, primitiveName + "Value", primitive), method.getStack()).use();
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field) {
    }
}
