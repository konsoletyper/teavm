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
package org.teavm.wasm.decompile;

import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.LoopGraph;
import org.teavm.model.Program;
import org.teavm.model.util.VariableType;
import org.teavm.wasm.model.WasmLocal;

public interface Context {
    void push(Step step);

    void setLabel(int node, Label label);

    Label getLabel(int node);

    Graph getDomGraph();

    LoopGraph getCfg();

    Program getProgram();

    DominatorTree getDomTree();

    WasmLocal getLocal(int index);

    VariableType getLocalType(int index);
}
