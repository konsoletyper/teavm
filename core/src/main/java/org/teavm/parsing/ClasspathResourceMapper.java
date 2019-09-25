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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import org.teavm.common.CachedFunction;
import org.teavm.model.ClassHolder;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.spi.ElementFilter;

public class ClasspathResourceMapper implements Function<String, ClassHolder>, ClassDateProvider {
    private static final String PACKAGE_PREFIX = "packagePrefix.";
    private static final String CLASS_PREFIX = "classPrefix.";
    private Function<String, ClassHolder> innerMapper;
    private List<Transformation> transformations = new ArrayList<>();
    private ClassRefsRenamer renamer;
    private ClassLoader classLoader;
    private Map<String, ModificationDate> modificationDates = new HashMap<>();
    private ReferenceCache referenceCache;
    private List<ElementFilter> elementFilters = new ArrayList<>();

    private static class Transformation {
        String packageName;
        String packagePrefix = "";
        String fullPrefix = "";
        String classPrefix = "";
    }

    public ClasspathResourceMapper(ClassLoader classLoader, ReferenceCache referenceCache,
            Function<String, ClassHolder> provider) {
        this.innerMapper = provider;
        this.referenceCache = referenceCache;
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
        renamer = new ClassRefsRenamer(referenceCache, new CachedFunction<>(classNameMapper));

        for (ElementFilter elementFilter : ServiceLoader.load(ElementFilter.class)) {
            elementFilters.add(elementFilter);
        }

        this.classLoader = classLoader;
    }

    public ClasspathResourceMapper(Properties properties, ReferenceCache referenceCache,
            Function<String, ClassHolder> innerMapper) {
        this.innerMapper = innerMapper;
        this.referenceCache = referenceCache;
        Map<String, Transformation> transformationMap = new HashMap<>();
        loadProperties(properties, transformationMap);
        transformations.addAll(transformationMap.values());
        renamer = new ClassRefsRenamer(referenceCache, new CachedFunction<>(classNameMapper));
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
    public ClassHolder apply(String name) {
        for (ElementFilter filter : elementFilters) {
            if (!filter.acceptClass(name)) {
                return null;
            }
        }

        ClassHolder cls = find(name);

        if (cls != null) {
            for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
                for (ElementFilter filter : elementFilters) {
                    if (!filter.acceptMethod(method.getReference())) {
                        cls.removeMethod(method);
                        break;
                    }
                }
            }

            for (FieldHolder field : cls.getFields().toArray(new FieldHolder[0])) {
                for (ElementFilter filter : elementFilters) {
                    if (!filter.acceptField(field.getReference())) {
                        cls.removeField(field);
                        break;
                    }
                }
            }
        }

        return cls;
    }

    private ClassHolder find(String name) {
        for (Transformation transformation : transformations) {
            if (name.startsWith(transformation.packageName)) {
                int index = name.lastIndexOf('.');
                String className = name.substring(index + 1);
                String packageName = index > 0 ? name.substring(0, index) : "";
                ClassHolder classHolder = innerMapper.apply(transformation.packagePrefix + packageName
                        + "." + transformation.classPrefix + className);
                if (classHolder != null) {
                    classHolder = renamer.rename(classHolder);
                }
                return classHolder;
            }
        }
        return innerMapper.apply(name);
    }

    private String renameClass(String name) {
        for (Transformation transformation : transformations) {
            if (name.startsWith(transformation.fullPrefix)) {
                int index = name.lastIndexOf('.');
                String className = name.substring(index + 1);
                String packageName = name.substring(0, index);
                if (className.startsWith(transformation.classPrefix)) {
                    String newName = packageName.substring(transformation.packagePrefix.length()) + "."
                            + className.substring(transformation.classPrefix.length());
                    return referenceCache.getCached(newName);
                }
            }
        }
        return name;
    }

    private Function<String, String> classNameMapper = this::renameClass;

    @Override
    public Date getModificationDate(String className) {
        ModificationDate mdate = modificationDates.get(className);
        if (mdate == null) {
            mdate = new ModificationDate();
            modificationDates.put(className, mdate);
            mdate.date = calculateModificationDate(className);
        }
        return mdate.date;
    }

    private Date calculateModificationDate(String className) {
        int dotIndex = className.lastIndexOf('.');
        String packageName;
        String simpleName;
        if (dotIndex > 0) {
            packageName = className.substring(0, dotIndex + 1);
            simpleName = className.substring(dotIndex + 1);
        } else {
            packageName = "";
            simpleName = className;
        }
        for (Transformation transformation : transformations) {
            if (packageName.startsWith(transformation.packageName)) {
                String fullName = transformation.packagePrefix + packageName + transformation.classPrefix + simpleName;
                Date date = getOriginalModificationDate(fullName);
                if (date != null) {
                    return date;
                }
            }
        }
        return getOriginalModificationDate(className);
    }

    private Date getOriginalModificationDate(String className) {
        if (classLoader == null) {
            return null;
        }
        URL url = classLoader.getResource(className.replace('.', '/') + ".class");
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file")) {
            try {
                File file = new File(url.toURI());
                return file.exists() ? new Date(file.lastModified()) : null;
            } catch (URISyntaxException e) {
                // If URI is invalid, we just report that class should be reparsed
                return null;
            }
        } else if (url.getProtocol().equals("jar") && url.getPath().startsWith("file:")) {
            int exclIndex = url.getPath().indexOf('!');
            String jarFileName = exclIndex >= 0 ? url.getPath().substring(0, exclIndex) : url.getPath();
            File file = new File(jarFileName.substring("file:".length()));
            return file.exists() ? new Date(file.lastModified()) : null;
        } else {
            return null;
        }
    }

    static class ModificationDate {
        Date date;
    }
}
