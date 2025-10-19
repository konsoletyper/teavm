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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.teavm.backend.wasm.WasmFunctionTypes;
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
    SuspensionPointCollector collector;
    private WasmFunctionTypes functionTypes;
    private CoroutineFunctions functions;
    private SwitchContainer currentSwitchContainer;
    WasmLocal stateLocal;
    WasmLocal fiberLocal;
    Supplier<WasmLocal> tmpValueLocalSupplier;
    private int currentStateOffset;
    List<WasmExpression> resultList = new ArrayList<>();
    WasmBlock mainBlock;
    private WasmTypeInference typeInference;
    private List<WasmType> stackTypes = new ArrayList<>();

    CoroutineTransformationVisitor(WasmFunctionTypes functionTypes, CoroutineFunctions functions) {
        this.functionTypes = functionTypes;
        this.functions = functions;
        typeInference = new WasmTypeInference();
    }

    void init() {
        currentSwitchContainer = createSwitchContainer();
        mainBlock = new WasmBlock(false);
    }

    void complete() {
        resultList.clear();
        currentStateOffset = 0;
        stateLocal = null;
        fiberLocal = null;
        collector = null;
        currentSwitchContainer = null;
        stackTypes.clear();
    }

    private SwitchContainer createSwitchContainer() {
        var block = new WasmBlock(false);
        var container = new SwitchContainer();
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
        switchExpr.getTargets().add(outerBlock);
        currentStateOffset++;
        container.switchExpr = switchExpr;
        resultList.add(outerBlock);
        container.parent = currentSwitchContainer;
        return container;
    }

    @Override
    public void visit(WasmBlock expression) {
        if (expression.isLoop()) {
            splitLoopBlock(expression);
        } else {
            splitRegularBlock(expression);
        }
    }

    private void splitLoopBlock(WasmBlock block) {
        var oldResultList = resultList;
        var oldStackTypes = stackTypes;
        block.setLoop(false);
        resultList.add(new WasmSetLocal(stateLocal, new WasmInt32Constant(currentStateOffset)));
        var jumpInsideBlock = new WasmBlock(false);
        jumpInsideBlock.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(jumpInsideBlock);
        stackTypes = new ArrayList<>();
        var containingBlock = new WasmBlock(true);
        containingBlock.getBody().add(block);
        containingBlock.getBody().add(new WasmSetLocal(stateLocal, new WasmInt32Constant(currentStateOffset)));
        containingBlock.getBody().add(new WasmBreak(containingBlock));
        resultList.add(containingBlock);
        resultList = new ArrayList<>();
        var oldSwitchContainer = currentSwitchContainer;
        currentSwitchContainer = createSwitchContainer();
        oldSwitchContainer.jumpInsideBlock = jumpInsideBlock;
        addJumpsToOuterSwitches(currentSwitchContainer);
        visitMany(block.getBody());
        block.getBody().clear();
        block.getBody().addAll(resultList);
        resultList = oldResultList;
        currentSwitchContainer = oldSwitchContainer;
        oldSwitchContainer.jumpInsideBlock = null;
        stackTypes = oldStackTypes;
    }

    private void splitRegularBlock(WasmBlock block) {
        if (block.getBody().isEmpty()) {
            return;
        }
        var typesOnStack = stackTypes.size();
        visitMany(block.getBody());
        if (typesOnStack < stackTypes.size()) {
            stackTypes.subList(typesOnStack, stackTypes.size()).clear();
        }
        stackTypes.addAll(block.getResultTypes());
        block.getBody().clear();
        block.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(block);
        block.setType(functionTypes.blockType(stackTypes));
    }

    private void visitMany(Iterable<? extends WasmExpression> expressions) {
        for (var expr : expressions) {
            if (collector.isSuspending(expr)) {
                expr.acceptVisitor(this);
            } else {
                addExpr(expr);
            }
        }
    }

    @Override
    public void visit(WasmBranch expression) {
        visitBinary(expression, WasmBranch::getResult, WasmBranch::setResult,
                WasmBranch::getCondition, WasmBranch::setCondition);
    }

    @Override
    public void visit(WasmNullBranch expression) {
        visitBinary(expression, WasmNullBranch::getResult, WasmNullBranch::setResult,
                WasmNullBranch::getValue, WasmNullBranch::setValue);
    }

    @Override
    public void visit(WasmCastBranch expression) {
        visitBinary(
                expression,
                WasmCastBranch::getResult, WasmCastBranch::setResult,
                WasmCastBranch::getValue, WasmCastBranch::setValue
        );
    }

    @Override
    public void visit(WasmBreak expression) {
        visitUnary(expression, WasmBreak::getResult, WasmBreak::setResult);
    }

    @Override
    public void visit(WasmSwitch expression) {
        expression.getSelector().acceptVisitor(this);
        expression.setSelector(new WasmPop(peekType(0)));
        popTypes(0);
        resultList.add(expression);
    }

    @Override
    public void visit(WasmConditional expression) {
        if (collector.isSuspending(expression.getCondition())) {
            expression.getCondition().acceptVisitor(this);
            expression.setCondition(new WasmPop(peekType(0)));
            popTypes(1);
        }
        if (!isSuspending(expression.getThenBlock().getBody()) && !isSuspending(expression.getElseBlock().getBody())) {
            addExpr(expression);
            return;
        }
        var oldStackTypes = stackTypes;
        var elseBlock = new WasmBlock(false);
        var thenBlock = new WasmBlock(false);

        var outputTypes = new ArrayList<>(stackTypes);
        if (expression.getType() != null) {
            outputTypes.addAll(expression.getType().getOutputTypes());
        }
        thenBlock.setType(functionTypes.blockType(outputTypes));
        elseBlock.setType(functionTypes.blockType(stackTypes));

        var br = new WasmBranch(expression.getCondition(), elseBlock);
        resultList.add(br);
        stackTypes = new ArrayList<>(oldStackTypes);
        visitMany(expression.getElseBlock().getBody());
        elseBlock.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(elseBlock);
        if (!elseBlock.getBody().get(elseBlock.getBody().size() - 1).isTerminating()) {
            elseBlock.getBody().add(new WasmBreak(thenBlock));
        }
        stackTypes = new ArrayList<>(oldStackTypes);
        visitMany(expression.getThenBlock().getBody());
        thenBlock.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(thenBlock);
        stackTypes = oldStackTypes;
        if (expression.getType() != null) {
            stackTypes.addAll(expression.getType().getOutputTypes());
        }
    }

    @Override
    public void visit(WasmReturn expression) {
        visitUnary(expression, WasmReturn::getValue, WasmReturn::setValue);
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
        visitUnary(expression, WasmIsNull::getValue, WasmIsNull::setValue);
    }

    @Override
    public void visit(WasmGetLocal expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmSetLocal expression) {
        visitUnary(expression, WasmSetLocal::getValue, WasmSetLocal::setValue);
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        visitUnary(expression, WasmSetGlobal::getValue, WasmSetGlobal::setValue);
    }

    @Override
    public void visit(WasmIntBinary expression) {
        visitBinary(expression, WasmIntBinary::getFirst, WasmIntBinary::setFirst,
                WasmIntBinary::getSecond, WasmIntBinary::setSecond);
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        visitBinary(
                expression,
                WasmFloatBinary::getFirst, WasmFloatBinary::setFirst,
                WasmFloatBinary::getSecond, WasmFloatBinary::setSecond
        );
    }

    @Override
    public void visit(WasmIntUnary expression) {
        visitUnary(expression, WasmIntUnary::getOperand, WasmIntUnary::setOperand);
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        visitUnary(expression, WasmFloatUnary::getOperand, WasmFloatUnary::setOperand);
    }

    @Override
    public void visit(WasmConversion expression) {
        visitUnary(expression, WasmConversion::getOperand, WasmConversion::setOperand);
    }

    @Override
    public void visit(WasmCall expression) {
        visitCall(
                expression,
                expression.isSuspensionPoint(),
                e -> new ArrayList<>(e.getArguments()),
                (e, args) -> {
                    e.getArguments().clear();
                    e.getArguments().addAll(args);
                },
                false,
                expression.getFunction().getType().getParameterTypes(),
                expression.getFunction().getType().getReturnTypes()
        );
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        var argTypes = new ArrayList<WasmType>();
        argTypes.add(WasmType.INT32);
        argTypes.addAll(expression.getType().getParameterTypes());
        visitCall(
                expression,
                expression.isSuspensionPoint(),
                e -> {
                    var result = new ArrayList<WasmExpression>();
                    result.add(e.getSelector());
                    result.addAll(e.getArguments());
                    return result;
                },
                (e, args) -> {
                    e.setSelector(args.get(0));
                    e.getArguments().clear();
                    e.getArguments().addAll(args.subList(1, args.size()));
                },
                false,
                argTypes,
                expression.getType().getReturnTypes()
        );
    }

    @Override
    public void visit(WasmCallReference expression) {
        var argTypes = new ArrayList<WasmType>(expression.getType().getParameterTypes());
        argTypes.add(expression.getType().getReference());
        visitCall(
                expression,
                expression.isSuspensionPoint(),
                e -> {
                    var result = new ArrayList<WasmExpression>();
                    result.addAll(e.getArguments());
                    result.add(e.getFunctionReference());
                    return result;
                },
                (e, args) -> {
                    e.setFunctionReference(args.get(0));
                    e.getArguments().clear();
                    e.getArguments().addAll(args.subList(1, args.size()));
                },
                true,
                argTypes,
                expression.getType().getReturnTypes()
        );
    }

    private <T extends WasmExpression> void visitCall(T expression,
            boolean suspending,
            Function<T, List<WasmExpression>> argsGetter,
            BiConsumer<T, List<? extends WasmExpression>> argsSetter,
            boolean saveLastArg,
            List<? extends WasmType> argTypes,
            List<? extends WasmType> returnTypes) {
        if (!collector.isSuspending(expression)) {
            addExpr(expression);
            return;
        }
        if (!suspending) {
            visitNAry(expression, argsGetter, argsSetter);
            return;
        }

        var arguments = argsGetter.apply(expression);
        for (var i = 0; i < arguments.size(); ++i) {
            var arg = arguments.get(i);
            arg.acceptVisitor(typeInference);
            var type = typeInference.getSingleResult();
            if (collector.isSuspending(arg)) {
                arg.acceptVisitor(this);
            } else {
                resultList.add(arg);
                stackTypes.add(type);
            }
            arguments.set(i, new WasmPop(type));
        }
        stackTypes.subList(stackTypes.size() - argTypes.size(), stackTypes.size()).clear();
        stackTypes.addAll(argTypes);
        argsSetter.accept(expression, arguments);

        var argsBlockType = functionTypes.blockType(stackTypes);
        var innerBlock = new WasmBlock(false);
        var outerBlock = new WasmBlock(false);
        outerBlock.setType(argsBlockType);
        innerBlock.getBody().addAll(resultList);
        resultList.clear();
        innerBlock.getBody().add(new WasmBreak(outerBlock));
        outerBlock.getBody().add(innerBlock);
        for (var type : stackTypes.subList(0, stackTypes.size() - arguments.size())) {
            restore(outerBlock.getBody(), type);
        }
        if (saveLastArg) {
            for (int i = 0; i < argTypes.size() - 1; i++) {
                pushDefault(outerBlock.getBody(), argTypes.get(i));
            }
            var type = argTypes.get(argTypes.size() - 1);
            restore(outerBlock.getBody(), type);
        } else {
            for (var type : argTypes) {
                pushDefault(outerBlock.getBody(), type);
            }
        }

        resultList.add(outerBlock);
        if (saveLastArg) {
            var type = argTypes.get(argTypes.size() - 1);
            resultList.add(new WasmSetLocal(tmpValueLocalSupplier.get(), new WasmPop(type)));
            resultList.add(new WasmCast(new WasmGetLocal(tmpValueLocalSupplier.get()), (WasmType.Reference) type));
        }
        var state = currentStateOffset++;
        resultList.add(new WasmSetLocal(stateLocal, new WasmInt32Constant(state)));
        resultList.add(expression);
        popTypes(arguments.size());

        var suspendCheck = new WasmConditional(isSuspending());
        stackTypes.addAll(returnTypes);
        if (!stackTypes.isEmpty()) {
            var types = List.copyOf(stackTypes);
            suspendCheck.setType(functionTypes.get(new WasmSignature(types, types)).asBlock());
        }
        for (var i = returnTypes.size() - 1; i >= 0; --i) {
            suspendCheck.getThenBlock().getBody().add(new WasmDrop(new WasmPop(returnTypes.get(i))));
        }
        if (saveLastArg) {
            var type = (WasmType.Reference) argTypes.get(argTypes.size() - 1);
            suspendCheck.getThenBlock().getBody().add(new WasmCast(new WasmGetLocal(tmpValueLocalSupplier.get()),
                    type));
            save(suspendCheck.getThenBlock().getBody(), type);
        }
        for (var i = stackTypes.size() - 1 - returnTypes.size(); i >= 0; --i) {
            save(suspendCheck.getThenBlock().getBody(), stackTypes.get(i));
        }
        suspendCheck.getThenBlock().getBody().add(new WasmBreak(mainBlock));
        resultList.add(suspendCheck);

        var sc = currentSwitchContainer;
        sc.switchExpr.getTargets().add(innerBlock);
        addJumpsToOuterSwitches(sc);
    }

    private void addJumpsToOuterSwitches(SwitchContainer sc) {
        while (true) {
            sc = sc.parent;
            if (sc == null) {
                break;
            }
            sc.switchExpr.getTargets().add(sc.jumpInsideBlock);
        }
    }

    @Override
    public void visit(WasmDrop expression) {
        expression.getOperand().acceptVisitor(this);
        var type = stackTypes.remove(stackTypes.size() - 1);
        resultList.add(new WasmDrop(new WasmPop(type)));
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        visitUnary(expression, WasmLoadInt32::getIndex, WasmLoadInt32::setIndex);
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        visitUnary(expression, WasmLoadInt64::getIndex, WasmLoadInt64::setIndex);
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        visitUnary(expression, WasmLoadFloat32::getIndex, WasmLoadFloat32::setIndex);
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        visitUnary(expression, WasmLoadFloat64::getIndex, WasmLoadFloat64::setIndex);
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        visitBinary(expression, WasmStoreInt32::getIndex, WasmStoreInt32::setIndex,
                WasmStoreInt32::getValue, WasmStoreInt32::setValue);
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        visitBinary(expression, WasmStoreInt64::getIndex, WasmStoreInt64::setIndex,
                WasmStoreInt64::getValue, WasmStoreInt64::setValue);
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        visitBinary(expression, WasmStoreFloat32::getIndex, WasmStoreFloat32::setIndex,
                WasmStoreFloat32::getValue, WasmStoreFloat32::setValue);
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        visitBinary(expression, WasmStoreFloat64::getIndex, WasmStoreFloat64::setIndex,
                WasmStoreFloat64::getValue, WasmStoreFloat64::setValue);
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        visitUnary(expression, WasmMemoryGrow::getAmount, WasmMemoryGrow::setAmount);
    }

    @Override
    public void visit(WasmFill expression) {
        visitNAry(
                expression,
                fill -> new ArrayList<>(List.of(fill.getIndex(), fill.getValue(), fill.getCount())),
                (fill, args) -> {
                    fill.setIndex(args.get(0));
                    fill.setValue(args.get(1));
                    fill.setCount(args.get(2));
                }
        );
    }

    @Override
    public void visit(WasmCopy expression) {
        visitNAry(
                expression,
                copy -> new ArrayList<>(List.of(copy.getDestinationIndex(), copy.getSourceIndex(), copy.getCount())),
                (copy, args) -> {
                    copy.setDestinationIndex(args.get(0));
                    copy.setSourceIndex(args.get(1));
                    copy.setCount(args.get(2));
                }
        );
    }

    @Override
    public void visit(WasmTry expression) {
        if (isSuspending(expression.getBody())) {
            splitTryBody(expression);
        }
        var hasSplitCatches = false;
        for (var catchClause : expression.getCatches()) {
            if (isSuspending(catchClause.getBody())) {
                hasSplitCatches = true;
                break;
            }
        }
        if (hasSplitCatches) {
            var oldStackTypes = stackTypes;
            var current = new WasmBlock(false);
            current.getBody().addAll(resultList);
            resultList.clear();
            resultList.add(current);
            for (var catchClause : expression.getCatches()) {
                if (isSuspending(catchClause.getBody())) {
                    stackTypes = new ArrayList<>();
                    stackTypes.addAll(catchClause.getTag().getType().getReturnTypes());
                    var wrapper = new WasmBlock(false);
                    visitMany(catchClause.getBody());
                    catchClause.getBody().clear();
                    catchClause.getBody().add(new WasmBreak(wrapper));
                    wrapper.getBody().addAll(resultList);
                    resultList.clear();
                    resultList.add(wrapper);
                    current = wrapper;
                }
            }
            current.setType(expression.getType() != null ? expression.getType().asBlock() : null);
            if (!expression.getBody().isEmpty()) {
                var lastBodyIndex = expression.getBody().size() - 1;
                var lastBodyPart = expression.getBody().get(lastBodyIndex);
                if (!lastBodyPart.isTerminating()) {
                    expression.getBody().add(new WasmBreak(current));
                }
            }
            stackTypes = oldStackTypes;
        }
    }

    private boolean isSuspending(List<? extends WasmExpression> expressions) {
        for (var part : expressions) {
            if (collector.isSuspending(part)) {
                return true;
            }
        }
        return false;
    }

    private void splitTryBody(WasmTry expression) {
        var oldResultList = resultList;
        var oldStackTypes = stackTypes;
        stackTypes = new ArrayList<>();
        resultList.add(new WasmSetLocal(stateLocal, new WasmInt32Constant(currentStateOffset)));
        var jumpInsideBlock = new WasmBlock(false);
        jumpInsideBlock.getBody().addAll(resultList);
        resultList.clear();
        resultList.add(jumpInsideBlock);
        resultList.add(expression);
        resultList = new ArrayList<>();
        var oldSwitchContainer = currentSwitchContainer;
        currentSwitchContainer = createSwitchContainer();
        oldSwitchContainer.jumpInsideBlock = jumpInsideBlock;
        addJumpsToOuterSwitches(currentSwitchContainer);
        visitMany(expression.getBody());
        expression.getBody().clear();
        expression.getBody().addAll(resultList);
        resultList = oldResultList;
        currentSwitchContainer = oldSwitchContainer;
        stackTypes = oldStackTypes;
        oldSwitchContainer.jumpInsideBlock = null;
        if (expression.getType() != null) {
            stackTypes.add(expression.getType());
        }
    }

    @Override
    public void visit(WasmThrow expression) {
        visitNAry(expression, e -> new ArrayList<>(e.getArguments()), (e, args) -> {
            e.getArguments().clear();
            e.getArguments().addAll(args);
        });
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        visitBinary(expression, WasmReferencesEqual::getFirst, WasmReferencesEqual::setFirst,
                WasmReferencesEqual::getSecond, WasmReferencesEqual::setSecond);
    }

    @Override
    public void visit(WasmCast expression) {
        visitUnary(expression, WasmCast::getValue, WasmCast::setValue);
    }

    @Override
    public void visit(WasmExternConversion expression) {
        visitUnary(expression, WasmExternConversion::getValue, WasmExternConversion::setValue);
    }

    @Override
    public void visit(WasmTest expression) {
        visitUnary(expression, WasmTest::getValue, WasmTest::setValue);
    }

    @Override
    public void visit(WasmStructNew expression) {
        visitNAry(expression, e -> new ArrayList<>(e.getInitializers()), (e, args) -> {
            e.getInitializers().clear();
            e.getInitializers().addAll(args);
        });
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmStructGet expression) {
        visitUnary(expression, WasmStructGet::getInstance, WasmStructGet::setInstance);
    }

    @Override
    public void visit(WasmStructSet expression) {
        visitBinary(expression, WasmStructSet::getInstance, WasmStructSet::setInstance,
                WasmStructSet::getValue, WasmStructSet::setValue);
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        visitNAry(expression, e -> new ArrayList<>(e.getElements()), (e, args) -> {
            e.getElements().clear();
            e.getElements().addAll(args);
        });
    }

    @Override
    public void visit(WasmArrayGet expression) {
        visitBinary(expression, WasmArrayGet::getInstance, WasmArrayGet::setInstance,
                WasmArrayGet::getIndex, WasmArrayGet::setIndex);
    }

    @Override
    public void visit(WasmArraySet expression) {
        visitNAry(expression,
                e -> {
                    var result = new ArrayList<WasmExpression>();
                    result.add(e.getInstance());
                    result.add(e.getIndex());
                    result.add(e.getValue());
                    return result;
                },
                (e, args) -> {
                    e.setInstance(args.get(0));
                    e.setIndex(args.get(1));
                    e.setValue(args.get(2));
                });
    }

    @Override
    public void visit(WasmArrayLength expression) {
        visitUnary(expression, WasmArrayLength::getInstance, WasmArrayLength::setInstance);
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        visitNAry(expression,
                e -> {
                    var result = new ArrayList<WasmExpression>();
                    result.add(e.getTargetArray());
                    result.add(e.getTargetIndex());
                    result.add(e.getSourceArray());
                    result.add(e.getSourceIndex());
                    result.add(e.getSize());
                    return result;
                },
                (e, args) -> {
                    e.setTargetArray(args.get(0));
                    e.setTargetIndex(args.get(1));
                    e.setSourceArray(args.get(2));
                    e.setSourceIndex(args.get(3));
                    e.setSize(args.get(4));
                });
    }

    @Override
    public void visit(WasmFunctionReference expression) {
        addExpr(expression);
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        visitUnary(expression, WasmInt31Reference::getValue, WasmInt31Reference::setValue);
    }

    @Override
    public void visit(WasmInt31Get expression) {
        visitUnary(expression, WasmInt31Get::getValue, WasmInt31Get::setValue);
    }

    @Override
    public void visit(WasmPush expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(WasmPop expression) {
        throw new UnsupportedOperationException();
    }

    private <T extends WasmExpression> void visitUnary(T expression, Function<T, WasmExpression> getter,
            BiConsumer<T, WasmExpression> setter) {
        var arg = getter.apply(expression);
        if (arg != null && collector.isSuspending(arg)) {
            arg.acceptVisitor(this);
            setter.accept(expression, new WasmPop(peekType(0)));
            popTypes(1);
        }
        addExpr(expression);
    }

    private <T extends WasmExpression> void visitBinary(T expression,
            Function<T, WasmExpression> getter1, BiConsumer<T, WasmExpression> setter1,
            Function<T, WasmExpression> getter2, BiConsumer<T, WasmExpression> setter2) {
        var first = getter1.apply(expression);
        var second = getter2.apply(expression);
        if (first == null) {
            if (collector.isSuspending(second)) {
                second.acceptVisitor(this);
                setter2.accept(expression, new WasmPop(peekType(0)));
                popTypes(1);
            }
        } else {
            if (collector.isSuspending(second)) {
                first.acceptVisitor(this);
                second.acceptVisitor(this);
                setter1.accept(expression, new WasmPop(peekType(1)));
                setter2.accept(expression, new WasmPop(peekType(0)));
                popTypes(2);
            } else if (collector.isSuspending(first)) {
                first.acceptVisitor(this);
                setter1.accept(expression, new WasmPop(peekType(0)));
                popTypes(1);
            }
        }
        addExpr(expression);
    }

    private <T extends WasmExpression> void visitNAry(T expression, Function<T, List<WasmExpression>> getter,
            BiConsumer<T, List<? extends WasmExpression>> setter) {
        var args = getter.apply(expression);
        var last = args.size() - 1;
        while (last > 0) {
            var arg = args.get(last);
            if (arg != null && collector.isSuspending(arg)) {
                break;
            }
            --last;
        }
        var depth = 0;
        for (var i = 0; i <= last; ++i) {
            var arg = args.get(i);
            if (arg != null) {
                if (collector.isSuspending(arg)) {
                    arg.acceptVisitor(this);
                } else {
                    addExpr(arg);
                }
                ++depth;
            }
        }
        var current = depth;
        for (var i = 0; i <= last; ++i) {
            var arg = args.get(i);
            if (arg != null) {
                args.set(i, new WasmPop(peekType(--current)));
            }
        }
        setter.accept(expression, args);
        popTypes(depth);
        addExpr(expression);
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
                    into.add(new WasmInt32Constant(0));
                    break;
                case INT64:
                    into.add(new WasmInt64Constant(0));
                    break;
                case FLOAT32:
                    into.add(new WasmFloat32Constant(0));
                    break;
                case FLOAT64:
                    into.add(new WasmFloat64Constant(0));
                    break;
            }
        } else {
            var ref = (WasmType.Reference) type;
            into.add(new WasmNullConstant(ref));
        }
    }

    private void save(List<WasmExpression> into, WasmType type) {
        into.add(functions.saveValue(type, fiberLocal, new WasmPop(type)));
    }

    private void restore(List<WasmExpression> into, WasmType type) {
        into.add(functions.restoreValue(type, fiberLocal));
    }

    private WasmExpression isSuspending() {
        return new WasmCall(functions.isSuspending(), new WasmGetLocal(fiberLocal));
    }

    private static class SwitchContainer {
        SwitchContainer parent;
        WasmSwitch switchExpr;
        WasmBlock jumpInsideBlock;
    }
}
