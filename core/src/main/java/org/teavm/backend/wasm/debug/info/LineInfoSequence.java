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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LineInfoSequence {
    private int startAddress;
    private int endAddress;
    private MethodInfo method;
    private LineInfoCommand[] commands;
    private List<LineInfoCommand> commandList;

    public LineInfoSequence(int startAddress, int endAddress, MethodInfo method, LineInfoCommand[] commands) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.method = method;
        this.commands = commands.clone();
        commandList = Collections.unmodifiableList(Arrays.asList(this.commands));
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

    public List<? extends LineInfoCommand> commands() {
        return commandList;
    }

    public LineInfoUnpackedSequence unpack() {
        var commandExecutor = new LineInfoCommandExecutor();
        var locations = new ArrayList<InstructionLocation>();
        for (var command : commands) {
            command.acceptVisitor(commandExecutor);
            var location = commandExecutor.createLocation();
            if (location != null) {
                locations.add(location);
            }
        }
        return new LineInfoUnpackedSequence(startAddress, endAddress, method, locations);
    }
}
