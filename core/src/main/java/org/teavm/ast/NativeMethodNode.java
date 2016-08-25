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
package org.teavm.ast;

import java.util.Collections;
import java.util.List;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.model.MethodReference;

public class NativeMethodNode extends MethodNode {
    private Generator generator;
    private boolean async;

    public NativeMethodNode(MethodReference reference) {
        super(reference);
    }

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public void acceptVisitor(MethodNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<VariableNode> getVariables() {
        return Collections.emptyList();
    }
}
