/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.model;

import java.util.HashMap;
import java.util.Map;
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
import org.teavm.backend.wasm.model.instruction.WasmArrayCopyInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayLengthInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixedInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArraySetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBlockInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBreakInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditionalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmConversionInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCopyInstruction;
import org.teavm.backend.wasm.model.instruction.WasmDropInstruction;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFillInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt31GetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt31ReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIsNullInstruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmMemoryGrowInstruction;
import org.teavm.backend.wasm.model.instruction.WasmNullBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmNullConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmReferencesEqualInstruction;
import org.teavm.backend.wasm.model.instruction.WasmReturnInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStructGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructSetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSwitchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTestInstruction;
import org.teavm.backend.wasm.model.instruction.WasmThrowInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmUnreachableInstruction;

public class WasmExpressionToInstructionConverter implements WasmExpressionVisitor {
    private WasmInstructionList target;
    private final Map<WasmBlock, WasmInstructionList> blockMapping = new HashMap<>();

    public WasmExpressionToInstructionConverter(WasmInstructionList target) {
        this.target = target;
    }

    public void convert(WasmExpression expression) {
        expression.acceptVisitor(this);
    }

    public void convertAll(Iterable<WasmExpression> expressions) {
        for (var expr : expressions) {
            expr.acceptVisitor(this);
        }
    }

    private void convertInto(WasmInstructionList list, Iterable<WasmExpression> expressions) {
        var savedTarget = target;
        target = list;
        convertAll(expressions);
        target = savedTarget;
    }

    @Override
    public void visit(WasmBlock expression) {
        var block = new WasmBlockInstruction(expression.isLoop());
        block.setType(expression.getType());
        block.setLocation(expression.getLocation());
        blockMapping.put(expression, block.getBody());
        convertInto(block.getBody(), expression.getBody());
        blockMapping.remove(expression);
        target.add(block);
    }

    @Override
    public void visit(WasmBranch expression) {
        if (expression.getResult() != null) {
            convert(expression.getResult());
        }
        convert(expression.getCondition());
        var insn = new WasmBranchInstruction(blockMapping.get(expression.getTarget()));
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmNullBranch expression) {
        if (expression.getResult() != null) {
            convert(expression.getResult());
        }
        convert(expression.getValue());
        var insn = new WasmNullBranchInstruction(expression.getCondition(),
                blockMapping.get(expression.getTarget()));
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmCastBranch expression) {
        if (expression.getResult() != null) {
            convert(expression.getResult());
        }
        convert(expression.getValue());
        var insn = new WasmCastBranchInstruction(expression.getCondition(), expression.getSourceType(),
                expression.getType(), blockMapping.get(expression.getTarget()));
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmBreak expression) {
        if (expression.getResult() != null) {
            convert(expression.getResult());
        }
        var insn = new WasmBreakInstruction(blockMapping.get(expression.getTarget()));
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmSwitch expression) {
        convert(expression.getSelector());
        var insn = new WasmSwitchInstruction(blockMapping.get(expression.getDefaultTarget()));
        insn.setLocation(expression.getLocation());
        for (var target : expression.getTargets()) {
            insn.getTargets().add(blockMapping.get(target));
        }
        target.add(insn);
    }

    @Override
    public void visit(WasmConditional expression) {
        convert(expression.getCondition());
        var conditional = new WasmConditionalInstruction();
        conditional.setType(expression.getType());
        conditional.setLocation(expression.getLocation());
        blockMapping.put(expression.getThenBlock(), conditional.getThenBlock());
        blockMapping.put(expression.getElseBlock(), conditional.getElseBlock());
        convertInto(conditional.getThenBlock(), expression.getThenBlock().getBody());
        convertInto(conditional.getElseBlock(), expression.getElseBlock().getBody());
        blockMapping.remove(expression.getThenBlock());
        blockMapping.remove(expression.getElseBlock());
        target.add(conditional);
    }

