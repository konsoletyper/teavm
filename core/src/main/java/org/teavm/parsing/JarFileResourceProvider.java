/*
 *  Copyright 2012 Alexey Andreev.
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

import java.io.*;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class JarFileResourceProvider implements ResourceProvider {
    private File file;

    public JarFileResourceProvider(File file) {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        this.file = file;
    }

    public JarFileResourceProvider(String fileName) {
        this(new File(fileName));
    }

    @Override
    public boolean hasResource(String name) {
        try (JarFile jar = new JarFile(file)) {
            return jar.getEntry(name) != null;
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("The underlying file does not exist", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Error reading jar file", ex);
        }
    }

    @Override
    public InputStream openResource(String name) {
        try {
            JarInputStream input = new JarInputStream(new FileInputStream(file));
            while (true) {
                ZipEntry entry = input.getNextEntry();
                if (entry == null) {
                    input.close();
                    throw new IllegalArgumentException("Resource not found: " + name);
                }
                if (entry.getName().equals(name)) {
                    return input;
                }
                if (name.startsWith("/") && entry.getName().equals(name.substring(1))) {
                    return input;
                }
            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("The underlying file does not exist");
        } catch (IOException e) {
            throw new IllegalStateException("Error reading jar file");
        }
    }
}
