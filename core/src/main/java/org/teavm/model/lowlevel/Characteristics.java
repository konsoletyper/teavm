/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.model.lowlevel;

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import org.teavm.interop.Function;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

public class Characteristics {
    private ClassReaderSource classSource;
    private ObjectByteMap<String> isStructure = new ObjectByteHashMap<>();
    private ObjectByteMap<String> isStaticInit = new ObjectByteHashMap<>();
    private ObjectByteMap<String> isFunction = new ObjectByteHashMap<>();
    private ObjectByteMap<MethodReference> isManaged = new ObjectByteHashMap<>();

    public Characteristics(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public boolean isStructure(String className) {
        byte result = isStructure.getOrDefault(className, (byte) -1);
        if (result < 0) {
            if (className.equals(Structure.class.getName())) {
                result = 1;
            } else {
                ClassReader cls = classSource.get(className);
                if (cls != null && cls.getParent() != null) {
                    result = isStructure(cls.getParent()) ? (byte) 1 : 0;
                } else {
                    result = 0;
                }
            }
            isStructure.put(className, result);
        }
        return result != 0;
    }

    public boolean isStaticInit(String className) {
        byte result = isStaticInit.getOrDefault(className, (byte) -1);
        if (result < 0) {
            ClassReader cls = classSource.get(className);
            result = cls != null && cls.getAnnotations().get(StaticInit.class.getName()) != null ? (byte) 1 : 0;
            isStaticInit.put(className, result);
        }
        return result != 0;
    }

    public boolean isFunction(String className) {
        byte result = isFunction.getOrDefault(className, (byte) -1);
        if (result < 0) {
            if (className.equals(Function.class.getName())) {
                result = 1;
            } else {
                ClassReader cls = classSource.get(className);
                if (cls != null && cls.getParent() != null) {
                    result = isFunction(cls.getParent()) ? (byte) 1 : 0;
                } else {
                    result = 0;
                }
            }
            isFunction.put(className, result);
        }
        return result != 0;
    }

    public boolean isManaged(MethodReference methodReference) {
        byte result = isManaged.getOrDefault(methodReference, (byte) -1);
        if (result < 0) {
            result = computeIsManaged(methodReference) ? (byte) 1 : 0;
            isManaged.put(methodReference, result);
        }
        return result != 0;
    }

    private boolean computeIsManaged(MethodReference methodReference) {
        MethodReader method = classSource.resolve(methodReference);
        if (method == null) {
            return true;
        }

        ClassReader cls = classSource.get(method.getOwnerName());
        if (cls.getAnnotations().get(Unmanaged.class.getName()) != null) {
            return false;
        }
        return method == null || method.getAnnotations().get(Unmanaged.class.getName()) == null;
    }
}
