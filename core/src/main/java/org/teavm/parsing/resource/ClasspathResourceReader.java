/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.parsing.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

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
        if (classLoader.getResource(name) == null) {
            return false;
        }
        try (InputStream input = classLoader.getResourceAsStream(name)) {
            if (input == null) {
                return false;
            }
            input.read();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public InputStream openResource(String name) throws IOException {
        InputStream result = classLoader.getResourceAsStream(name);
        return result != null ? new BufferedInputStream(result) : null;
    }
}
