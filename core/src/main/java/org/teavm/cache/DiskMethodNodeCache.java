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
package org.teavm.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.RegularMethodNode;
import org.teavm.model.MethodReference;

public class DiskMethodNodeCache implements MethodNodeCache {
    private final File directory;
    private final AstIO astIO;
    private final Map<MethodReference, Item> cache = new HashMap<>();
    private final Map<MethodReference, AsyncItem> asyncCache = new HashMap<>();
    private final Set<MethodReference> newMethods = new HashSet<>();
    private final Set<MethodReference> newAsyncMethods = new HashSet<>();

    public DiskMethodNodeCache(File directory, SymbolTable symbolTable, SymbolTable fileTable) {
        this.directory = directory;
        astIO = new AstIO(symbolTable, fileTable);
    }

    @Override
    public RegularMethodNode get(MethodReference methodReference, CacheStatus cacheStatus) {
        Item item = cache.get(methodReference);
        if (item == null) {
            item = new Item();
            cache.put(methodReference, item);
            File file = getMethodFile(methodReference, false);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    if (!checkIfDependenciesChanged(input, cacheStatus)) {
                        item.node = astIO.read(input, methodReference);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.node;
    }

    @Override
    public void store(MethodReference methodReference, RegularMethodNode node, Supplier<String[]> dependencies) {
        Item item = new Item();
        item.node = node;
        item.dependencies = dependencies.get().clone();
        cache.put(methodReference, item);
        newMethods.add(methodReference);
    }

    @Override
    public AsyncMethodNode getAsync(MethodReference methodReference, CacheStatus cacheStatus) {
        AsyncItem item = asyncCache.get(methodReference);
        if (item == null) {
            item = new AsyncItem();
            asyncCache.put(methodReference, item);
            File file = getMethodFile(methodReference, true);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    if (!checkIfDependenciesChanged(input, cacheStatus)) {
                        item.node = astIO.readAsync(input, methodReference);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.node;
    }

    private boolean checkIfDependenciesChanged(DataInput input, CacheStatus cacheStatus) throws IOException {
        int depCount = input.readShort();
        for (int i = 0; i < depCount; ++i) {
            String depClass = input.readUTF();
            if (cacheStatus.isStaleClass(depClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeAsync(MethodReference methodReference, AsyncMethodNode node, Supplier<String[]> depenencies) {
        AsyncItem item = new AsyncItem();
        item.node = node;
        item.dependencies = depenencies.get().clone();
        asyncCache.put(methodReference, item);
        newAsyncMethods.add(methodReference);
    }

    public void flush() throws IOException {
        for (MethodReference method : newMethods) {
            File file = getMethodFile(method, true);
            Item item = cache.get(method);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                output.writeShort(item.dependencies.length);
                for (String dependency : item.dependencies) {
                    output.writeUTF(dependency);
                }
                astIO.write(output, item.node);
            }
        }
        for (MethodReference method : newAsyncMethods) {
            File file = getMethodFile(method, true);
            AsyncItem item = asyncCache.get(method);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                output.writeShort(item.dependencies.length);
                for (String dependency : item.dependencies) {
                    output.writeUTF(dependency);
                }
                astIO.writeAsync(output, item.node);
            }
        }
    }

    private File getMethodFile(MethodReference method, boolean async) {
        File dir = new File(directory, method.getClassName().replace('.', '/'));
        return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-ast"
                + (async ? "-async" : ""));
    }

    private static class Item {
        RegularMethodNode node;
        String[] dependencies;
    }

    private static class AsyncItem {
        AsyncMethodNode node;
        String[] dependencies;
    }
}
