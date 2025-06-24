/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.backend.wasm.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.model.WasmLocal;
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
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
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
import org.teavm.backend.wasm.render.WasmTypeInference;

public class CoroutineTransformation implements WasmExpressionVisitor {
    private SuspensionPointCollector collector;
    private SwitchContainer currentSwitchContainer;
    public WasmLocal stateLocal;
    public int currentStateOffset;
    private List<WasmExpression> resultList = new ArrayList<>();
    private Map<WasmBlock, WasmBlock> blockMap = new HashMap<>();
    private WasmTypeInference typeInference;
    private TemporaryVariablePool tempVars;

    public CoroutineTransformation(SuspensionPointCollector collector) {
        this.collector = collector;
        currentSwitchContainer = createSwitchContainer();
    }

    private SwitchContainer createSwitchContainer() {
        var block = new WasmBlock(false);
        var container = new SwitchContainer(currentStateOffset);
        WasmExpression expr = new WasmGetLocal(stateLocal);
        if (currentStateOffset > 0) {
            expr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, expr,
                    new WasmInt32Constant(currentStateOffset));
        }
        var switchExpr = new WasmSwitch(expr, block);
        block.getBody().add(switchExpr);

        var outerBlock = new WasmBlock(false);
        outerBlock.getBody().add(block);
        outerBlock.getBody().add(new WasmUnreachable());
        container.switchExpr = switchExpr;
        resultList.add(outerBlock);
        container.parent = currentSwitchContainer;
        return container;
    }

    @Override
    public void visit(WasmBlock expression) {
        if (!collector.isSuspending(expression)) {
            addExpr(expression);
            return;
        }
        if (expression.isLoop()) {

        } else {
            splitRegularBlock(expression);
        }
    }

    private void splitLoopBlock(WasmBlock block) {

    }

    private void splitRegularBlock(WasmBlock block) {
        var replacement = new WasmBlock(false);
        visitMany(block.getBody());
        blockMap.put(block, replacement);
        replacement.setType(block.getType());
        replacement.setLocation(block.getLocation());
        replacement.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(replacement);
    }

    @Override
    public void visit(WasmBranch expression) {
        var target = blockMap.get(expression.getTarget());
        expression.setTarget(target);
    }

    @Override
    public void visit(WasmNullBranch expression) {

    }

    @Override
    public void visit(WasmCastBranch expression) {

    }

    @Override
    public void visit(WasmBreak expression) {
        var target = blockMap.get(expression.getTarget());
        expression.setTarget(target);
        addExpr(expression);
    }

    @Override
    public void visit(WasmSwitch expression) {

    }

    @Override
    public void visit(WasmConditional expression) {

    }

    @Override
    public void visit(WasmReturn expression) {

    }

    @Override
    public void visit(WasmUnreachable expression) {

    }

    @Override
    public void visit(WasmInt32Constant expression) {

    }

    @Override
    public void visit(WasmInt64Constant expression) {

    }

    @Override
    public void visit(WasmFloat32Constant expression) {

    }

    @Override
    public void visit(WasmFloat64Constant expression) {

    }

    @Override
    public void visit(WasmNullConstant expression) {

    }

    @Override
    public void visit(WasmIsNull expression) {

    }

    @Override
    public void visit(WasmGetLocal expression) {

    }

    @Override
    public void visit(WasmSetLocal expression) {

    }

    @Override
    public void visit(WasmGetGlobal expression) {

    }

    @Override
    public void visit(WasmSetGlobal expression) {

    }

    @Override
    public void visit(WasmIntBinary expression) {

    }

    @Override
    public void visit(WasmFloatBinary expression) {

    }

    @Override
    public void visit(WasmIntUnary expression) {

    }

    @Override
    public void visit(WasmFloatUnary expression) {

    }

    @Override
    public void visit(WasmConversion expression) {

    }

    @Override
    public void visit(WasmCall expression) {
        if (!collector.isSuspending(expression)) {
            addExpr(expression);
            return;
        }

        processArguments(expression.getArguments());

        var state = currentStateOffset++;
        beforeCallSite(state);

        var block = new WasmBlock(false);
        resultList.get(resultList.size() - 1).acceptVisitor(typeInference);
        block.setType(typeInference.getResult());
        block.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(block);

        for (var sc = currentSwitchContainer; sc != null; sc = sc.parent) {
            sc.switchExpr.getTargets().add(block);
        }

        resultList.add(expression);

        afterCallSite(state);
    }

    @Override
    public void visit(WasmIndirectCall expression) {

    }

    @Override
    public void visit(WasmCallReference expression) {

    }

    private void processArguments(List<WasmExpression> arguments) {
        var locals = new ArrayList<WasmLocal>();
        for (var i = 0; i < firstNonSuspendingArgument; ++i) {
            var arg = arguments.get(i);
            arg.acceptVisitor(typeInference);
            var type = typeInference.getResult();
            arg.acceptVisitor(this);
            var tempVar = tempVars.acquire(type);
            resultList.add(new WasmSetLocal(tempVar, arg));
            arguments.set(i, new WasmGetLocal(tempVar));
            locals.add(tempVar);
        }
        for (var i = locals.size() - 1; i >= 0; --i) {
            tempVars.release(locals.get(i));
        }
    }

    @Override
    public void visit(WasmDrop expression) {

    }

    @Override
    public void visit(WasmLoadInt32 expression) {

    }

    @Override
    public void visit(WasmLoadInt64 expression) {

    }

    @Override
    public void visit(WasmLoadFloat32 expression) {

    }

    @Override
    public void visit(WasmLoadFloat64 expression) {

    }

    @Override
    public void visit(WasmStoreInt32 expression) {

    }

    @Override
    public void visit(WasmStoreInt64 expression) {

    }

    @Override
    public void visit(WasmStoreFloat32 expression) {

    }

    @Override
    public void visit(WasmStoreFloat64 expression) {

    }

    @Override
    public void visit(WasmMemoryGrow expression) {

    }

    @Override
    public void visit(WasmFill expression) {

    }

    @Override
    public void visit(WasmCopy expression) {

    }

    @Override
    public void visit(WasmTry expression) {

    }

    @Override
    public void visit(WasmThrow expression) {

    }

    @Override
    public void visit(WasmReferencesEqual expression) {

    }

    @Override
    public void visit(WasmCast expression) {

    }

    @Override
    public void visit(WasmExternConversion expression) {

    }

    @Override
    public void visit(WasmTest expression) {

    }

    @Override
    public void visit(WasmStructNew expression) {

    }

    @Override
    public void visit(WasmStructNewDefault expression) {

    }

    @Override
    public void visit(WasmStructGet expression) {

    }

    @Override
    public void visit(WasmStructSet expression) {

    }

    @Override
    public void visit(WasmArrayNewDefault expression) {

    }

    @Override
    public void visit(WasmArrayNewFixed expression) {

    }

    @Override
    public void visit(WasmArrayGet expression) {

    }

    @Override
    public void visit(WasmArraySet expression) {

    }

    @Override
    public void visit(WasmArrayLength expression) {

    }

    @Override
    public void visit(WasmArrayCopy expression) {

    }

    @Override
    public void visit(WasmFunctionReference expression) {

    }

    @Override
    public void visit(WasmInt31Reference expression) {

    }

    @Override
    public void visit(WasmInt31Get expression) {

    }

    @Override
    public void visit(WasmPush expression) {

    }

    @Override
    public void visit(WasmPop expression) {

    }

    private void visitMany(List<WasmExpression> expressions) {
        for (var expr : expressions) {
            expr.acceptVisitor(this);
        }
    }

    private void addExpr(WasmExpression expr) {
        resultList.add(expr);
    }

    protected void beforeCallSite(int state) {

    }

    protected void afterCallSite(int state) {

    }

    private static class SwitchContainer {
        SwitchContainer parent;
        final int offset;
        WasmSwitch switchExpr;

        SwitchContainer(int offset) {
            this.offset = offset;
        }
    }
}
