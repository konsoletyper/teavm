package org.teavm.model.resource;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasspathClassHolderSource implements ClassHolderSource {
    private MapperClassHolderSource innerClassSource;

    public ClasspathClassHolderSource(ClassLoader classLoader) {
        ClasspathResourceReader reader = new ClasspathResourceReader(classLoader);
        ResourceClassHolderMapper rawMapper = new ResourceClassHolderMapper(reader);
        ClasspathResourceMapper classPathMapper = new ClasspathResourceMapper(classLoader, rawMapper);
        innerClassSource = new MapperClassHolderSource(classPathMapper);
    }

    public ClasspathClassHolderSource() {
        this(ClasspathClassHolderSource.class.getClassLoader());
    }

    @Override
    public ClassHolder getClassHolder(String name) {
        return innerClassSource.getClassHolder(name);
    }
}
