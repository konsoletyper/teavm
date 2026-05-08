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
package org.teavm.backend.wasm.transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlockInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBreakInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastInstruction;
import org.teavm.backend.wasm.model.instruction.WasmConditionalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmDropInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmNullConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmReturnInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSwitchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTypeInference;
import org.teavm.backend.wasm.render.WasmSignature;
import org.teavm.model.TextLocation;

public class CoroutineTransformation {
    private static final String FIBER = "org.teavm.runtime.Fiber";
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private CoroutineFunctions coroutineFunctions;
    private WasmFunction currentFunction;
    private WasmLocal savedFunctionLocal;
    private WasmLocal fiberLocal;
    private WasmLocal stateLocal;
    private SuspensionPointCollector collector;
    private int currentStateOffset;

    public CoroutineTransformation(WasmFunctionTypes functionTypes, BaseWasmFunctionRepository functions,
            WasmGCClassInfoProvider classInfoProvider) {
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        coroutineFunctions = new CoroutineFunctions(functions);
    }

    public void transform(WasmFunction function) {
        currentFunction = function;
        collector = new SuspensionPointCollector();
        collector.visitMany(function.getBody());
        var originalLocals = function.getLocalVariables().size();
        fiberLocal = new WasmLocal(classInfoProvider.getClassInfo(FIBER).getType(), "coroutine$fiber");
        stateLocal = new WasmLocal(WasmType.INT32, "coroutine$state");
        function.add(fiberLocal);
        function.add(stateLocal);

        var mainBlock = new WasmBlockInstruction(false);
        mainBlock.getBody().transferFrom(function.getBody());

        generatePrologue(originalLocals);
        function.getBody().add(mainBlock);
        splitList(mainBlock.getBody(), Collections.emptyList(), function.getType().getReturnTypes(),
                mainBlock.getBody());
        if (!mainBlock.getBody().getLast().isTerminating()) {
            mainBlock.getBody().add(new WasmReturnInstruction());
        }
        generateEpilogue(originalLocals);

        currentFunction = null;
        savedFunctionLocal = null;
        collector = null;
        currentStateOffset = 0;
        stateLocal = null;
        fiberLocal = null;
    }

    private void generatePrologue(int localsCount) {
        var prologue = new WasmInstructionList();
        var builder = prologue.builder();
        builder
                .call(coroutineFunctions.currentFiber())
                .teeLocal(fiberLocal)
                .call(coroutineFunctions.isResuming());
        var restoreCond = builder.conditional(WasmType.INT32);
        restoreCond.getThenBlock().builder()
                .getLocal(fiberLocal)
                .call(coroutineFunctions.popInt());
        for (var i = localsCount - 1; i >= 0; i--) {
            var local = currentFunction.getLocalVariables().get(i);
            restoreCond.getThenBlock().add(new WasmGetLocalInstruction(fiberLocal));
            coroutineFunctions.restoreValue(local.getType(), restoreCond.getThenBlock(), null);
            restoreCond.getThenBlock().add(new WasmSetLocalInstruction(local));
        }

        restoreCond.getElseBlock().builder()
                .i32Const(0);
        builder.setLocal(stateLocal);

        currentFunction.getBody().addFirst(prologue);
    }

    private void generateEpilogue(int localsCount) {
        var list = new WasmInstructionList();
        for (var i = 0; i < localsCount; i++) {
            var local = currentFunction.getLocalVariables().get(i);
            list.add(new WasmGetLocalInstruction(local));
            coroutineFunctions.saveValue(local.getType(), list, fiberLocal, null);
        }
        list.add(new WasmGetLocalInstruction(stateLocal));
        list.add(new WasmGetLocalInstruction(fiberLocal));
        list.add(new WasmCallInstruction(coroutineFunctions.pushInt()));

        var returnType = currentFunction.getType().getSingleReturnType();
        if (returnType != null) {
            if (returnType instanceof WasmType.Number) {
                switch (((WasmType.Number) returnType).number) {
                    case INT32:
                        list.add(new WasmInt32ConstantInstruction(0));
                        break;
                    case INT64:
                        list.add(new WasmInt64ConstantInstruction(0));
                        break;
                    case FLOAT32:
                        list.add(new WasmFloat32ConstantInstruction(0));
                        break;
                    case FLOAT64:
                        list.add(new WasmFloat64ConstantInstruction(0));
                        break;
                }
            } else {
                list.add(new WasmNullConstantInstruction((WasmType.Reference) returnType));
            }
        }

        currentFunction.getBody().transferFrom(list);
    }

