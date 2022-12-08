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
package org.teavm.backend.wasm.dwarf;

import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_CHILDREN_NO;
import static org.teavm.backend.wasm.dwarf.DwarfConstants.DW_CHILDREN_YES;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.teavm.backend.wasm.blob.BinaryDataConsumer;
import org.teavm.backend.wasm.blob.Blob;
import org.teavm.backend.wasm.blob.Marker;

public class DwarfInfoWriter {
    private Blob output = new Blob();
    private List<DwarfAbbreviation> abbreviations = new ArrayList<>();
    private List<Placement> placements = new ArrayList<>();
    private Marker placeholderMarker;

    public DwarfInfoWriter write(byte[] data) {
        output.write(data);
        return this;
    }

    public DwarfInfoWriter write(byte[] data, int offset, int limit) {
        output.write(data, offset, limit);
        return this;
    }

    public DwarfInfoWriter writeInt(int value) {
        output.writeInt(value);
        return this;
    }

    public DwarfInfoWriter writeShort(int value) {
        output.writeShort(value);
        return this;
    }

    public DwarfInfoWriter writeByte(int value) {
        output.write((byte) value);
        return this;
    }

    public DwarfInfoWriter writeLEB(int value) {
        output.writeLEB(value);
        return this;
    }

    public DwarfInfoWriter skip(int count) {
        output.skip(count);
        return this;
    }

    public Marker marker() {
        return output.marker();
    }

    public DwarfAbbreviation abbreviation(int tag, boolean hasChildren, Consumer<Blob> blob) {
        var abbr = new DwarfAbbreviation(tag, hasChildren, blob);
        abbreviations.add(abbr);
        return abbr;
    }

    public DwarfInfoWriter tag(DwarfAbbreviation abbreviation) {
        placements.add(new Placement(output.ptr()) {
            @Override
            void write(Blob blob) {
                blob.writeLEB(abbreviation.index);
            }
        });
        abbreviation.count++;
        return this;
    }

    public DwarfInfoWriter emptyTag() {
        output.write((byte) 0);
        return this;
    }

    public DwarfPlaceholder placeholder(int size) {
        return new DwarfPlaceholder(size);
    }

    public DwarfInfoWriter ref(DwarfPlaceholder placeholder, DwarfPlaceholderWriter writer) {
        placements.add(new Placement(output.ptr()) {
            @Override
            void write(Blob blob) {
                if (placeholder.ptr >= 0) {
                    placeholderMarker.update();
                    writer.write(blob, placeholder.ptr);
                    placeholderMarker.rewind();
                    blob.skip(placeholder.size);
                } else {
                    placeholder.addForwardRef(writer, blob.marker());
                    blob.skip(placeholder.size);
                }
            }
        });
        return this;
    }

    public DwarfInfoWriter mark(DwarfPlaceholder placeholder) {
        Objects.requireNonNull(placeholder);
        placements.add(new Placement(output.ptr()) {
            @Override
            void write(Blob blob) {
                if (placeholder.ptr >= 0) {
                    throw new IllegalStateException();
                }
                placeholder.ptr = blob.ptr();
                if (placeholder.forwardReferences != null) {
                    placeholderMarker.update();
                    for (var forwardRef : placeholder.forwardReferences) {
                        forwardRef.marker.rewind();
                        forwardRef.writer.write(blob, placeholder.ptr);
                    }
                    placeholder.forwardReferences = null;
                    placeholderMarker.rewind();
                }
            }
        });
        return this;
    }

    public void buildAbbreviations(Blob target) {
        var orderedAbbreviations = new ArrayList<>(abbreviations);
        orderedAbbreviations.sort(Comparator.comparingInt(a -> -a.count));
        var sz = orderedAbbreviations.size();
        while (sz > 0 && orderedAbbreviations.get(sz - 1).count == 0) {
            --sz;
        }
        for (var i = 0; i < sz; ++i) {
            var abbrev = orderedAbbreviations.get(i);
            abbrev.index = i + 1;
            target.writeLEB(abbrev.index).writeLEB(abbrev.tag)
                    .writeByte(abbrev.hasChildren ? DW_CHILDREN_YES : DW_CHILDREN_NO);
            abbrev.writer.accept(target);
            target.writeByte(0).writeByte(0);
        }
        target.writeByte(0);
    }

    public void build(Blob target) {
        placeholderMarker = target.marker();
        this.targetBlob = target;
        var reader = output.newReader(targetBlobWritingConsumer);
        for (var placement : placements) {
            reader.advance(placement.offset);
            placement.write(target);
        }
        reader.advance(output.size());
        this.targetBlob = null;
        placeholderMarker = null;
    }

    private Blob targetBlob;
    private BinaryDataConsumer targetBlobWritingConsumer = (data, offset, limit) ->
            targetBlob.write(data, offset, limit);

    private static abstract class Placement {
        int offset;

        Placement(int offset) {
            this.offset = offset;
        }

        abstract void write(Blob blob);
    }
}
