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

import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_DATA2;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_FORM_LINE_STRP;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNCT_DIRECTORY_INDEX;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNCT_PATH;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNE_END_SEQUENCE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNS_ADVANCE_LINE;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNS_ADVANCE_PC;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNS_COPY;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_LNS_SET_FILE;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.blob.Blob;
import org.teavm.backend.wasm.blob.Marker;

class DwarfLinesGenerator {
    private static final int MIN_INSN_LEN = 1;
    private static final int LINE_BASE = -3;
    private static final int LINE_RANGE = 8;
    private static final int OPCODE_BASE = 13;

    Blob blob = new Blob();
    private DwarfStrings strings;
    private SourceFileResolver sourceFileResolver;
    private Blob instructionsBlob = new Blob();
    private Marker unitLengthMarker;
    private Marker headerLengthMarker;
    private Blob filesBlob = new Blob();
    private ObjectIntMap<String> fileIndexes = new ObjectIntHashMap<>();
    private Blob dirsBlob = new Blob();
    private ObjectIntMap<String> dirIndexes = new ObjectIntHashMap<>();
    private int address;
    private int file = 1;
    private int line = 1;
    private boolean sequenceStarted;
    private Map<String, String> resolvedFileMap = new HashMap<>();

    DwarfLinesGenerator(DwarfStrings strings, SourceFileResolver sourceFileResolver) {
        this.strings = strings;
        this.sourceFileResolver = sourceFileResolver;
    }

    void begin() {
        emitLinesHeader();
    }

    private void emitLinesHeader() {
        // length
        unitLengthMarker = blob.marker();
        blob.skip(4);

        // version
        blob.writeShort(5);

        // address_size
        blob.writeByte(4);

        // segment_selector_size
        blob.writeByte(0);

        // header_length
        headerLengthMarker = blob.marker();
        blob.skip(4);

        // minimum_instruction_length
        blob.writeByte(MIN_INSN_LEN);

        // maximum_operations_per_instruction
        blob.writeByte(1);

        // default_is_stmt
        blob.writeByte(1);

        blob.writeByte(LINE_BASE).writeByte(LINE_RANGE).writeByte(OPCODE_BASE);

        // standard_opcode_lengths

        blob.writeByte(0); // DW_LNS_COPY
        blob.writeByte(1); // DW_LNS_ADVANCE_PC
        blob.writeByte(1); // DW_LNS_ADVANCE_LINE
        blob.writeByte(1); // DW_LNS_SET_FILE
        blob.writeByte(1); // DW_LNS_SET_COLUMN
        blob.writeByte(0); // DW_LNS_NEGATE_STMT
        blob.writeByte(0); // DW_LNS_SET_BASIC_BLOCK
        blob.writeByte(0); // DW_LNS_CONST_ADD_PC
        blob.writeByte(1); // DW_LNS_FIXED_ADVANCE_PC
        blob.writeByte(0); // DW_LNS_SET_PROLOGUE_END
        blob.writeByte(0); // DW_LNS_SET_EPILOGUE_BEGIN
        blob.writeByte(1); // DW_LNS_SET_ISA
    }

    void end() {
        emitDirsAndFiles();
        finishHeader();

        instructionsBlob.newReader(blob.writer()).readRemaining();

        var length = blob.ptr() - unitLengthMarker.ptr() - 4;
        unitLengthMarker.rewind();
        blob.writeInt(length);
    }

    private void emitDirsAndFiles() {
        blob.writeByte(1); // directory_entry_format_count
        blob.writeByte(DW_LNCT_PATH);
        blob.writeByte(DW_FORM_LINE_STRP);
        blob.writeLEB(dirIndexes.size());
        dirsBlob.newReader(blob.writer()).readRemaining();

        blob.writeByte(2); // file_name_entry_format_count
        blob.writeByte(DW_LNCT_DIRECTORY_INDEX);
        blob.writeByte(DW_FORM_DATA2);
        blob.writeByte(DW_LNCT_PATH);
        blob.writeByte(DW_FORM_LINE_STRP);
        blob.writeLEB(fileIndexes.size());
        filesBlob.newReader(blob.writer()).readRemaining();
    }

    private void finishHeader() {
        var marker = blob.marker();
        var headerLength = blob.ptr() - headerLengthMarker.ptr() - 4;
        headerLengthMarker.rewind();
        blob.writeInt(headerLength);
        marker.rewind();
    }

    private int fileRef(String path) {
        path = resolvePath(path);
        var ref = fileIndexes.getOrDefault(path, -1);
        if (ref < 0) {
            var nameIndex = path.lastIndexOf('/') + 1;
            var name = path.substring(nameIndex);
            var dir = path.substring(0, Math.max(0, nameIndex - 1));

            var dirPtr = dirRef(dir);
            ref = fileIndexes.size();
            fileIndexes.put(path, ref);

            filesBlob.writeShort(dirPtr);
            filesBlob.writeInt(strings.stringRef(name));
        }
        return ref;
    }

    private String resolvePath(String path) {
        if (sourceFileResolver == null) {
            return path;
        }
        return resolvedFileMap.computeIfAbsent(path, p -> {
           var result = sourceFileResolver.resolveFile(p);
           return result != null ? result : p;
        });
    }

    private int dirRef(String path) {
        var ref = dirIndexes.getOrDefault(path, -1);
        if (ref < 0) {
            ref = dirIndexes.size();
            dirIndexes.put(path, ref);
            dirsBlob.writeInt(strings.stringRef(path));
        }
        return ref;
    }

    void lineNumber(int address, String file, int line) {
        var changed = false;
        var fileRef = fileRef(file);
        if (fileRef != this.file) {
            this.file = fileRef;
            instructionsBlob.writeByte(DW_LNS_SET_FILE);
            instructionsBlob.writeLEB(fileRef);
            changed = true;
        }
        if (line != this.line || this.address != address) {
            changed = advanceTo(address, line);
        }
        if (changed) {
            instructionsBlob.writeByte(DW_LNS_COPY);
        }
        sequenceStarted = true;
    }

    void endLineNumberSequence(int address) {
        if (!sequenceStarted) {
            return;
        }
        if (this.address != address) {
            advanceTo(address, line);
        }
        instructionsBlob.writeByte(0);
        instructionsBlob.writeByte(1);
        instructionsBlob.writeByte(DW_LNE_END_SEQUENCE);
        this.line = 1;
        this.file = 1;
        this.address = 0;
        sequenceStarted = false;
    }

    private boolean advanceTo(int address, int line) {
        int lineIncrement = line - this.line;
        int addressIncrement = address - this.address;
        var result = !tryEmitSpecial(lineIncrement, addressIncrement);
        if (result) {
            if (lineIncrement != 0) {
                instructionsBlob.writeByte(DW_LNS_ADVANCE_LINE);
                instructionsBlob.writeSLEB(lineIncrement);
            }
            if (addressIncrement != 0) {
                instructionsBlob.writeByte(DW_LNS_ADVANCE_PC);
                instructionsBlob.writeLEB(addressIncrement);
            }
        }
        this.line = line;
        this.address = address;
        return result;
    }

    private boolean tryEmitSpecial(int lineIncrement, int addressIncrement) {
        if (lineIncrement < LINE_BASE || lineIncrement >= LINE_BASE + LINE_RANGE) {
            return false;
        }
        int opcode = lineIncrement - LINE_BASE + (LINE_RANGE * addressIncrement) + OPCODE_BASE;
        if (opcode <= OPCODE_BASE || opcode > 255) {
            return false;
        }
        instructionsBlob.writeByte(opcode);
        return true;
    }
}
