/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.tooling.sources;

import java.util.Set;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.instructions.AbstractInstructionReader;

class ProgramSourceAggregator extends AbstractInstructionReader {
    private Set<String> sourceFiles;

    public ProgramSourceAggregator(Set<String> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public void addLocationsOfProgram(ProgramReader program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            block.readAllInstructions(this);
        }
    }

    @Override
    public void location(TextLocation location) {
        if (location != null && location.getFileName() != null && !location.getFileName().isEmpty()) {
            sourceFiles.add(location.getFileName());
        }
    }
}
