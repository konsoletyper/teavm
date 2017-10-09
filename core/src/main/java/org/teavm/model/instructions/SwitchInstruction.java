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
package org.teavm.model.instructions;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Variable;

public class SwitchInstruction extends Instruction {
    private Variable condition;
    private List<SwitchTableEntry> entries = new ArrayList<>();
    private BasicBlock defaultTarget;

    public Variable getCondition() {
        return condition;
    }

    public void setCondition(Variable condition) {
        this.condition = condition;
    }

    private List<SwitchTableEntry> safeEntries = new AbstractList<SwitchTableEntry>() {
        @Override
        public SwitchTableEntry set(int index, SwitchTableEntry element) {
            SwitchTableEntry oldElement = entries.get(index);
            oldElement.setInstruction(null);
            entries.set(index, element);
            element.setInstruction(SwitchInstruction.this);
            return oldElement;
        }

        @Override
        public void add(int index, SwitchTableEntry element) {
            entries.add(index, element);
            element.setInstruction(SwitchInstruction.this);
        }

        @Override
        public SwitchTableEntry remove(int index) {
            SwitchTableEntry element = entries.remove(index);
            element.setInstruction(null);
            return element;
        }

        @Override
        public void clear() {
            for (SwitchTableEntry element : entries) {
                element.setInstruction(null);
            }
            entries.clear();
        }

        @Override
        public SwitchTableEntry get(int index) {
            return entries.get(index);
        }

        @Override
        public int size() {
            return entries.size();
        }
    };

    public List<SwitchTableEntry> getEntries() {
        return safeEntries;
    }

    public BasicBlock getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(BasicBlock defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
