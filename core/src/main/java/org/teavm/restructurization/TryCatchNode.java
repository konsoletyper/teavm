/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

import java.util.Objects;
import org.teavm.model.Variable;

class TryCatchNode {
    String exceptionType;
    Variable variable;
    LabeledBlock handler;
    Block openingBlock;
    TryCatchNode containing;

    TryCatchNode(String exceptionType, Variable variable, LabeledBlock handler, Block openingBlock, TryCatchNode containing) {
        this.exceptionType = exceptionType;
        this.variable = variable;
        this.handler = handler;
        this.openingBlock = openingBlock;
        this.containing = containing;
    }

    boolean sameAs(TryCatchNode other) {
        return Objects.equals(exceptionType, other.exceptionType) && handler == other.handler;
    }
}
