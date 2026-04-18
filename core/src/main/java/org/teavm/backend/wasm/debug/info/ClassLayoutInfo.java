/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug.info;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

public abstract class ClassLayoutInfo {
    private IntObjectMap<TypeLayout> layoutByGlobalIndex;

    public abstract List<? extends TypeLayout> types();

    public TypeLayout find(int globalIndex) {
        if (layoutByGlobalIndex == null) {
            layoutByGlobalIndex = new IntObjectHashMap<>();
            for (var typeLayout : types()) {
                layoutByGlobalIndex.put(typeLayout.globalIndex(), typeLayout);
            }
        }
        return layoutByGlobalIndex.get(globalIndex);
    }

    public void dump(PrintStream out) {
        var indexes = new ObjectIntHashMap<TypeLayout>();
        for (var i = 0; i < types().size(); ++i) {
            indexes.put(types().get(i), i);
        }
        for (var i = 0; i < types().size(); ++i) {
            out.print("#" + i + ": ");
            var type = types().get(i);
            out.println(type.kind().name().toLowerCase());
            out.println("  address: " + Integer.toHexString(type.globalIndex()));
            switch (type.kind()) {
                case CLASS:
                    dumpClass(out, indexes, (ClassLayout) type);
                    break;
                case ARRAY:
                    dumpArray(out, (ArrayLayout) type);
                    break;
                default:
                    break;
            }
        }
    }

    private static void dumpClass(PrintStream out, ObjectIntMap<TypeLayout> indexes, ClassLayout cls) {
        out.println("  name: " + cls.classRef().fullName());
        if (cls.superclass() != null) {
            out.println("  superclass: #" + indexes.get(cls.superclass()));
        }
        if (!cls.instanceFields().isEmpty()) {
            out.println("  fields:");
            dumpFields(out, cls.instanceFields());
        }
    }

    private static void dumpFields(PrintStream out, Collection<? extends FieldInfo> fields) {
        for (var field : fields) {
            out.println("    " + field.name() + ": ");
            out.println("      index: " + Integer.toHexString(field.index()));
            out.println("      type: " + field.type().name().toLowerCase());
        }
    }

    private static void dumpArray(PrintStream out, ArrayLayout array) {
        out.println("  element: #" + array.elementType().name().toLowerCase());
    }
}
