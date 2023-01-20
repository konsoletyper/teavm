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
    private IntObjectMap<TypeLayout> layoutByAddress;

    public abstract List<? extends TypeLayout> types();

    public TypeLayout find(int address) {
        if (layoutByAddress == null) {
            layoutByAddress = new IntObjectHashMap<>();
            for (var typeLayout : types()) {
                layoutByAddress.put(typeLayout.address(), typeLayout);
            }
        }
        return layoutByAddress.get(address);
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
            out.println("  address: " + Integer.toHexString(type.address()));
            switch (type.kind()) {
                case CLASS:
                    dumpClass(out, indexes, (ClassLayout) type);
                    break;
                case INTERFACE:
                    dumpInterface(out, (InterfaceLayout) type);
                    break;
                case ARRAY:
                    dumpArray(out, indexes, (ArrayLayout) type);
                    break;
                case PRIMITIVE:
                    dumpPrimitive(out, (PrimitiveLayout) type);
                    break;
                default:
                    break;
            }
        }
    }

    private static void dumpClass(PrintStream out, ObjectIntMap<TypeLayout> indexes, ClassLayout cls) {
        out.println("  name: " + cls.classRef().fullName());
        out.println("  size: " + cls.size());
        if (cls.superclass() != null) {
            out.println("  superclass: #" + indexes.get(cls.superclass()));
        }
        if (!cls.staticFields().isEmpty()) {
            out.println("  static fields:");
            dumpFields(out, cls.staticFields());
        }
        if (!cls.instanceFields().isEmpty()) {
            out.println("  instance fields:");
            dumpFields(out, cls.instanceFields());
        }
    }

    private static void dumpFields(PrintStream out, Collection<? extends FieldInfo> fields) {
        for (var field : fields) {
            out.println("    " + field.name() + ": ");
            out.println("      offset: " + Integer.toHexString(field.address()));
            out.println("      type: " + field.type().name().toLowerCase());
        }
    }

    private static void dumpInterface(PrintStream out, InterfaceLayout cls) {
        out.println("  name: " + cls.classRef().fullName());
    }

    private static void dumpArray(PrintStream out, ObjectIntMap<TypeLayout> indexes, ArrayLayout array) {
        out.println("  element: #" + indexes.get(array.elementType()));
    }

    private static void dumpPrimitive(PrintStream out, PrimitiveLayout primitive) {
        out.println("  primitive type: "  + primitive.primitiveType().name().toLowerCase());
    }
}
