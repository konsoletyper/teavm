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
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;
import org.teavm.common.CachedFunction;
import org.teavm.model.ClassHolder;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.substitution.ClassExclusions;
import org.teavm.parsing.substitution.ClassMappings;
import org.teavm.parsing.substitution.OrderedProperties;
import org.teavm.parsing.substitution.PrefixMapping;
import org.teavm.vm.spi.ElementFilter;

public class ClasspathResourceMapper implements Function<String, ClassHolder>, ClassDateProvider {
    private static final String STRIP_PREFIX_FROM_PREFIX = "stripPrefixFrom";
    private static final String STRIP_PREFIX_FROM_PACKAGE_HIERARCHY_PREFIX =
            STRIP_PREFIX_FROM_PREFIX + "PackageHierarchyClasses";
    private static final String STRIP_PREFIX_FROM_PACKAGE_PREFIX = STRIP_PREFIX_FROM_PREFIX + "PackageClasses";
    private static final String MAP_PREFIX = "map";
    private static final String MAP_PACKAGE_HIERARCHY_PREFIX = MAP_PREFIX + "PackageHierarchy";
    private static final String MAP_PACKAGE_PREFIX = MAP_PREFIX + "Package";
    private static final String MAP_CLASS_PREFIX = MAP_PREFIX + "Class";
    private static final String INCLUDE_PREFIX = "include";
    private static final String INCLUDE_PACKAGE_HIERARCHY_PREFIX = INCLUDE_PREFIX + "PackageHierarchy";
    private static final String INCLUDE_PACKAGE_PREFIX = INCLUDE_PREFIX + "Package";
    private static final String INCLUDE_CLASS_PREFIX = INCLUDE_PREFIX + "Class";
    private static final Date VOID_DATE = new Date(0);
    private Function<String, ClassHolder> innerMapper;
    private ClassRefsRenamer renamer;
    private ClassLoader classLoader;
    private Map<String, Date> modificationDates = new HashMap<>();
    private List<ElementFilter> elementFilters = new ArrayList<>();
    private ClassMappings classMappings = new ClassMappings();
    private PrefixMapping prefixMapping = new PrefixMapping();
    private ClassMappings packageMappings = new ClassMappings();
    private ClassExclusions classExclusions = new ClassExclusions();
    private ClassMappings reverseClassMappings = new ClassMappings();
    private ClassMappings reversePackageMappings = new ClassMappings();

    public ClasspathResourceMapper(ClassLoader classLoader, ReferenceCache referenceCache,
            Function<String, ClassHolder> provider) {
        this.innerMapper = provider;
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/teavm.properties");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                Properties properties = new OrderedProperties();
                try (InputStream input = resource.openStream()) {
                    properties.load(input);
                }
                loadProperties(properties);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading resources", e);
        }
        renamer = new ClassRefsRenamer(referenceCache, new CachedFunction<>(this::toUnmappedClassName));

        for (ElementFilter elementFilter : ServiceLoader.load(ElementFilter.class)) {
            elementFilters.add(elementFilter);
        }

