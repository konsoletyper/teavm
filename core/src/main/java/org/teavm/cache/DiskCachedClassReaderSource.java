/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClassDateProvider;

public class DiskCachedClassReaderSource implements ClassReaderSource, CacheStatus {
    private File directory;
    private ClassHolderSource innerSource;
    private ClassDateProvider classDateProvider;
    private Map<String, Item> cache = new LinkedHashMap<>();
    private Set<String> newClasses = new HashSet<>();
    private ClassIO classIO;

    public DiskCachedClassReaderSource(File directory, ReferenceCache referenceCache, SymbolTable symbolTable,
            SymbolTable fileTable, SymbolTable variableTable, ClassHolderSource innerSource,
            ClassDateProvider classDateProvider) {
        this.directory = directory;
        this.innerSource = innerSource;
        this.classDateProvider = classDateProvider;
        classIO = new ClassIO(referenceCache, symbolTable, fileTable, variableTable);
    }

    @Override
    public ClassReader get(String name) {
        return getItemFromCache(name).cls;
    }

    @Override
    public boolean isStaleClass(String className) {
        return getItemFromCache(className).dirty;
    }

    @Override
    public boolean isStaleMethod(MethodReference method) {
        return isStaleClass(method.getClassName());
    }

    private Item getItemFromCache(String name) {
        Item item = cache.get(name);
        if (item == null) {
            item = new Item();
            cache.put(name, item);
            File classFile = new File(directory, name.replace('.', '/') + ".teavm-cls");
            if (classFile.exists()) {
                Date classDate = classDateProvider.getModificationDate(name);
                if (classDate != null && classDate.before(new Date(classFile.lastModified()))) {
                    try (InputStream input = new BufferedInputStream(new FileInputStream(classFile))) {
                        item.cls = classIO.readClass(input, name);
                    } catch (IOException e) {
                        // We could not access cache file, so let's parse class file
                        item.cls = null;
                    }
                }
            }
            if (item.cls == null) {
                item.dirty = true;
                item.cls = innerSource.get(name);
                newClasses.add(name);
            }
        }
        return item;
    }

    private static class Item {
        ClassReader cls;
        boolean dirty;
    }

    public void flush() throws IOException {
        for (String className : newClasses) {
            Item item = cache.get(className);
            if (item.cls != null) {
                File classFile = new File(directory, className.replace('.', '/') + ".teavm-cls");
                classFile.getParentFile().mkdirs();
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(classFile))) {
                    classIO.writeClass(output, item.cls);
                }
            }
        }
    }
}
