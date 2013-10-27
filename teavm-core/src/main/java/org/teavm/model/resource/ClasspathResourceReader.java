package org.teavm.model.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasspathResourceReader implements ResourceReader {
    private ClassLoader classLoader;

    public ClasspathResourceReader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClasspathResourceReader() {
        this(ClasspathResourceReader.class.getClassLoader());
    }

    @Override
    public boolean hasResource(String name) {
        return classLoader.getResource(name) != null;
    }

    @Override
    public InputStream openResource(String name) throws IOException {
        return classLoader.getResourceAsStream(name);
    }
}
