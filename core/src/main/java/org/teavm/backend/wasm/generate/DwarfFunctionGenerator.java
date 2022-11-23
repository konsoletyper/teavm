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
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_LOCATION;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_LOW_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_NAME;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_SPECIFICATION;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_TYPE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_ADDR;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_EXPRLOC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_REF4;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_STRP;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_OP_STACK_VALUE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_OP_WASM_LOCATION;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_FORMAL_PARAMETER;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_SUBPROGRAM;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_VARIABLE;
import org.teavm.backend.wasm.dwarf.DwarfAbbreviation;
import org.teavm.backend.wasm.dwarf.blob.Blob;
import org.teavm.backend.wasm.dwarf.blob.Marker;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.model.util.VariableType;

public class DwarfFunctionGenerator {
    private DwarfClassGenerator classGen;
    private DwarfGenerator generator;
    private WasmFunction function;
    private int offset;
    private Marker endProgramMarker;
    private DwarfAbbreviation methodAbbrev;
    private DwarfAbbreviation functionAbbrev;
    private DwarfAbbreviation parameterAbbrev;
    private DwarfAbbreviation variableAbbrev;
    private DwarfClassGenerator.Subprogram subprogram;

    public DwarfFunctionGenerator(DwarfClassGenerator classGen, DwarfGenerator generator) {
        this.classGen = classGen;
        this.generator = generator;
    }

    public void begin(WasmFunction function, int offset) {
        if (function.getName() == null) {
            return;
        }

        subprogram = classGen.getSubprogram(function.getName());
        var writer = generator.getInfoWriter();
        var strings = generator.strings;
        writer.tag(subprogram != null ? getMethodAbbrev() : getFunctionAbbrev());
        if (subprogram != null) {
            writer.ref(subprogram.ref, Blob::writeInt);
        } else {
            writer.writeInt(strings.stringRef(subprogram != null ? subprogram.name : function.getName()));
        }
        writer.writeInt(offset);
        endProgramMarker = writer.marker();
        writer.skip(4);

        this.function = function;
        this.offset = offset;

        writeLocals();
    }

    private void writeLocals() {
        if (subprogram == null) {
            return;
        }
        var descriptor = subprogram.descriptor;
        if (descriptor == null) {
            return;
        }

        var writer = generator.getInfoWriter();
        var strings = generator.strings;
        var offset = subprogram.isStatic ? 0 : 1;
        int count = Math.min(function.getLocalVariables().size() - offset, descriptor.parameterCount());
        for (var i = 0; i < count; ++i) {
            var local = function.getLocalVariables().get(i + offset);
            if (local.getName() == null) {
                continue;
            }
            writer.tag(getParameterAbbrev());
            writer.writeInt(strings.stringRef(local.getName()));
            writer.ref(classGen.getTypePtr(descriptor.parameterType(i)), Blob::writeInt);

            var operations = new Blob();
            operations.writeByte(DW_OP_WASM_LOCATION).writeByte(0).writeLEB(i + 1);
            writer.writeLEB(operations.size());
            operations.newReader(writer::write).readRemaining();
        }

        for (var i = count + offset; i < function.getLocalVariables().size(); ++i) {
            var local = function.getLocalVariables().get(i);
            if (local.getName() == null || local.getJavaType() == null) {
                continue;
            }
            writer.tag(getVariableAbbrev());
            writer.writeInt(strings.stringRef(local.getName()));
            writer.ref(classGen.getTypePtr(local.getJavaType()), Blob::writeInt);

            var operations = new Blob();
            operations.writeByte(DW_OP_WASM_LOCATION).writeByte(0).writeLEB(i + 1);
            if (local.getJavaType() == VariableType.OBJECT) {
                operations.writeByte(DW_OP_STACK_VALUE);
            }
            writer.writeLEB(operations.size());
            operations.newReader(writer::write).readRemaining();
        }
    }

    public void end(int size) {
        if (function == null) {
            return;
        }

        var writer = generator.getInfoWriter();
        if (endProgramMarker != null) {
            var backup = writer.marker();
            endProgramMarker.rewind();
            writer.writeInt(offset + size);
            backup.rewind();
        }
        writer.emptyTag();
        classGen.flushTypes();
        subprogram = null;
        endProgramMarker = null;
        function = null;
    }

    private DwarfAbbreviation getMethodAbbrev() {
        if (methodAbbrev == null) {
            methodAbbrev = generator.getInfoWriter().abbreviation(DW_TAG_SUBPROGRAM, true, data -> {
                data.writeLEB(DW_AT_SPECIFICATION).writeLEB(DW_FORM_REF4);
                data.writeLEB(DW_AT_LOW_PC).writeLEB(DW_FORM_ADDR);
                data.writeLEB(DW_AT_HIGH_PC).writeLEB(DW_FORM_ADDR);
            });
        }
        return methodAbbrev;
    }

    private DwarfAbbreviation getFunctionAbbrev() {
        if (functionAbbrev == null) {
            functionAbbrev = generator.getInfoWriter().abbreviation(DW_TAG_SUBPROGRAM, true, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_LOW_PC).writeLEB(DW_FORM_ADDR);
                data.writeLEB(DW_AT_HIGH_PC).writeLEB(DW_FORM_ADDR);
            });
        }
        return functionAbbrev;
    }

    private DwarfAbbreviation getParameterAbbrev() {
        if (parameterAbbrev == null) {
            parameterAbbrev = generator.getInfoWriter().abbreviation(DW_TAG_FORMAL_PARAMETER, false, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_TYPE).writeLEB(DW_FORM_REF4);
                data.writeLEB(DW_AT_LOCATION).writeLEB(DW_FORM_EXPRLOC);
            });
        }
        return parameterAbbrev;
    }

    private DwarfAbbreviation getVariableAbbrev() {
        if (variableAbbrev == null) {
            variableAbbrev = generator.getInfoWriter().abbreviation(DW_TAG_VARIABLE, false, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_TYPE).writeLEB(DW_FORM_REF4);
                data.writeLEB(DW_AT_LOCATION).writeLEB(DW_FORM_EXPRLOC);
            });
        }
        return variableAbbrev;
    }
}
