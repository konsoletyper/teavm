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
package org.teavm.idea.jps;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.teavm.asm.ClassReader;
import org.teavm.asm.ClassWriter;

public class RenamingClassLoader extends URLClassLoader {
    private List<Rename> renameList = new ArrayList<>();

    public RenamingClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void rename(String prefix, String to) {
        renameList.add(new Rename(prefix, to));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
            // continue
        }

        try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
            ClassReader classReader = new ClassReader(new BufferedInputStream(input));
            ClassWriter writer = new ClassWriter(0);
            RenamingVisitor visitor = new RenamingVisitor(writer);
            for (Rename rename : renameList) {
                visitor.rename(rename.prefix, rename.to);
            }
            classReader.accept(visitor, 0);
            byte[] data = writer.toByteArray();
            return defineClass(name, data, 0, data.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    static class Rename {
        final String prefix;
        final String to;

        Rename(String prefix, String to) {
            this.prefix = prefix;
            this.to = to;
        }
    }
}
