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

import java.util.Set;
import org.teavm.ast.ControlFlowEntry;
import org.teavm.backend.javascript.codegen.RememberedSource;
import org.teavm.model.AccessLevel;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReference;

public class PreparedMethod {
    public final AccessLevel accessLevel;
    public final Set<ElementModifier> modifiers;
    public final MethodReference reference;
    public final RememberedSource body;
    public final RememberedSource parameters;
    public final boolean async;
    public final ControlFlowEntry[] cfg;
    public final PreparedVariable[] variables;

    public PreparedMethod(AccessLevel accessLevel, Set<ElementModifier> modifiers, MethodReference reference,
            RememberedSource body, RememberedSource parameters, boolean async, ControlFlowEntry[] cfg,
            PreparedVariable[] variables) {
        this.accessLevel = accessLevel;
        this.modifiers = modifiers;
        this.reference = reference;
        this.body = body;
        this.parameters = parameters;
        this.async = async;
        this.cfg = cfg;
        this.variables = variables;
    }
}
