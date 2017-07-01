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

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.*;

public class ConstructMultiArrayInstruction extends Instruction {
    private List<Variable> dimensions = new ArrayList<>();
    private ValueType itemType;
    private Variable receiver;

    public ValueType getItemType() {
        return itemType;
    }

    public void setItemType(ValueType itemType) {
        this.itemType = itemType;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public List<Variable> getDimensions() {
        return dimensions;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
