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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.common.CollectionUtil;

public class LineInfo {
    private LineInfoSequence[] sequences;
    private List<? extends LineInfoSequence> sequenceList;

    public LineInfo(LineInfoSequence[] sequences) {
        this.sequences = sequences.clone();
        sequenceList = Collections.unmodifiableList(Arrays.asList(this.sequences));
    }

    public List<? extends LineInfoSequence> sequences() {
        return sequenceList;
    }

    public DeobfuscatedLocation[] deobfuscate(int[] addresses) {
        var result = new ArrayList<DeobfuscatedLocation>();
        for (var address : addresses) {
            var part = deobfuscateSingle(address);
            if (part != null) {
                result.addAll(List.of(part));
            }
        }
        return result.toArray(new DeobfuscatedLocation[0]);
    }

    public DeobfuscatedLocation[] deobfuscateSingle(int address) {
        var sequence = find(address);
        if (sequence == null) {
            return null;
        }
        var instructionLoc = sequence.unpack().find(address);
        if (instructionLoc == null) {
            return returnForSequence(sequence);
        }
        var location = instructionLoc.location();
        if (location == null) {
            return returnForSequence(sequence);
        }
        var result = new DeobfuscatedLocation[location.depth()];
        var method = sequence.method();
        var i = result.length - 1;
        while (true) {
            result[i--] = new DeobfuscatedLocation(location.file(), method, location.line());
            if (i < 0) {
                break;
            }
            method = location.inlining().method();
            location = location.inlining().location();
        }
        return result;
    }

    private DeobfuscatedLocation[] returnForSequence(LineInfoSequence sequence) {
        return new DeobfuscatedLocation[] {
                new DeobfuscatedLocation(null, sequence.method(), -1)
        };
    }

    public LineInfoSequence find(int address) {
        var index = CollectionUtil.binarySearch(sequenceList, address, LineInfoSequence::endAddress);
        if (index < 0) {
            index = -index - 1;
        }
        if (index >= sequenceList.size()) {
            return null;
        }
        var sequence = sequenceList.get(index);
        return address >= sequence.startAddress() ? sequence : null;
    }

    public void dump(PrintStream out) {
        for (var i = 0; i < sequences.length; ++i) {
            var sequence = sequences[i];
            out.println("Sequence " + i + ": " + sequence.method().fullName() + " ["
                    + Integer.toHexString(sequence.startAddress()) + ".."
                    + Integer.toHexString(sequence.endAddress()) + ")");
            for (var location : sequence.unpack().locations()) {
                out.print("  at " + Integer.toHexString(location.address()) + " ");
                if (location.location() != null) {
                    out.println(location.location().file().fullName() + ":" + location.location().line());
                } else {
                    out.println("<unmapped>");
                }
            }
        }
    }
}
