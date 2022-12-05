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
