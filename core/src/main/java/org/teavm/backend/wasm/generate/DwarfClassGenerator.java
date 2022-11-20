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
package org.teavm.backend.wasm.generate;

import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_HIGH_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_LOW_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_NAME;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_ADDR;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_STRP;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_CLASS_TYPE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_NAMESPACE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_SUBPROGRAM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.dwarf.DwarfAbbreviation;
import org.teavm.backend.wasm.dwarf.DwarfInfoWriter;
import org.teavm.model.MethodDescriptor;

public class DwarfClassGenerator {
    final Namespace root = new Namespace(null);
    final Map<String, Subprogram> subprogramsByFunctionName = new HashMap<>();
    final List<Subprogram> rootSubprograms = new ArrayList<>();
    private DwarfInfoWriter infoWriter;
    private DwarfStrings strings;
    private DwarfAbbreviation nsAbbrev;
    private DwarfAbbreviation classTypeAbbrev;
    private DwarfAbbreviation methodAbbrev;

    public ClassType getClass(String fullName) {
        var index = 0;
        var ns = root;
        while (true) {
            var next = fullName.indexOf('.', index);
            if (next < 0) {
                break;
            }
            ns = ns.getNamespace(fullName.substring(index, next));
            index = next + 1;
        }
        return ns.getClass(fullName.substring(index));
    }

    public void registerSubprogram(String functionName, Subprogram subprogram) {
        subprogramsByFunctionName.put(functionName, subprogram);
    }

    public Subprogram getSubprogram(String functionName) {
        return subprogramsByFunctionName.get(functionName);
    }

    public Subprogram createSubprogram(String functionName) {
        var subprogram = new Subprogram(functionName);
        subprogramsByFunctionName.put(functionName, subprogram);
        rootSubprograms.add(subprogram);
        return subprogram;
    }

    public void write(DwarfInfoWriter infoWriter, DwarfStrings strings) {
        this.infoWriter = infoWriter;
        this.strings = strings;
        root.writeChildren();
        for (var subprogram : rootSubprograms) {
            subprogram.write();
        }
        this.infoWriter = null;
        this.strings = null;
        methodAbbrev = null;
    }

    private DwarfAbbreviation getMethodAbbrev() {
        if (methodAbbrev == null) {
            methodAbbrev = infoWriter.abbreviation(DW_TAG_SUBPROGRAM, true, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_LOW_PC).writeLEB(DW_FORM_ADDR);
                data.writeLEB(DW_AT_HIGH_PC).writeLEB(DW_FORM_ADDR);
            });
        }
        return methodAbbrev;
    }

    private DwarfAbbreviation getNsAbbrev() {
        if (nsAbbrev == null) {
            nsAbbrev = infoWriter.abbreviation(DW_TAG_NAMESPACE, true, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
            });
        }
        return nsAbbrev;
    }

    private DwarfAbbreviation getClassTypeAbbrev() {
        if (classTypeAbbrev == null) {
            classTypeAbbrev = infoWriter.abbreviation(DW_TAG_CLASS_TYPE, true, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
            });
        }
        return classTypeAbbrev;
    }

    public class Namespace {
        public final String name;
        final Map<String, Namespace> namespaces = new LinkedHashMap<>();
        final Map<String, ClassType> classes = new LinkedHashMap<>();

        private Namespace(String name) {
            this.name = name;
        }

        private Namespace getNamespace(String name) {
            return namespaces.computeIfAbsent(name, Namespace::new);
        }

        private void write() {
            infoWriter.tag(getNsAbbrev());
            infoWriter.writeInt(strings.stringRef(name));
            writeChildren();
            infoWriter.emptyTag();
        }

        private void writeChildren() {
            for (var child : namespaces.values()) {
                child.write();
            }
            for (var child : classes.values()) {
                child.write();
            }
        }

        ClassType getClass(String name) {
            return classes.computeIfAbsent(name, ClassType::new);
        }
    }

    public class ClassType {
        public final String name;
        final Map<MethodDescriptor, Subprogram> subprograms = new LinkedHashMap<>();

        private ClassType(String name) {
            this.name = name;
        }

        public Subprogram getSubprogram(MethodDescriptor desc) {
            return subprograms.computeIfAbsent(desc, d -> new Subprogram(d.getName()));
        }

        private void write() {
            infoWriter.tag(getClassTypeAbbrev());
            infoWriter.writeInt(strings.stringRef(name));
            for (var child : subprograms.values()) {
                child.write();
            }
            infoWriter.emptyTag();
        }
    }

    public class Subprogram {
        public final String name;
        public int startAddress;
        public int endAddress;

        private Subprogram(String name) {
            this.name = name;
        }

        private void write() {
            infoWriter.tag(getMethodAbbrev());
            infoWriter.writeInt(strings.stringRef(name));
            infoWriter.writeInt(startAddress);
            infoWriter.writeInt(endAddress);
            infoWriter.emptyTag();
        }
    }
}
