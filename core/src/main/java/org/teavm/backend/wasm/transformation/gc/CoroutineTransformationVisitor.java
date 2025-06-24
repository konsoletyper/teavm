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
package org.teavm.backend.wasm.transformation.gc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.model.WasmLocal;
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
import org.teavm.backend.wasm.render.WasmSignature;
import org.teavm.backend.wasm.render.WasmTypeInference;
import org.teavm.backend.wasm.transformation.SuspensionPointCollector;

class CoroutineTransformationVisitor implements WasmExpressionVisitor {
    private SuspensionPointCollector collector;
    private WasmFunctionTypes functionTypes;
    private CoroutineFunctions functions;
    private SwitchContainer currentSwitchContainer;
    public WasmLocal stateLocal;
    public int currentStateOffset;
    private List<WasmExpression> resultList = new ArrayList<>();
    private Map<WasmBlock, WasmBlock> blockMap = new HashMap<>();
    private WasmBlock mainBlock = new WasmBlock(false);
    private WasmLocal fiberLocal;
    private WasmTypeInference typeInference;
    private TemporaryVariablePool tempVars;
    private List<WasmType> stackTypes = new ArrayList<>();

    CoroutineTransformationVisitor(SuspensionPointCollector collector, WasmFunctionTypes functionTypes,
            CoroutineFunctions functions) {
        this.collector = collector;
        this.functionTypes = functionTypes;
        this.functions = functions;
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
        stackTypes.addAll(block.getResultTypes());
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
        if (expression.getResult() != null && collector.isSuspending(expression.getResult())) {
            expression.getResult().acceptVisitor(this);
            expression.setResult(new WasmPop(peekType(0)));
            popTypes(1);
        }
        var target = blockMap.get(expression.getTarget());
        expression.setTarget(target);
    }

    @Override
    public void visit(WasmSwitch expression) {

    }

    @Override
    public void visit(WasmConditional expression) {

    }

