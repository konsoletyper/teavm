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
import org.teavm.backend.wasm.blob.Blob;
import org.teavm.backend.wasm.debug.info.VariableType;
import org.teavm.backend.wasm.dwarf.DwarfAbbreviation;
import org.teavm.backend.wasm.dwarf.DwarfInfoWriter;

public class DwarfFunctionGenerator {
    private DwarfClassGenerator classGen;
    private DwarfInfoWriter writer;
    private DwarfStrings strings;
    private DwarfAbbreviation methodAbbrev;
    private DwarfAbbreviation parameterAbbrev;
    private DwarfAbbreviation variableAbbrev;

    public DwarfFunctionGenerator(DwarfClassGenerator classGen, DwarfInfoWriter writer, DwarfStrings strings) {
        this.classGen = classGen;
        this.writer = writer;
        this.strings = strings;
    }

    public void prepareContent(DwarfClassGenerator.Subprogram subprogram) {
        if (subprogram.function == null || subprogram.function.getName() == null) {
            return;
        }
        var descriptor = subprogram.descriptor;
        if (descriptor == null) {
            return;
        }
        var function = subprogram.function;

        var offset = subprogram.isStatic ? 0 : 1;
        int count = Math.min(function.getLocalVariables().size() - offset, descriptor.parameterCount());
        for (var i = 0; i < count; ++i) {
            var local = function.getLocalVariables().get(i + offset);
            if (local.getName() == null) {
                continue;
            }
            classGen.getTypePtr(descriptor.parameterType(i));
        }

        for (var i = count + offset; i < function.getLocalVariables().size(); ++i) {
            var local = function.getLocalVariables().get(i);
            if (local.getName() == null || local.getJavaType() == null) {
                continue;
            }
            classGen.getTypePtr(local.getJavaType());
        }
    }

    public void writeContent(DwarfClassGenerator.Subprogram subprogram) {
        if (subprogram.function.getName() == null) {
            return;
        }

        writer.tag(getMethodAbbrev());
        writer.writeInt(strings.stringRef(subprogram.name));
        writer.writeInt(subprogram.startOffset);
        writer.writeInt(subprogram.endOffset);

        writeLocals(subprogram);

        writer.emptyTag();
    }

    private void writeLocals(DwarfClassGenerator.Subprogram subprogram) {
        var descriptor = subprogram.descriptor;
        if (descriptor == null) {
            return;
        }

        var function = subprogram.function;
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
            if (local.getJavaType() == VariableType.OBJECT) {
                operations.writeByte(DW_OP_STACK_VALUE);
            }
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

    private DwarfAbbreviation getMethodAbbrev() {
        if (methodAbbrev == null) {
            methodAbbrev = writer.abbreviation(DW_TAG_SUBPROGRAM, true, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_LOW_PC).writeLEB(DW_FORM_ADDR);
                data.writeLEB(DW_AT_HIGH_PC).writeLEB(DW_FORM_ADDR);
            });
        }
        return methodAbbrev;
    }

    private DwarfAbbreviation getParameterAbbrev() {
        if (parameterAbbrev == null) {
            parameterAbbrev = writer.abbreviation(DW_TAG_FORMAL_PARAMETER, false, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_TYPE).writeLEB(DW_FORM_REF4);
                data.writeLEB(DW_AT_LOCATION).writeLEB(DW_FORM_EXPRLOC);
            });
        }
        return parameterAbbrev;
    }

    private DwarfAbbreviation getVariableAbbrev() {
        if (variableAbbrev == null) {
            variableAbbrev = writer.abbreviation(DW_TAG_VARIABLE, false, data -> {
                data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
                data.writeLEB(DW_AT_TYPE).writeLEB(DW_FORM_REF4);
                data.writeLEB(DW_AT_LOCATION).writeLEB(DW_FORM_EXPRLOC);
            });
        }
        return variableAbbrev;
    }
}
