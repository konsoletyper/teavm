/*
 *  Copyright 2017 Alexey Andreev.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataProvider;

public final class MetadataUtils {
    private MetadataUtils() {
    }

    public static MetadataGenerator createMetadataGenerator(ClassLoader classLoader, MethodReader method,
            CallLocation callLocation, Diagnostics diagnostics) {
        AnnotationReader annot = method.getAnnotations().get(MetadataProvider.class.getName());
        ValueType generatorType = annot.getValue("value").getJavaClass();
        String generatorClassName = ((ValueType.Object) generatorType).getClassName();

        Class<?> generatorClass;
        try {
            generatorClass = Class.forName(generatorClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            diagnostics.error(callLocation, "Can't find metadata provider class {{c0}}",
                    generatorClassName);
            return null;
        }
        Constructor<?> cons;
        try {
            cons = generatorClass.getConstructor();
        } catch (NoSuchMethodException e) {
            diagnostics.error(callLocation, "Metadata generator {{c0}} does not have "
                    + "a public no-arg constructor", generatorClassName);
            return null;
        }
        MetadataGenerator generator;
        try {
            generator = (MetadataGenerator) cons.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            diagnostics.error(callLocation, "Error instantiating metadata "
                    + "generator {{c0}}", generatorClassName);
            return null;
        }
        return generator;
    }
}
