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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.model.ReferenceCache;

public class DiskProgramCache implements ProgramCache {
    private File directory;
    private ProgramIO programIO;
    private Map<MethodReference, Item> cache = new HashMap<>();
    private Set<MethodReference> newMethods = new HashSet<>();

    public DiskProgramCache(File directory, ReferenceCache referenceCache, SymbolTable symbolTable,
            SymbolTable fileTable, SymbolTable variableTable) {
        this.directory = directory;
        programIO = new ProgramIO(referenceCache, symbolTable, fileTable, variableTable);
    }

    @Override
    public Program get(MethodReference method, CacheStatus cacheStatus) {
        Item item = cache.get(method);
        if (item == null) {
            item = new Item();
            cache.put(method, item);
            File file = getMethodFile(method);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    int depCount = input.readShort();
                    boolean dependenciesChanged = false;
                    for (int i = 0; i < depCount; ++i) {
                        String depClass = input.readUTF();
                        if (cacheStatus.isStaleClass(depClass)) {
                            dependenciesChanged = true;
                            break;
                        }
                    }
                    if (!dependenciesChanged) {
                        item.program = programIO.read(stream);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.program;
    }

    @Override
    public void store(MethodReference method, Program program, Supplier<String[]> dependencies) {
        Item item = new Item();
        cache.put(method, item);
        item.program = program;
        item.dependencies = dependencies.get().clone();
        newMethods.add(method);
    }

    public void flush() throws IOException {
        for (MethodReference method : newMethods) {
            Item item = cache.get(method);
            File file = getMethodFile(method);
            file.getParentFile().mkdirs();

            try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                DataOutput output = new DataOutputStream(stream);

                output.writeShort(item.dependencies.length);
                for (String dep : item.dependencies) {
                    output.writeUTF(dep);
                }
                programIO.write(item.program, stream);
            }
        }
    }

    private File getMethodFile(MethodReference method) {
        File dir = new File(directory, method.getClassName().replace('.', '/'));
        return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-opt");
    }

    static class Item {
        Program program;
        String[] dependencies;
    }
}