    @Override
    public void visit(WasmReturn expression) {
        if (expression.getValue() != null) {
            convert(expression.getValue());
        }
        var insn = new WasmReturnInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmUnreachable expression) {
        var insn = new WasmUnreachableInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        var insn = new WasmInt32ConstantInstruction(expression.getValue());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        var insn = new WasmInt64ConstantInstruction(expression.getValue());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        var insn = new WasmFloat32ConstantInstruction(expression.getValue());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        var insn = new WasmFloat64ConstantInstruction(expression.getValue());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmNullConstant expression) {
        var insn = new WasmNullConstantInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmIsNull expression) {
        convert(expression.getValue());
        var insn = new WasmIsNullInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmGetLocal expression) {
        var insn = new WasmGetLocalInstruction(expression.getLocal());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmSetLocal expression) {
        convert(expression.getValue());
        var insn = new WasmSetLocalInstruction(expression.getLocal());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmGetGlobal expression) {
        var insn = new WasmGetGlobalInstruction(expression.getGlobal());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        convert(expression.getValue());
        var insn = new WasmSetGlobalInstruction(expression.getGlobal());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmIntBinary expression) {
        convert(expression.getFirst());
        convert(expression.getSecond());
        var insn = new WasmIntBinaryInstruction(expression.getType(), expression.getOperation());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        convert(expression.getFirst());
        convert(expression.getSecond());
        var insn = new WasmFloatBinaryInstruction(expression.getType(), expression.getOperation());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmIntUnary expression) {
        convert(expression.getOperand());
        var insn = new WasmIntUnaryInstruction(expression.getType(), expression.getOperation());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        convert(expression.getOperand());
        var insn = new WasmFloatUnaryInstruction(expression.getType(), expression.getOperation());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmConversion expression) {
        convert(expression.getOperand());
        var insn = new WasmConversionInstruction(expression.getSourceType(), expression.getTargetType(),
                expression.isSigned());
        insn.setReinterpret(expression.isReinterpret());
        insn.setNonTrapping(expression.isNonTrapping());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmCall expression) {
        convertAll(expression.getArguments());
        var insn = new WasmCallInstruction(expression.getFunction());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        convertAll(expression.getArguments());
        convert(expression.getSelector());
        var insn = new WasmIndirectCallInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmCallReference expression) {
        convertAll(expression.getArguments());
        convert(expression.getFunctionReference());
        var insn = new WasmCallReferenceInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmDrop expression) {
        convert(expression.getOperand());
        var insn = new WasmDropInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        convert(expression.getIndex());
        var insn = new WasmLoadInt32Instruction(expression.getAlignment(), expression.getConvertFrom());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        convert(expression.getIndex());
        var insn = new WasmLoadInt64Instruction(expression.getAlignment(), expression.getConvertFrom());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        convert(expression.getIndex());
        var insn = new WasmLoadFloat32Instruction(expression.getAlignment());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        convert(expression.getIndex());
        var insn = new WasmLoadFloat64Instruction(expression.getAlignment());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        convert(expression.getIndex());
        convert(expression.getValue());
        var insn = new WasmStoreInt32Instruction(expression.getAlignment(), expression.getConvertTo());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        convert(expression.getIndex());
        convert(expression.getValue());
        var insn = new WasmStoreInt64Instruction(expression.getAlignment(), expression.getConvertTo());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        convert(expression.getIndex());
        convert(expression.getValue());
        var insn = new WasmStoreFloat32Instruction(expression.getAlignment());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        convert(expression.getIndex());
        convert(expression.getValue());
        var insn = new WasmStoreFloat64Instruction(expression.getAlignment());
        insn.setOffset(expression.getOffset());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        convert(expression.getAmount());
        var insn = new WasmMemoryGrowInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFill expression) {
        convert(expression.getIndex());
        convert(expression.getValue());
        convert(expression.getCount());
        var insn = new WasmFillInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmCopy expression) {
        convert(expression.getDestinationIndex());
        convert(expression.getSourceIndex());
        convert(expression.getCount());
        var insn = new WasmCopyInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmTry expression) {
        var tryInsn = new WasmTryInstruction();
        tryInsn.setType(expression.getType());
        tryInsn.setLocation(expression.getLocation());
        convertInto(tryInsn.getBody(), expression.getBody());
        for (var catchClause : expression.getCatches()) {
            var clause = new WasmCatchClause(catchClause.getTag());
            for (var variable : catchClause.getCatchVariables()) {
                clause.add(new WasmSetLocalInstruction(variable));
            }
            convertInto(clause, catchClause.getBody());
            tryInsn.getCatches().add(clause);
        }
        target.add(tryInsn);
    }

    @Override
    public void visit(WasmThrow expression) {
        convertAll(expression.getArguments());
        var insn = new WasmThrowInstruction(expression.getTag());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        convert(expression.getFirst());
        convert(expression.getSecond());
        var insn = new WasmReferencesEqualInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmCast expression) {
        convert(expression.getValue());
        var insn = new WasmCastInstruction(expression.getTargetType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmExternConversion expression) {
        convert(expression.getValue());
        var insn = new WasmExternConversionInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmTest expression) {
        convert(expression.getValue());
        var insn = new WasmTestInstruction(expression.getTestType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStructNew expression) {
        convertAll(expression.getInitializers());
        var insn = new WasmStructNewInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
        var insn = new WasmStructNewDefaultInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStructGet expression) {
        convert(expression.getInstance());
        var insn = new WasmStructGetInstruction(expression.getType(), expression.getFieldIndex());
        insn.setSignedType(expression.getSignedType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmStructSet expression) {
        convert(expression.getInstance());
        convert(expression.getValue());
        var insn = new WasmStructSetInstruction(expression.getType(), expression.getFieldIndex());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        convert(expression.getLength());
        var insn = new WasmArrayNewDefaultInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        convertAll(expression.getElements());
        var insn = new WasmArrayNewFixedInstruction(expression.getType(), expression.getElements().size());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArrayGet expression) {
        convert(expression.getInstance());
        convert(expression.getIndex());
        var insn = new WasmArrayGetInstruction(expression.getType());
        insn.setSignedType(expression.getSignedType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArraySet expression) {
        convert(expression.getInstance());
        convert(expression.getIndex());
        convert(expression.getValue());
        var insn = new WasmArraySetInstruction(expression.getType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArrayLength expression) {
        convert(expression.getInstance());
        var insn = new WasmArrayLengthInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        convert(expression.getTargetArray());
        convert(expression.getTargetIndex());
        convert(expression.getSourceArray());
        convert(expression.getSourceIndex());
        convert(expression.getSize());
        var insn = new WasmArrayCopyInstruction(expression.getTargetArrayType(), expression.getSourceArrayType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmFunctionReference expression) {
        var insn = new WasmFunctionReferenceInstruction(expression.getFunction());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        convert(expression.getValue());
        var insn = new WasmInt31ReferenceInstruction();
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmInt31Get expression) {
        convert(expression.getValue());
        var insn = new WasmInt31GetInstruction(expression.getSignedType());
        insn.setLocation(expression.getLocation());
        target.add(insn);
    }

    @Override
    public void visit(WasmPush expression) {
        convert(expression.getArgument());
    }

    @Override
    public void visit(WasmPop expression) {
        // WasmPop leaves nothing on the stack - its counterpart WasmPush already emitted it
    }
}