        this.classLoader = classLoader;
    }

    public ClasspathResourceMapper(Properties properties, ReferenceCache referenceCache,
            Function<String, ClassHolder> innerMapper) {
        this.innerMapper = innerMapper;
        loadProperties(properties);
        renamer = new ClassRefsRenamer(referenceCache, new CachedFunction<>(this::toUnmappedClassName));
    }

    @Override
    public ClassHolder apply(String name) {
        ClassHolder cls = null;
        for (String mappedClassName : classMappings.apply(name)) {
            if (classExclusions.apply(mappedClassName)) {
                continue;
            }
            ClassHolder classHolder = innerMapper.apply(mappedClassName);
            if (classHolder == null) {
                continue;
            }
            cls = renamer.rename(classHolder);
            break;
        }
        if (cls == null) {
            for (String mappedClassName : packageMappings.apply(name)) {
                mappedClassName = prefixMapping.apply(mappedClassName);
                if (classExclusions.apply(mappedClassName)) {
                    continue;
                }
                ClassHolder classHolder = innerMapper.apply(mappedClassName);
                if (classHolder == null) {
                    continue;
                }
                cls = renamer.rename(classHolder);
                break;
            }
        }
        if (cls == null) {
            if (!classExclusions.apply(name)) {
                cls = innerMapper.apply(name);
            }
        }
        if (cls != null && !elementFilters.isEmpty()) {
            for (ElementFilter filter : elementFilters) {
                if (!filter.acceptClass(name)) {
                    return null;
                }
            }
            MethodHolder[] methodHolders = cls.getMethods().toArray(new MethodHolder[0]);
            FieldHolder[] fieldHolders = cls.getFields().toArray(new FieldHolder[0]);
            for (ElementFilter filter : elementFilters) {
                for (MethodHolder method : methodHolders) {
                    if (!filter.acceptMethod(method.getReference())) {
                        cls.removeMethod(method);
                        break;
                    }
                }
                for (FieldHolder field : fieldHolders) {
                    if (!filter.acceptField(field.getReference())) {
                        cls.removeField(field);
                        break;
                    }
                }
            }
        }
        return cls;
    }

    @Override
    public Date getModificationDate(String className) {
        Date mdate = modificationDates.get(className);
        if (mdate == null) {
            mdate = getOriginalModificationDate(toUnmappedClassName(className));
            modificationDates.put(className, mdate);
        }
        return mdate == VOID_DATE ? null : mdate;
    }

    private String toUnmappedClassName(String name) {
        if (classExclusions.apply(name)) {
            return name;
        }
        name = prefixMapping.revert(name);
        for (String originalClassName : reverseClassMappings.apply(name)) {
            return originalClassName;
        }
        for (String originalClassName : reversePackageMappings.apply(name)) {
            return originalClassName;
        }
        return name;
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

    private void loadProperties(Properties properties) {
        for (String propertyName : properties.stringPropertyNames()) {
            final String[] instruction = propertyName.split("\\|", 2);
            switch (instruction[0]) {
                case STRIP_PREFIX_FROM_PACKAGE_HIERARCHY_PREFIX:
                    prefixMapping.setPackageHierarchyClassPrefixRule(instruction[1].split("\\."),
                            properties.getProperty(propertyName));
                    break;
                case STRIP_PREFIX_FROM_PACKAGE_PREFIX:
                    prefixMapping.setPackageClassPrefixRule(instruction[1].split("\\."),
                            properties.getProperty(propertyName));
                    break;
                case MAP_PACKAGE_HIERARCHY_PREFIX:
                    packageMappings.addPackageHierarchyMappingRule(properties.getProperty(propertyName).split("\\."),
                            instruction[1]);
                    reversePackageMappings.addPackageHierarchyMappingRule(instruction[1].split("\\."),
                            properties.getProperty(propertyName));
                    break;
                case MAP_PACKAGE_PREFIX:
                    packageMappings
                            .addPackageMappingRule(properties.getProperty(propertyName).split("\\."), instruction[1]);
                    reversePackageMappings
                            .addPackageMappingRule(instruction[1].split("\\."), properties.getProperty(propertyName));
                    break;
                case MAP_CLASS_PREFIX:
                    classMappings
                            .addClassMappingRule(properties.getProperty(propertyName).split("\\."), instruction[1]);
                    reverseClassMappings
                            .addClassMappingRule(instruction[1].split("\\."), properties.getProperty(propertyName));
                    break;
                case INCLUDE_PACKAGE_HIERARCHY_PREFIX:
                    classExclusions.setPackageHierarchyExclusion(instruction[1].split("\\."),
                            !Boolean.parseBoolean(properties.getProperty(propertyName)));
                    break;
                case INCLUDE_PACKAGE_PREFIX:
                    classExclusions.setPackageExclusion(instruction[1].split("\\."),
                            !Boolean.parseBoolean(properties.getProperty(propertyName)));
                    break;
                case INCLUDE_CLASS_PREFIX:
                    classExclusions.setClassExclusion(instruction[1].split("\\."),
                            !Boolean.parseBoolean(properties.getProperty(propertyName)));
                    break;
            }
        }
    }
}