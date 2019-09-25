/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.javascript.decompile;

import org.teavm.ast.ControlFlowEntry;
import org.teavm.ast.MethodNode;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;

public class PreparedMethod {
    public final MethodHolder methodHolder;
    public final MethodReference reference;
    public final MethodNode node;
    public final Generator generator;
    public final boolean async;
    public final ControlFlowEntry[] cfg;

    public PreparedMethod(MethodHolder method, MethodNode node, Generator generator, boolean async,
            ControlFlowEntry[] cfg) {
        this.reference = method.getReference();
        this.methodHolder = method;
        this.node = node;
        this.generator = generator;
        this.async = async;
        this.cfg = cfg;
    }
}
