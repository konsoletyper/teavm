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
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_TAG_COMPILE_UNIT;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_UT_COMPILE;
import java.util.Arrays;
import java.util.Collection;
import org.teavm.backend.wasm.dwarf.DwarfInfoWriter;
import org.teavm.backend.wasm.dwarf.DwarfPlaceholder;
import org.teavm.backend.wasm.dwarf.blob.Blob;
import org.teavm.backend.wasm.model.WasmCustomSection;

public class DwarfGenerator {
    private DwarfInfoWriter infoWriter = new DwarfInfoWriter();
    private DwarfPlaceholder endOfSection;

    public void begin() {
        endOfSection = infoWriter.placeholder(4);
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
        infoWriter.tag(infoWriter.abbreviation(DW_TAG_COMPILE_UNIT, true, data -> { }));
    }

    public void end() {
        closeTag(); // compilation unit
        infoWriter.mark(endOfSection);
    }

    private void closeTag() {
        infoWriter.writeByte(0);
    }

    public Collection<? extends WasmCustomSection> createSections() {
        var abbreviations = new Blob();
        infoWriter.buildAbbreviations(abbreviations);

        var info = new Blob();
        infoWriter.build(info);

        return Arrays.asList(
                new WasmCustomSection(".debug_abbrev", abbreviations.toArray()),
                new WasmCustomSection(".debug_info", info.toArray())
        );
    }
}
