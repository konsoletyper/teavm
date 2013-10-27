package org.teavm.model.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.teavm.codegen.Mapper;
import org.teavm.model.ClassHolder;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasspathResourceMapper implements Mapper<String, ClassHolder> {
    private static String PACKAGE_PREFIX = "packagePrefix.";
    private static String CLASS_PREFIX = "classPrefix.";
    private Mapper<String, ClassHolder> innerMapper;
    private List<Transformation> transformations = new ArrayList<>();

    private static class Transformation {
        String packageName;
        String packagePrefix = "";
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
    }

    private void loadProperties(Properties properties, Map<String, Transformation> cache) {
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.startsWith(PACKAGE_PREFIX)) {
                String packageName = propertyName.substring(PACKAGE_PREFIX.length());
                Transformation transformation = getTransformation(cache, packageName);
                transformation.packagePrefix = properties.getProperty(propertyName) + ".";
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
            transformation.packageName = packageName;
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
                String packageName = name.substring(0, index);
                ClassHolder classHolder = innerMapper.map(transformation.packagePrefix + "." + packageName +
                        "." + transformation.classPrefix + className);
                return classHolder;
            }
        }
        return innerMapper.map(name);
    }
}
