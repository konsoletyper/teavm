/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.util;

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import org.teavm.common.GraphSplittingBackend;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;

/**
 *
 * @author Alexey Andreev
 */
public class ProgramNodeSplittingBackend implements GraphSplittingBackend {
    private Program program;

    public ProgramNodeSplittingBackend(Program program) {
        this.program = program;
    }

    @Override
    public int[] split(int[] domain, int[] nodes) {
        int[] copies = new int[nodes.length];
        IntIntMap map = new IntIntOpenHashMap();
        for (int i = 0; i < nodes.length; ++i) {
            int node = nodes[i];
            BasicBlock block = program.basicBlockAt(node);
            BasicBlock blockCopy = program.createBasicBlock();
            blockCopy.getInstructions().addAll(ProgramUtils.copyInstructions(block, 0,
                    block.getInstructions().size(), program));
            copies[i] = blockCopy.getIndex();
            map.put(nodes[i], copies[i] + 1);
        }
        CopyBlockMapper copyBlockMapper = new CopyBlockMapper(map);
        for (int i = 0; i < copies.length; ++i) {
            copyBlockMapper.transform(program.basicBlockAt(copies[i]));
        }
        for (int i = 0; i < domain.length; ++i) {
            copyBlockMapper.transform(program.basicBlockAt(domain[i]));
        }
        return copies;
    }

    private static class CopyBlockMapper extends BasicBlockMapper {
        private IntIntMap map;

        public CopyBlockMapper(IntIntMap map) {
            this.map = map;
        }

        @Override
        protected BasicBlock map(BasicBlock block) {
            int mappedIndex = map.get(block.getIndex());
            if (mappedIndex == 0) {
                return block;
            }
            return block.getProgram().basicBlockAt(mappedIndex - 1);
        }
    }
}