    private void splitList(WasmInstructionList list, List<? extends WasmType> inputTypes,
            List<? extends WasmType> outputTypes, WasmInstructionList suspendLabel) {
        var splitter = new ListSplitter(inputTypes, outputTypes, suspendLabel);
        var first = list.getFirst();
        splitter.addPrologue(list);
        splitter.process(first);
    }

    private class ListSplitter {
        private List<? extends WasmType> inputTypes;
        private List<? extends WasmType> outputTypes;
        private WasmInstructionList suspendLabel;
        private WasmTypeInference typeInference;
        private int minDepth;
        private List<WasmType> stackSnapshot = new ArrayList<>();
        private WasmSwitchInstruction switchInsn;

        ListSplitter(List<? extends WasmType> inputTypes, List<? extends WasmType> outputTypes,
                WasmInstructionList suspendLabel) {
            this.inputTypes = inputTypes;
            this.outputTypes = outputTypes;
            this.suspendLabel = suspendLabel;
            typeInference = new WasmTypeInference();
            typeInference.typeStack.addAll(inputTypes);
            minDepth = inputTypes.size();
        }

        void process(WasmInstruction insn) {
            while (insn != null) {
                var next = insn.getNext();
                if (collector.isSuspending(insn)) {
                    var block = new WasmBlockInstruction(false);
                    var jumpToInsn = block;
                    if (!inputTypes.isEmpty()) {
                        var signature = new WasmSignature(Collections.emptyList(), mapToNullableTypes(inputTypes));
                        block.setType(functionTypes.get(signature).asBlock());
                    }
                    moveAllPreviousTo(insn, block.getBody());
                    var stateIndex = ++currentStateOffset;
                    block.getBody().add(new WasmInt32ConstantInstruction(stateIndex), insn.getLocation());
                    block.getBody().add(new WasmSetLocalInstruction(stateLocal), insn.getLocation());

                    minDepth = Math.min(minDepth, typeInference.typeStack.size());
                    stackSnapshot.clear();
                    stackSnapshot.addAll(typeInference.typeStack);
                    WasmType functionType = null;
                    if (insn instanceof WasmCallReferenceInstruction) {
                        functionType = ((WasmCallReferenceInstruction) insn).getType().getReference();
                    }
                    updateTypes(insn);
                    if (minDepth != inputTypes.size() || inputTypes.size() != stackSnapshot.size()) {
                        block = createRestoreInstructions(block, insn.getLocation(), functionType != null);
                    }

                    insn.insertPrevious(block);
                    insn.insertNext(createSaveInstructions(functionType, insn.getLocation()));
                    switchInsn.getTargets().add(jumpToInsn.getBody());
                    handleSplitInstruction(insn, stateIndex);
                    for (var i = stateIndex; i < currentStateOffset; ++i) {
                        switchInsn.getTargets().add(jumpToInsn.getBody());
                    }
                } else {
                    updateTypes(insn);
                }
                insn = next;
            }
        }

        private void updateTypes(WasmInstruction insn) {
            insn.acceptVisitor(typeInference);
            minDepth = Math.min(minDepth, typeInference.getDepthBeforeLastInstructionOut());
        }

        void addPrologue(WasmInstructionList target) {
            var jumpToFirstLabel = new WasmBlockInstruction(false);
            var jumpToUnreachable = new WasmBlockInstruction(false);
            jumpToFirstLabel.getBody().builder()
                    .add(jumpToUnreachable)
                    .unreachable();

            var jumpToUnreachableBuilder = jumpToUnreachable.getBody().builder();
            jumpToUnreachableBuilder.getLocal(stateLocal);
            if (currentStateOffset > 0) {
                jumpToUnreachableBuilder
                        .i32Const(currentStateOffset)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
            }
            switchInsn = new WasmSwitchInstruction(jumpToUnreachable.getBody());
            switchInsn.getTargets().add(jumpToFirstLabel.getBody());
            jumpToUnreachableBuilder.add(switchInsn);
            target.addFirst(jumpToFirstLabel);
        }

