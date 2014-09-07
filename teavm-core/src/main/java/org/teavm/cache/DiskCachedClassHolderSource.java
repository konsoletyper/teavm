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

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DiskCachedClassHolderSource implements ClassHolderSource {
    private File directory;
    private ClassHolderSource innerSource;
    private Map<String, Item> cache = new HashMap<>();
    private Set<String> newClasses = new HashSet<>();

    public DiskCachedClassHolderSource(File directory, ClassHolderSource innerSource) {
        this.directory = directory;
        this.innerSource = innerSource;
    }

    @Override
    public ClassHolder get(String name) {
        Item item = cache.get(name);
        if (item == null) {
            item = new Item();
            cache.put(name, item);
            File classDir = new File(directory, name);
        }
        return item.cls;
    }

    private static class Item {
        ClassHolder cls;
    }

    public void flush() throws IOException {

    }

    private void writeClass(OutputStream stream, ClassHolder cls) throws IOException {
        DataOutput output = new DataOutputStream(stream);
        output.writeByte(cls.getLevel().ordinal());
        if (cls.getParent() != null) {
            output.writeByte(1);
            output.writeUTF(cls.getParent());
        } else {
            output.writeByte(0);
        }
        if (cls.getOwnerName() != null) {
            output.writeByte(1);
            output.writeUTF(cls.getOwnerName());
        } else {
            output.writeByte(0);
        }
    }
}
