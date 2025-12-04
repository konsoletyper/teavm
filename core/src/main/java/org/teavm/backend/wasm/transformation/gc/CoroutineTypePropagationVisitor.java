/*
 *  Copyright 2025 lax1dude.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmCastBranch;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmCopy;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmFill;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt31Get;
import org.teavm.backend.wasm.model.expression.WasmInt31Reference;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmPop;
import org.teavm.backend.wasm.model.expression.WasmPush;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmTry;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.backend.wasm.render.WasmSignature;

class CoroutineTypePropagationVisitor implements WasmExpressionVisitor {

    private final CoroutineTransformationVisitor parent;
    private Map<WasmExpression, PropagationState> propagationState = new HashMap<>();

    private static class PropagationState {

        private final List<WasmType> propagatedTypes = new ArrayList<>();
        private int propagatedDepth;

        private PropagationState(int initialDepth) {
            propagatedDepth = initialDepth;
        }

    }

    CoroutineTypePropagationVisitor(CoroutineTransformationVisitor parent) {
        this.parent = parent;
    }

    public void propagateTypes(WasmBlock block) {
        visit(block);
        assert propagationState.isEmpty();
    }

    private void updatePropagation(WasmBlock block) {
        var top = parent.stackBlocks.size() - 1;
        var currentBlock = parent.stackBlocks.get(top);
        if (block == currentBlock) {
            return;
        }
        for (var i = top - 1; i >= 0; --i) {
            if (block == parent.stackBlocks.get(i)) {
                var base = parent.stackAddedTypes.size();
                if (i < base) {
                    for (var j = top; j >= base; --j) {
                        setPropagationDepth(parent.stackBlocks.get(j), i);
                    }
                }
                return;
            }
        }
        throw new IllegalStateException("Tried to branch out of a block that wasn't on the stack");
    }

    private void setPropagationDepth(WasmBlock block, int depth) {
        PropagationState state = propagationState.get(block);
        if (state == null) {
            state = new PropagationState(parent.stackAddedTypes.size());
            propagationState.put(block, state);
        }
        for (int i = state.propagatedDepth - 1; i >= depth; --i) {
            WasmType[] newTypes = parent.stackAddedTypes.get(i);
            for (int j = newTypes.length - 1; j >= 0; --j) {
                state.propagatedTypes.add(newTypes[j]);
            }
        }
        state.propagatedDepth = depth;
    }

    private void propagate(WasmBlock block) {
        var state = propagationState.remove(block);
        if (state != null && !state.propagatedTypes.isEmpty()) {
            Collections.reverse(state.propagatedTypes);
            List<WasmType> params = state.propagatedTypes;
            List<WasmType> results = new ArrayList<>(state.propagatedTypes);
            WasmBlockType oldType = block.getType();
            if (oldType != null) {
                params.addAll(oldType.getInputTypes());
                params.addAll(oldType.getOutputTypes());
            }
            block.setType(parent.functionTypes.get(
                    new WasmSignature(List.copyOf(results), List.copyOf(params))).asBlock());
        }
    }

    @Override
    public void visit(WasmBlock expr) {
        if (expr.getBody().isEmpty()) {
            return;
        }
        var blocksOnStack = parent.stackBlocks.size();
        parent.stackBlocks.add(expr);
        for (var child : expr.getBody()) {
            child.acceptVisitor(this);
        }
        propagate(expr);
        if (blocksOnStack < parent.stackBlocks.size()) {
            parent.stackBlocks.subList(blocksOnStack, parent.stackBlocks.size()).clear();
        }
    }

    @Override
    public void visit(WasmBranch expr) {
        if (expr.getResult() != null) {
            expr.getResult().acceptVisitor(this);
        }
        expr.getCondition().acceptVisitor(this);
        updatePropagation(expr.getTarget());
    }

    @Override
    public void visit(WasmNullBranch expr) {
        if (expr.getResult() != null) {
            expr.getResult().acceptVisitor(this);
        }
        expr.getValue().acceptVisitor(this);
        updatePropagation(expr.getTarget());
    }

    @Override
    public void visit(WasmCastBranch expr) {
        if (expr.getResult() != null) {
            expr.getResult().acceptVisitor(this);
        }
        expr.getValue().acceptVisitor(this);
        updatePropagation(expr.getTarget());
    }

    @Override
    public void visit(WasmBreak expr) {
        if (expr.getResult() != null) {
            expr.getResult().acceptVisitor(this);
        }
        updatePropagation(expr.getTarget());
    }

    @Override
    public void visit(WasmSwitch expr) {
        for (var block : expr.getTargets()) {
            updatePropagation(block);
        }
        updatePropagation(expr.getDefaultTarget());
    }

    @Override
    public void visit(WasmConditional expr) {
        expr.getCondition().acceptVisitor(this);
        expr.getThenBlock().acceptVisitor(this);
        expr.getElseBlock().acceptVisitor(this);
    }

    @Override
    public void visit(WasmReturn expr) {
        if (expr.getValue() != null) {
            expr.getValue().acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmUnreachable expr) {
    }

    @Override
    public void visit(WasmInt32Constant expr) {
    }

    @Override
    public void visit(WasmInt64Constant expr) {
    }

    @Override
    public void visit(WasmFloat32Constant expr) {
    }

    @Override
    public void visit(WasmFloat64Constant expr) {
    }

    @Override
    public void visit(WasmNullConstant expr) {
    }

    @Override
    public void visit(WasmIsNull expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmGetLocal expr) {
    }

    @Override
    public void visit(WasmSetLocal expr) {
        expr.getValue().acceptVisitor(this); 
    }

    @Override
    public void visit(WasmGetGlobal expr) {
    }

    @Override
    public void visit(WasmSetGlobal expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmIntBinary expr) {
        expr.getFirst().acceptVisitor(this);
        expr.getSecond().acceptVisitor(this);
    }

    @Override
    public void visit(WasmFloatBinary expr) {
        expr.getFirst().acceptVisitor(this);
        expr.getSecond().acceptVisitor(this);
    }

    @Override
    public void visit(WasmIntUnary expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(WasmFloatUnary expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(WasmConversion expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(WasmCall expr) {
        for (var arg : expr.getArguments()) {
            arg.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmIndirectCall expr) {
        for (var arg : expr.getArguments()) {
            arg.acceptVisitor(this);
        }
        expr.getSelector().acceptVisitor(this);
    }

    @Override
    public void visit(WasmCallReference expr) {
        for (var arg : expr.getArguments()) {
            arg.acceptVisitor(this);
        }
        expr.getFunctionReference().acceptVisitor(this);
    }

    @Override
    public void visit(WasmDrop expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(WasmLoadInt32 expr) {
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(WasmLoadInt64 expr) {
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(WasmLoadFloat32 expr) {
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(WasmLoadFloat64 expr) {
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStoreInt32 expr) {
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStoreInt64 expr) {
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStoreFloat32 expr) {
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStoreFloat64 expr) {
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmMemoryGrow expr) {
        expr.getAmount().acceptVisitor(this);
    }

    @Override
    public void visit(WasmFill expr) {
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
        expr.getCount().acceptVisitor(this);
    }

    @Override
    public void visit(WasmCopy expr) {
        expr.getDestinationIndex().acceptVisitor(this);
        expr.getSourceIndex().acceptVisitor(this);
        expr.getCount().acceptVisitor(this);
    }

    @Override
    public void visit(WasmTry expr) {
        for (var child : expr.getBody()) {
            child.acceptVisitor(this);
        }
        for (var catchChild : expr.getCatches()) {
            for (var child : catchChild.getBody()) {
                child.acceptVisitor(this);
            }
        }
    }

    @Override
    public void visit(WasmThrow expr) {
        for (var child : expr.getArguments()) {
            child.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmReferencesEqual expr) {
        expr.getFirst().acceptVisitor(this);
        expr.getSecond().acceptVisitor(this);
    }

    @Override
    public void visit(WasmCast expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmExternConversion expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmTest expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStructNew expr) {
        for (var init : expr.getInitializers()) {
            init.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmStructNewDefault expr) {
    }

    @Override
    public void visit(WasmStructGet expr) {
        expr.getInstance().acceptVisitor(this);
    }

    @Override
    public void visit(WasmStructSet expr) {
        expr.getInstance().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmArrayNewDefault expr) {
        expr.getLength().acceptVisitor(this);
    }

    @Override
    public void visit(WasmArrayNewFixed expr) {
        for (var init : expr.getElements()) {
            init.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmArrayGet expr) {
        expr.getInstance().acceptVisitor(this);
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(WasmArraySet expr) {
        expr.getInstance().acceptVisitor(this);
        expr.getIndex().acceptVisitor(this);
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmArrayLength expr) {
        expr.getInstance().acceptVisitor(this);
    }

    @Override
    public void visit(WasmArrayCopy expr) {
        expr.getTargetArray().acceptVisitor(this);
        expr.getTargetIndex().acceptVisitor(this);
        expr.getSourceArray().acceptVisitor(this);
        expr.getSourceIndex().acceptVisitor(this);
        expr.getSize().acceptVisitor(this);
    }

    @Override
    public void visit(WasmFunctionReference expr) {
    }

    @Override
    public void visit(WasmInt31Reference expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmInt31Get expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmPush expr) {
        expr.getArgument().acceptVisitor(this);
    }

    @Override
    public void visit(WasmPop expr) {
    }

}