    @Override
    public void visit(WasmReturn expression) {
        if (collector.isSuspending(expression.getValue())) {
            expression.getValue().acceptVisitor(this);
            expression.setValue(new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    @Override
    public void visit(WasmUnreachable expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmNullConstant expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmIsNull expression) {
        if (collector.isSuspending(expression.getValue())) {
            expression.getValue().acceptVisitor(this);
            expression.setValue(new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    @Override
    public void visit(WasmGetLocal expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmSetLocal expression) {
        if (collector.isSuspending(expression.getValue())) {
            expression.getValue().acceptVisitor(this);
            expression.setValue(new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        if (collector.isSuspending(expression.getValue())) {
            expression.getValue().acceptVisitor(this);
            expression.setValue(new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    @Override
    public void visit(WasmIntBinary expression) {
        if (collector.isSuspending(expression.getSecond())) {
            expression.getFirst().acceptVisitor(this);
            expression.getSecond().acceptVisitor(this);
            expression.setFirst(new WasmPop(peekType(1)));
            expression.setSecond(new WasmPop(peekType(0)));
            popTypes(2);
        } else if (collector.isSuspending(expression.getFirst())) {
            expression.getFirst().acceptVisitor(this);
            expression.setFirst(new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        if (collector.isSuspending(expression.getSecond())) {
            expression.getFirst().acceptVisitor(this);
            expression.setFirst(new WasmPop(addExpr(expression.getFirst())));
            expression.getSecond().acceptVisitor(this);
            expression.setFirst(new WasmPop(addExpr(expression.getSecond())));
            popTypes(2);
        } else if (collector.isSuspending(expression.getFirst())) {
            expression.getFirst().acceptVisitor(this);
            expression.setFirst(new WasmPop(addExpr(expression.getFirst())));
            popTypes(1);
        }
        addExpr(expression);
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

        var arguments = expression.getArguments();
        var argTypes = new WasmType[arguments.size()];
        for (var i = 0; i < arguments.size(); ++i) {
            var arg = arguments.get(i);
            arg.acceptVisitor(this);
            var type = addExpr(arg);
            arguments.set(i, new WasmPop(type));
            argTypes[i] = type;
        }

        var argsBlockType = functionTypes.get(new WasmSignature(List.copyOf(stackTypes), List.of())).asBlock();
        var innerBlock = new WasmBlock(false);
        var outerBlock = new WasmBlock(false);
        outerBlock.setType(argsBlockType);
        innerBlock.getBody().addAll(resultList);
        resultList.clear();
        innerBlock.getBody().add(new WasmBreak(outerBlock));
        outerBlock.getBody().add(innerBlock);
        var entriesToRestore = stackTypes.size() - arguments.size();
        for (var type : stackTypes.subList(0, stackTypes.size() - arguments.size())) {
            restore(outerBlock.getBody(), type);
        }
        for (var type : argTypes) {
            pushDefault(outerBlock.getBody(), type);
        }

        resultList.add(outerBlock);
        resultList.add(expression);
        popTypes(arguments.size());

        var suspendCheck = new WasmConditional(isSuspending());
        for (var i = stackTypes.size() - 1; i >= 0; --i) {
            save(suspendCheck.getThenBlock().getBody(), stackTypes.get(i));
        }
        suspendCheck.getThenBlock().getBody().add(new WasmBreak(mainBlock));
        resultList.add(suspendCheck);

        var state = currentStateOffset++;
        saveState(resultList, state);
        currentSwitchContainer.switchExpr.getTargets().add(innerBlock);
    }

    @Override
    public void visit(WasmIndirectCall expression) {

    }

    @Override
    public void visit(WasmCallReference expression) {

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

    private WasmType addExpr(WasmExpression expr) {
        expr.acceptVisitor(typeInference);
        resultList.add(expr);
        var type = typeInference.getSingleResult();
        if (type != null) {
            stackTypes.add(type);
        }
        return type;
    }

    private void popTypes(int count) {
        stackTypes.subList(stackTypes.size() - count, stackTypes.size()).clear();
    }

    private WasmType peekType(int depth) {
        return stackTypes.get(stackTypes.size() - depth - 1);
    }

    private void pushDefault(List<WasmExpression> into, WasmType type) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    into.add(new WasmCall(functions.pushInt(), new WasmGetLocal(fiberLocal),
                            new WasmInt32Constant(0)));
                    break;
                case INT64:
                    into.add(new WasmCall(functions.pushLong(), new WasmGetLocal(fiberLocal),
                            new WasmInt64Constant(0)));
                    break;
                case FLOAT32:
                    into.add(new WasmCall(functions.pushFloat(), new WasmGetLocal(fiberLocal),
                            new WasmFloat32Constant(0)));
                    break;
                case FLOAT64:
                    into.add(new WasmCall(functions.pushDouble(), new WasmGetLocal(fiberLocal),
                            new WasmFloat64Constant(0)));
                    break;
            }
        } else {
            into.add(new WasmCall(functions.pushObject(), new WasmGetLocal(fiberLocal),
                    new WasmNullConstant(functions.objectStructure().getReference())));
        }
    }

    private void save(List<WasmExpression> into, WasmType type) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    into.add(new WasmCall(functions.pushInt(), new WasmGetLocal(fiberLocal), new WasmPop(type)));
                    break;
                case INT64:
                    into.add(new WasmCall(functions.pushLong(), new WasmGetLocal(fiberLocal), new WasmPop(type)));
                    break;
                case FLOAT32:
                    into.add(new WasmCall(functions.pushFloat(), new WasmGetLocal(fiberLocal), new WasmPop(type)));
                    break;
                case FLOAT64:
                    into.add(new WasmCall(functions.pushDouble(), new WasmGetLocal(fiberLocal), new WasmPop(type)));
                    break;
            }
        } else {
            into.add(new WasmCall(functions.pushObject(), new WasmGetLocal(fiberLocal), new WasmPop(type)));
        }
    }

    private void saveState(List<WasmExpression> into, int index) {
        into.add(new WasmCall(functions.pushInt(), new WasmGetLocal(fiberLocal), new WasmInt32Constant(index)));
    }

    private void restore(List<WasmExpression> into, WasmType type) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    into.add(new WasmPush(new WasmCall(functions.popInt(), new WasmGetLocal(fiberLocal))));
                    break;
                case INT64:
                    into.add(new WasmPush(new WasmCall(functions.popLong(), new WasmGetLocal(fiberLocal))));
                    break;
                case FLOAT32:
                    into.add(new WasmPush(new WasmCall(functions.popFloat(), new WasmGetLocal(fiberLocal))));
                    break;
                case FLOAT64:
                    into.add(new WasmPush(new WasmCall(functions.popDouble(), new WasmGetLocal(fiberLocal))));
                    break;
            }
        } else if (type instanceof WasmType.Reference) {
            var refType = (WasmType.Reference) type;
            var obj = new WasmCall(functions.popObject(), new WasmGetLocal(fiberLocal));
            into.add(new WasmPush(new WasmCast(obj, refType)));
        }
    }

    private WasmExpression isResuming() {
        return new WasmCall(functions.isResuming(), new WasmGetLocal(fiberLocal));
    }

    private WasmExpression isSuspending() {
        return new WasmCall(functions.isSuspending(), new WasmGetLocal(fiberLocal));
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


/*
push N
push X
push Y
call
consume //N

isRestoring
{@0
    if {
        restore
        pop STATE
        restoreLocals
    } else {
        push 0
        pop STATE
    }
    {@1
        {@2
            {@3
                push STATE
                switch {
                    case 0:
                        break 3
                    case 1:
                        break 2
                }
            }
            push(N)
            push(X)
            push(Y)
            break 1
        }
        restore // N
        push 0
        push 0
    }
    call
    isSuspending
    if {
        save // N
        br 0
    }
    consume // N
    return
}
saveLocals
 */