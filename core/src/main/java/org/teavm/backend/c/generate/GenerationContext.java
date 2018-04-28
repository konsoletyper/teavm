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
package org.teavm.backend.c.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.lowlevel.Characteristics;

public class GenerationContext {
    private VirtualTableProvider virtualTableProvider;
    private Characteristics characteristics;
    private StringPool stringPool;
    private NameProvider names;
    private Diagnostics diagnostics;
    private ClassReaderSource classSource;
    private List<Intrinsic> intrinsics;
    private List<Generator> generators;
    private Map<MethodReference, Intrinsic> intrinsicCache = new HashMap<>();

    public GenerationContext(VirtualTableProvider virtualTableProvider, Characteristics characteristics,
            StringPool stringPool, NameProvider names, Diagnostics diagnostics, ClassReaderSource classSource,
            List<Intrinsic> intrinsics, List<Generator> generators) {
        this.virtualTableProvider = virtualTableProvider;
        this.characteristics = characteristics;
        this.stringPool = stringPool;
        this.names = names;
        this.diagnostics = diagnostics;
        this.classSource = classSource;
        this.intrinsics = new ArrayList<>(intrinsics);
        this.generators = new ArrayList<>(generators);
    }

    public void addIntrinsic(Intrinsic intrinsic) {
        intrinsics.add(intrinsic);
    }

    public VirtualTableProvider getVirtualTableProvider() {
        return virtualTableProvider;
    }

    public Characteristics getCharacteristics() {
        return characteristics;
    }

    public StringPool getStringPool() {
        return stringPool;
    }

    public NameProvider getNames() {
        return names;
    }

    public Diagnostics getDiagnostics() {
        return diagnostics;
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public Intrinsic getIntrinsic(MethodReference method) {
        return intrinsicCache.computeIfAbsent(method,
                m -> intrinsics.stream().filter(i -> i.canHandle(m)).findFirst().orElse(null));
    }

    public Generator getGenerator(MethodReference method) {
        for (Generator generator : generators) {
            if (generator.canHandle(method)) {
                return generator;
            }
        }
        return null;
    }
}
