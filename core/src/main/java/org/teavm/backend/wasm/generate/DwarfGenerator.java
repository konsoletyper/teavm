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

import static org.teavm.backend.wasm.dwarf.DwarfConstants.DWARF_VERSION;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_HIGH_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_LANGUAGE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_LOW_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_NAME;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_PRODUCER;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_AT_STMT_LIST;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_ADDR;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_DATA2;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_SEC_OFFSET;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_STRP;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LANG_C_PLUS_PLUS;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_COMPILE_UNIT;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_UT_COMPILE;
import java.util.ArrayList;
import java.util.Collection;
import org.teavm.backend.wasm.blob.Blob;
import org.teavm.backend.wasm.blob.Marker;
import org.teavm.backend.wasm.dwarf.DwarfInfoWriter;
import org.teavm.backend.wasm.dwarf.DwarfPlaceholder;
import org.teavm.backend.wasm.model.WasmCustomSection;

public class DwarfGenerator {
    private DwarfInfoWriter infoWriter = new DwarfInfoWriter();
    private DwarfPlaceholder endOfSection;
    public final DwarfStrings strings = new DwarfStrings();
    private DwarfStrings lineStrings = new DwarfStrings();
    private DwarfLinesGenerator lines;
    private Marker highPcMarker;

    public DwarfGenerator(SourceFileResolver sourceFileResolver) {
         lines = new DwarfLinesGenerator(lineStrings, sourceFileResolver);
    }

    public void begin() {
        endOfSection = infoWriter.placeholder(4);
        lines.begin();
        emitUnitHeader();
        compilationUnit();
    }

    private void emitUnitHeader() {
        // unit_length
        infoWriter.ref(endOfSection, (blob, ptr) -> {
            int size = ptr - blob.ptr() - 4;
            blob.writeInt(size);
        });

        // version
        infoWriter.writeShort(DWARF_VERSION);

        // unit_type
        infoWriter.writeByte(DW_UT_COMPILE);

        // address_size
        infoWriter.writeByte(4);

        // debug_abbrev_offset
        infoWriter.writeInt(0);
    }

    private void compilationUnit() {
        infoWriter.tag(infoWriter.abbreviation(DW_TAG_COMPILE_UNIT, true, data -> {
            data.writeLEB(DW_AT_PRODUCER).writeLEB(DW_FORM_STRP);
            data.writeLEB(DW_AT_LANGUAGE).writeLEB(DW_FORM_DATA2);
            data.writeLEB(DW_AT_NAME).writeLEB(DW_FORM_STRP);
            data.writeLEB(DW_AT_STMT_LIST).writeLEB(DW_FORM_SEC_OFFSET);
            data.writeLEB(DW_AT_LOW_PC).writeLEB(DW_FORM_ADDR);
            data.writeLEB(DW_AT_HIGH_PC).writeLEB(DW_FORM_ADDR);
        }));
        infoWriter.writeInt(strings.stringRef("TeaVM"));
        infoWriter.writeShort(DW_LANG_C_PLUS_PLUS);
        infoWriter.writeInt(strings.stringRef("classes.wasm"));
        infoWriter.writeInt(0);
        infoWriter.writeInt(0);
        highPcMarker = infoWriter.marker();
        infoWriter.skip(4);
    }

    public void end() {
        closeTag(); // compilation unit
        infoWriter.mark(endOfSection);
        lines.end();
    }

    private void closeTag() {
        infoWriter.writeByte(0);
    }

    public void setCodeSize(int codeSize) {
        var backup = infoWriter.marker();
        highPcMarker.rewind();
        infoWriter.writeInt(codeSize);
        backup.rewind();
    }

    public Collection<? extends WasmCustomSection> createSections() {
        var sections = new ArrayList<WasmCustomSection>();

        var abbreviations = new Blob();
        infoWriter.buildAbbreviations(abbreviations);
        sections.add(new WasmCustomSection(".debug_abbrev", abbreviations.toArray()));

        var info = new Blob();
        infoWriter.build(info);
        sections.add(new WasmCustomSection(".debug_info", info.toArray()));

        if (strings.blob.size() > 0) {
            sections.add(new WasmCustomSection(".debug_str", strings.blob.toArray()));
        }
        if (lineStrings.blob.size() > 0) {
            sections.add(new WasmCustomSection(".debug_line_str", lineStrings.blob.toArray()));
        }

        sections.add(new WasmCustomSection(".debug_line", lines.blob.toArray()));

        return sections;
    }

    public void lineNumber(int address, String fileName, int lineNumber) {
        lines.lineNumber(address, fileName, lineNumber);
    }

    public void endLineNumberSequence(int address) {
        lines.endLineNumberSequence(address);
    }

    public DwarfInfoWriter getInfoWriter() {
        return infoWriter;
    }
}
