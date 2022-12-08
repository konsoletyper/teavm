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

public class LineInfoCommandExecutor implements LineInfoCommandVisitor {
    private FileInfo file;
    private int line = 1;
    private int address;
    private InliningLocation inliningLocation;

    public InstructionLocation createLocation() {
        return new InstructionLocation(address, file != null ? new Location(file, line, inliningLocation) : null);
    }

    @Override
    public void visit(LineInfoEnterCommand command) {
        address = command.address();
        inliningLocation = new InliningLocation(new Location(file, line, inliningLocation), command.method());
        file = null;
        line = 1;
    }

    @Override
    public void visit(LineInfoExitCommand command) {
        address = command.address();
        inliningLocation = inliningLocation.location().inlining();
    }

    @Override
    public void visit(LineInfoFileCommand command) {
        address = command.address();
        file = command.file();
        line = command.line();
    }

    @Override
    public void visit(LineInfoLineCommand command) {
        address = command.address();
        line = command.line();
    }
}