        private WasmBlockInstruction createRestoreInstructions(WasmBlockInstruction block, TextLocation location,
                boolean isCallRef) {
            var depthWithoutArgs = typeInference.getDepthBeforeLastInstructionOut();
            var restoreBlock = new WasmBlockInstruction(false);
            var signature = new WasmSignature(mapToNullableTypes(stackSnapshot), mapToNullableTypes(inputTypes));
            restoreBlock.setType(functionTypes.get(signature).asBlock());
            restoreBlock.setLocation(location);

            restoreBlock.getBody().add(block);
            block.getBody().add(new WasmBreakInstruction(restoreBlock.getBody()), location);
            var typesToPush = stackSnapshot.subList(0, depthWithoutArgs);
            for (var type : typesToPush) {
                restoreBlock.getBody().add(new WasmGetLocalInstruction(fiberLocal), location);
                coroutineFunctions.restoreValue(type, restoreBlock.getBody(), location);
            }

            var dummyArgs = stackSnapshot.size();
            if (isCallRef) {
                --dummyArgs;
            }
            for (var i = depthWithoutArgs; i < dummyArgs; ++i) {
                var type = stackSnapshot.get(i);
                pushDefault(restoreBlock.getBody(), type, location);
            }
            if (isCallRef) {
                var type = stackSnapshot.get(dummyArgs);
                restoreBlock.getBody().add(new WasmGetLocalInstruction(fiberLocal), location);
                coroutineFunctions.restoreValue(type, restoreBlock.getBody(), location);
            }
            return restoreBlock;
        }

        private WasmInstructionList createSaveInstructions(WasmType callRefType, TextLocation location) {
            var depthWithoutArgs = typeInference.getDepthBeforeLastInstructionOut();
            var result = new WasmInstructionList();
            emitIsSuspending(result, location);
            var check = new WasmConditionalInstruction();
            result.add(check, location);

            if (callRefType != null) {
                check.getThenBlock().add(new WasmGetLocalInstruction(savedFunctionLocal()));
                coroutineFunctions.saveValue(callRefType, check.getThenBlock(), fiberLocal, location);
            }
            var condTypes = mapToNullableTypes(typeInference.typeStack.subList(minDepth,
                    typeInference.typeStack.size()));
            if (!condTypes.isEmpty()) {
                check.setType(functionTypes.get(new WasmSignature(condTypes, condTypes)).asBlock());
            }
            for (var i = typeInference.typeStack.size() - 1; i >= depthWithoutArgs; --i) {
                check.getThenBlock().add(new WasmDropInstruction(), location);
            }
            for (var i = depthWithoutArgs - 1; i >= minDepth; --i) {
                var type = stackSnapshot.get(i);
                coroutineFunctions.saveValue(type, check.getThenBlock(), fiberLocal, location);
            }
            for (var i = 0; i < outputTypes.size(); ++i) {
                pushDefault(check.getThenBlock(), outputTypes.get(i), location);
            }
            check.getThenBlock().add(new WasmBreakInstruction(suspendLabel), location);
            return result;
        }
    }

    private List<WasmType> mapToNullableTypes(List<? extends WasmType> types) {
        var result = new ArrayList<WasmType>();
        for (var type : types) {
            if (type instanceof WasmType.Reference) {
                type = ((WasmType.Reference) type).asNullable();
            }
            result.add(type);
        }
        return result;
    }

    private void moveAllPreviousTo(WasmInstruction instruction, WasmInstructionList list) {
        instruction = instruction.getPrevious();
        while (instruction != null) {
            var previous = instruction.getPrevious();
            instruction.delete();
            list.addFirst(instruction);
            instruction = previous;
        }
    }

    private void handleSplitInstruction(WasmInstruction instruction, int stateIndex) {
        if (instruction instanceof WasmCallReferenceInstruction) {
            var callRef = (WasmCallReferenceInstruction) instruction;
            instruction.insertPrevious(new WasmTeeLocalInstruction(savedFunctionLocal()));
            instruction.insertPrevious(new WasmCastInstruction(callRef.getType().getReference()));
        } else if (instruction instanceof WasmBlockInstruction) {
            var block = (WasmBlockInstruction) instruction;
            if (block.isLoop()) {
                splitLoop(block, stateIndex);
            } else {
                List<? extends WasmType> inputTypes = block.getType() != null
                        ? block.getType().getInputTypes()
                        : Collections.emptyList();
                List<? extends WasmType> outputTypes = block.getType() != null
                        ? block.getType().getOutputTypes()
                        : Collections.emptyList();
                splitList(block.getBody(), inputTypes, outputTypes, block.getBody());
            }
        } else if (instruction instanceof WasmConditionalInstruction) {
            splitConditional((WasmConditionalInstruction) instruction);
        } else if (instruction instanceof WasmTryInstruction) {
            var tryInsn = (WasmTryInstruction) instruction;
            List<? extends WasmType> outTypes = tryInsn.getType() != null
                    ? List.of(tryInsn.getType())
                    : Collections.emptyList();
            splitList(tryInsn.getBody(), Collections.emptyList(), outTypes, tryInsn.getBody());
        }
    }

