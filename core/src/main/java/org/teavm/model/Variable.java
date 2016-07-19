/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Variable implements VariableReader {
    private Program program;
    private int index;
    private int register;
    private Set<String> debugNames;

    Variable(Program program) {
        this.program = program;
        this.debugNames = new HashSet<>();
    }

    @Override
    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    @Override
    public Program getProgram() {
        return program;
    }

    void setProgram(Program program) {
        this.program = program;
    }

    @Override
    public int getRegister() {
        return register;
    }

    public void setRegister(int register) {
        this.register = register;
    }

    public Set<String> getDebugNames() {
        return debugNames;
    }

    @Override
    public Set<String> readDebugNames() {
        return Collections.unmodifiableSet(debugNames);
    }
}
