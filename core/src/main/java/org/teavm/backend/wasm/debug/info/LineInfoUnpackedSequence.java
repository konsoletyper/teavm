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

import java.util.List;
import org.teavm.common.CollectionUtil;

public class LineInfoUnpackedSequence {
    private int startAddress;
    private int endAddress;
    private MethodInfo method;
    private List<? extends InstructionLocation> locations;

    LineInfoUnpackedSequence(int startAddress, int endAddress, MethodInfo method,
            List<? extends InstructionLocation> locations) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.method = method;
        this.locations = locations;
    }

    public int startAddress() {
        return startAddress;
    }

    public int endAddress() {
        return endAddress;
    }

    public MethodInfo method() {
        return method;
    }

    public List<? extends InstructionLocation> locations() {
        return locations;
    }

    public InstructionLocation find(int address) {
        var index = findIndex(address);
        return index >= 0 ? locations.get(index) : null;
    }

    public int findIndex(int address) {
        if (address < startAddress || address >= endAddress) {
            return -1;
        }
        var index = CollectionUtil.binarySearch(locations, address, InstructionLocation::address);
        if (index < 0) {
            index = -index - 2;
        }
        return index;
    }
}