    private void splitLoop(WasmBlockInstruction block, int stateIndex) {
        var breakLabel = new WasmBlockInstruction(false);
        var newLoop = new WasmBlockInstruction(true);
        breakLabel.getBody().add(newLoop);

        block.insertPrevious(breakLabel);
        block.delete();
        block.setLoop(false);
        newLoop.getBody().add(block);
        newLoop.getBody().add(new WasmInt32ConstantInstruction(stateIndex));
        newLoop.getBody().add(new WasmSetLocalInstruction(stateLocal));
        newLoop.getBody().add(new WasmBreakInstruction(newLoop.getBody()));

        splitList(newLoop.getBody(), Collections.emptyList(), Collections.emptyList(), breakLabel.getBody());
    }

    private void splitConditional(WasmConditionalInstruction conditional) {
        var inputTypes = new ArrayList<WasmType>();
        var outputTypes = new ArrayList<WasmType>();
        if (conditional.getType() != null) {
            inputTypes.addAll(conditional.getType().getInputTypes());
            outputTypes.addAll(conditional.getType().getOutputTypes());
        }
        inputTypes.add(WasmType.INT32);

        var wrapper = new WasmBlockInstruction(false);
        wrapper.setType(functionTypes.get(new WasmSignature(outputTypes, inputTypes)).asBlock());
        conditional.insertPrevious(wrapper);
        if (conditional.getElseBlock().isEmpty()) {
            wrapper.getBody().add(new WasmIntUnaryInstruction(WasmIntType.INT32, WasmIntUnaryOperation.EQZ));
            wrapper.getBody().add(new WasmBranchInstruction(conditional.getThenBlock()));
            wrapper.getBody().transferFrom(conditional.getThenBlock());
            splitList(wrapper.getBody(), inputTypes, outputTypes, wrapper.getBody());
            var replacement = new BreakTargetReplacement(target -> {
                if (target == conditional.getThenBlock()) {
                    return wrapper.getBody();
                } else {
                    return null;
                }
            });
            replacement.visit(wrapper);
        } else {
            var thenWrapper = new WasmBlockInstruction(false);
            thenWrapper.setType(functionTypes.get(new WasmSignature(Collections.emptyList(), inputTypes)).asBlock());
            thenWrapper.getBody().add(new WasmIntUnaryInstruction(WasmIntType.INT32, WasmIntUnaryOperation.EQZ));
            thenWrapper.getBody().add(new WasmBranchInstruction(conditional.getThenBlock()));
            thenWrapper.getBody().transferFrom(conditional.getThenBlock());
            var splitter = new ListSplitter(inputTypes, outputTypes, wrapper.getBody());
            var first = thenWrapper.getBody().getFirst();
            splitter.addPrologue(thenWrapper.getBody());
            splitter.process(first);
            if (!thenWrapper.getBody().getLast().isTerminating()) {
                thenWrapper.getBody().add(new WasmBreakInstruction(wrapper.getBody()));
            }
            wrapper.getBody().add(thenWrapper);
            wrapper.getBody().transferFrom(conditional.getElseBlock());
            splitter.typeInference.typeStack.clear();
            if (conditional.getType() != null) {
                splitter.typeInference.typeStack.addAll(conditional.getType().getInputTypes());
            }
            splitter.process(wrapper.getBody().getFirst().getNext());

            var replacement = new BreakTargetReplacement(target -> {
                if (target == conditional.getThenBlock()) {
                    return thenWrapper.getBody();
                } else if (target == conditional.getElseBlock()) {
                    return wrapper.getBody();
                } else {
                    return null;
                }
            });
            replacement.visit(wrapper);
        }
        conditional.delete();
    }

    private WasmLocal savedFunctionLocal() {
        if (savedFunctionLocal == null) {
            savedFunctionLocal = new WasmLocal(WasmType.FUNC, "coroutine$savedFunction");
            currentFunction.add(savedFunctionLocal);
        }
        return savedFunctionLocal;
    }

    private void pushDefault(WasmInstructionList list, WasmType type, TextLocation location) {
        if (type instanceof WasmType.Number) {
            switch (((WasmType.Number) type).number) {
                case INT32:
                    list.add(new WasmInt32ConstantInstruction(0), location);
                    break;
                case INT64:
                    list.add(new WasmInt64ConstantInstruction(0L), location);
                    break;
                case FLOAT32:
                    list.add(new WasmFloat32ConstantInstruction(0), location);
                    break;
                case FLOAT64:
                    list.add(new WasmFloat64ConstantInstruction(0), location);
                    break;
            }
        } else {
            var ref = (WasmType.Reference) type;
            list.add(new WasmNullConstantInstruction(ref), location);
        }
    }

    private void emitIsSuspending(WasmInstructionList list, TextLocation location) {
        list.add(new WasmGetLocalInstruction(fiberLocal), location);
        list.add(new WasmCallInstruction(coroutineFunctions.isSuspending()), location);
    }
}
