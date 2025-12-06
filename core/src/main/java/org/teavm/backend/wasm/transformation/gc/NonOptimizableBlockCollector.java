/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.transformation.gc;

import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCastBranch;
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.transformation.SuspensionPointCollector;

class NonOptimizableBlockCollector extends WasmDefaultExpressionVisitor {
    SuspensionPointCollector suspendable;
    Set<WasmBlock> nonOptimizableBlocks;
    private Set<WasmBlock> breakTargets;
    private boolean isInSuspendingBlock = true;
    private WasmBlock currentBlock;

    @Override
    public void visit(WasmBlock expression) {
        var oldIsInSuspendingBlock = isInSuspendingBlock;
        var oldCurrentBlock = currentBlock;
        var oldBreakTargets = breakTargets;
        isInSuspendingBlock = suspendable.isSuspending(expression);
        currentBlock = expression;
        breakTargets = new LinkedHashSet<>();
        super.visit(expression);
        if (nonOptimizableBlocks.contains(expression)) {
            nonOptimizableBlocks.addAll(breakTargets);
        }
        isInSuspendingBlock = oldIsInSuspendingBlock;
        currentBlock = oldCurrentBlock;
        breakTargets = oldBreakTargets;
    }

    @Override
    public void visit(WasmBreak expression) {
        super.visit(expression);
        visitBranch(expression.getTarget());
    }

    @Override
    public void visit(WasmBranch expression) {
        super.visit(expression);
        visitBranch(expression.getTarget());
    }

    @Override
    public void visit(WasmCastBranch expression) {
        super.visit(expression);
        visitBranch(expression.getTarget());
    }

    @Override
    public void visit(WasmNullBranch expression) {
        super.visit(expression);
        visitBranch(expression.getTarget());
    }

    private void visitBranch(WasmBlock target) {
        if (!isInSuspendingBlock) {
            if (suspendable.isSuspending(target)) {
                nonOptimizableBlocks.add(target);
            }
        }
        breakTargets.add(target);
    }
}
