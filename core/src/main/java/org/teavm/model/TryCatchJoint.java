/*
 *  Copyright 2016 Alexey Andreev.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TryCatchJoint implements TryCatchJointReader {
    private List<Variable> sourceVariables = new ArrayList<>();
    private List<VariableReader> readonlySourceVariables;
    private Variable receiver;
    TryCatchBlock block;

    @Override
    public List<VariableReader> readSourceVariables() {
        if (readonlySourceVariables == null) {
            readonlySourceVariables = Collections.unmodifiableList(sourceVariables);
        }
        return readonlySourceVariables;
    }

    public List<Variable> getSourceVariables() {
        return sourceVariables;
    }

    @Override
    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public TryCatchBlock getBlock() {
        return block;
    }
}
