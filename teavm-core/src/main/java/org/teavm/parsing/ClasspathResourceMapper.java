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
package org.teavm.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.teavm.common.CachedMapper;
import org.teavm.common.Mapper;
import org.teavm.model.ClassHolder;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasspathResourceMapper implements Mapper<String, ClassHolder> {
    private static final String PACKAGE_PREFIX = "packagePrefix.";
    private static final String CLASS_PREFIX = "classPrefix.";
    private Mapper<String, ClassHolder> innerMapper;
    private List<Transformation> transformations = new ArrayList<>();
    private ClassRefsRenamer renamer;

    private static class Transformation {
        String packageName;
        String packagePrefix = "";
        String fullPrefix = "";
        String classPrefix = "";
    }

    public ClasspathResourceMapper(ClassLoader classLoader, Mapper<String, ClassHolder> innerMapper) {
        this.innerMapper = innerMapper;
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/teavm.properties");
            Map<String, Transformation> transformationMap = new HashMap<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = new Properties();
                try (InputStream input = resource.openStream()) {
                    properties.load(input);
                }
                loadProperties(properties, transformationMap);
            }
            transformations.addAll(transformationMap.values());
        } catch (IOException e) {
            throw new RuntimeException("Error reading resources", e);
        }
        renamer = new ClassRefsRenamer(new CachedMapper<>(classNameMapper));
    }

    private void loadProperties(Properties properties, Map<String, Transformation> cache) {
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(PACKAGE_PREFIX)) {
                String packageName = propertyName.substring(PACKAGE_PREFIX.length());
                Transformation transformation = getTransformation(cache, packageName);
                transformation.packagePrefix = properties.getProperty(propertyName) + ".";
                transformation.fullPrefix = transformation.packagePrefix + transformation.packageName;
            } else if (propertyName.startsWith(CLASS_PREFIX)) {
                String packageName = propertyName.substring(CLASS_PREFIX.length());
                Transformation transformation = getTransformation(cache, packageName);
                transformation.classPrefix = properties.getProperty(propertyName);
            }
        }
    }

    private Transformation getTransformation(Map<String, Transformation> cache, String packageName) {
        Transformation transformation = cache.get(packageName);
        if (transformation == null) {
            transformation = new Transformation();
            transformation.packageName = packageName + ".";
            transformation.fullPrefix = packageName + ".";
            cache.put(packageName, transformation);
        }
        return transformation;
    }

    @Override
    public ClassHolder map(String name) {
        for (Transformation transformation : transformations) {
            if (name.startsWith(transformation.packageName)) {
                int index = name.lastIndexOf('.');
                String className = name.substring(index + 1);
                String packageName = index > 0 ? name.substring(0, index) : "";
                ClassHolder classHolder = innerMapper.map(transformation.packagePrefix + packageName +
                        "." + transformation.classPrefix + className);
                if (classHolder != null) {
                    classHolder = renamer.rename(classHolder);
                }
                return classHolder;
            }
        }
        return innerMapper.map(name);
    }

    private String renameClass(String name) {
        for (Transformation transformation : transformations) {
            if (name.startsWith(transformation.fullPrefix)) {
                int index = name.lastIndexOf('.');
                String className = name.substring(index + 1);
                String packageName = name.substring(0, index);
                if (className.startsWith(transformation.classPrefix)) {
                    return packageName.substring(transformation.packagePrefix.length()) + "." +
                            className.substring(transformation.classPrefix.length());
                }
            }
        }
        return name;
    }

    private Mapper<String, String> classNameMapper = new Mapper<String, String>() {
        @Override
        public String map(String preimage) {
            return renameClass(preimage);
        }
    };
}
