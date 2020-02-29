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
package org.teavm.backend.wasm.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.TextLocation;

class WasmCRenderingVisitor implements WasmExpressionVisitor {
    private CExpression value;
    private Map<WasmBlock, BlockInfo> blockInfoMap = new HashMap<>();
    private WasmType requiredType;
    private int temporaryIndex;
    private int blockIndex;
    private WasmType functionType;
    private WasmModule module;
    private String[] localVariableNames;
    private Set<String> usedVariableNames = new HashSet<>();
    private boolean memoryAccessChecked;

    WasmCRenderingVisitor(WasmType functionType, int variableCount, WasmModule module) {
        localVariableNames = new String[variableCount];
        this.functionType = functionType;
        this.module = module;
    }

    public boolean isMemoryAccessChecked() {
        return memoryAccessChecked;
    }

    public void setMemoryAccessChecked(boolean memoryAccessChecked) {
        this.memoryAccessChecked = memoryAccessChecked;
    }

    public CExpression getValue() {
        return value;
    }

    void setRequiredType(WasmType requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    public void visit(WasmBlock expression) {
        BlockInfo info = new BlockInfo();
        info.type = requiredType;
        info.index = blockInfoMap.size();
        blockInfoMap.put(expression, info);

        List<WasmExpression> body = expression.getBody();
        CExpression result = new CExpression();

        if (!body.isEmpty()) {
            for (int i = 0; i < body.size() - 1; ++i) {
                requiredType = null;
                body.get(i).acceptVisitor(this);
                result.getLines().addAll(value.getLines());
            }

            requiredType = info.type;
            WasmExpression last = body.get(body.size() - 1);
            last.acceptVisitor(this);
            result.getLines().addAll(value.getLines());
            if (info.type != null) {
                if (info.temporaryVariable != null) {
                    result.getLines().add(new CSingleLine(info.temporaryVariable + " = " + value.getText() + ";"));
                    result.setText(info.temporaryVariable);
                } else {
                    result.setText(value.getText());
                }
            }

            if (info.temporaryVariable != null) {
                result.getLines().add(0, declareVariable(info.temporaryVariable, info.type));
            }

            if (expression.isLoop()) {
                List<CLine> lines = new ArrayList<>();
                lines.add(new CSingleLine(getLabel(info) + ": do {"));
                lines.add(new CBlock(result.getLines()));
                lines.add(new CSingleLine("} while(0);"));
                result.getLines().clear();
                result.getLines().addAll(lines);
            } else if (info.label != null) {
                result.getLines().add(new CSingleLine(info.label + ": ;"));
            }
        }

        blockInfoMap.remove(expression);
        value = result;
    }

    @Override
    public void visit(WasmBranch expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getCondition().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        result.addLine("if (" + value.getText() + ") {", expression.getLocation());
        CBlock breakBlock = new CBlock(generateBreak(expression.getResult(), expression.getTarget(),
                expression.getLocation()));
        result.getLines().add(breakBlock);
        result.getLines().add(new CSingleLine("}"));

        value = result;
    }

    @Override
    public void visit(WasmBreak expression) {
        CExpression result = new CExpression();
        result.getLines().addAll(generateBreak(expression.getResult(), expression.getTarget(),
                expression.getLocation()));
        value = result;
    }

    private List<CLine> generateBreak(WasmExpression result, WasmBlock target, TextLocation location) {
        List<CLine> lines = new ArrayList<>();
        BlockInfo targetInfo = blockInfoMap.get(target);

        if (result != null) {
            if (targetInfo.temporaryVariable == null) {
                targetInfo.temporaryVariable = "tmp_" + temporaryIndex++;
            }
            requiredType = targetInfo.type;
            result.acceptVisitor(this);
            lines.addAll(value.getLines());
            lines.add(new CSingleLine(targetInfo.temporaryVariable + " = " + value.getText() + ";",
                    result.getLocation()));
        }
        lines.add(new CSingleLine("goto " + getLabel(targetInfo) + ";", location));

        return lines;
    }

    private String getLabel(BlockInfo blockInfo) {
        if (blockInfo.label == null) {
            blockInfo.label = "block_" + blockIndex++;
        }
        return blockInfo.label;
    }

    @Override
    public void visit(WasmSwitch expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getSelector().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        result.addLine("switch (" + value.getText() + ") {", expression.getLocation());

        CBlock switchBlock = new CBlock();
        result.getLines().add(switchBlock);
        for (int i = 0; i < expression.getTargets().size(); ++i) {
            BlockInfo targetInfo = blockInfoMap.get(expression.getTargets().get(i));
            switchBlock.getLines().add(new CSingleLine("case " + i + ": goto " + getLabel(targetInfo) + ";"));
        }

        BlockInfo defaultTargetInfo = blockInfoMap.get(expression.getDefaultTarget());
        switchBlock.getLines().add(new CSingleLine("default: goto " + getLabel(defaultTargetInfo) + ";"));

        result.getLines().add(new CSingleLine("}"));

        value = result;
    }

    @Override
    public void visit(WasmConditional expression) {
        WasmType type = requiredType;

        requiredType = WasmType.INT32;
        expression.getCondition().acceptVisitor(this);
        CExpression condition = value;

        requiredType = type;
        expression.getThenBlock().acceptVisitor(this);
        CExpression thenExpression = value;

        requiredType = type;
        expression.getElseBlock().acceptVisitor(this);
        CExpression elseExpression = value;

        CExpression result = new CExpression();
        result.getLines().addAll(condition.getLines());

        if (type != null && thenExpression.getLines().isEmpty() && elseExpression.getLines().isEmpty()
                && elseExpression.getText() != null) {
            result.setText("(" + condition.getText() + " ? " + thenExpression.getText()
                    + " : " + elseExpression.getText() + ")");
        } else {
            String temporary = null;
            if (type != null) {
                temporary = "tmp_" + temporaryIndex++;
                result.setText(temporary);
                result.getLines().add(declareVariable(temporary, type));
            }
            result.getLines().add(new CSingleLine("if (" + condition.getText() + ") {"));

            CBlock thenBlock = new CBlock();
            thenBlock.getLines().addAll(thenExpression.getLines());
            if (temporary != null) {
                thenBlock.getLines().add(new CSingleLine(temporary + " = " + thenExpression.getText() + ";"));
            }
            result.getLines().add(thenBlock);

            if (elseExpression.getText() != null || !elseExpression.getLines().isEmpty()) {
                result.getLines().add(new CSingleLine("} else {"));
                CBlock elseBlock = new CBlock();
                elseBlock.getLines().addAll(elseExpression.getLines());
                if (temporary != null) {
                    elseBlock.getLines().add(new CSingleLine(temporary + " = " + elseExpression.getText() + ";"));
                }
                result.getLines().add(elseBlock);
            }

            result.getLines().add(new CSingleLine("}"));
        }

        value = result;
    }

    @Override
    public void visit(WasmReturn expression) {
        CExpression result = new CExpression();
        if (expression.getValue() != null) {
            requiredType = functionType;
            expression.getValue().acceptVisitor(this);
            result.getLines().addAll(value.getLines());
            result.addLine("return " + value.getText() + ";", expression.getLocation());
        } else {
            result.addLine("return;", expression.getLocation());
        }

        value = result;
    }

    @Override
    public void visit(WasmUnreachable expression) {
        CExpression result = new CExpression();
        result.addLine("assert(0);", expression.getLocation());
        value = result;
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        value = CExpression.relocatable("INT32_C(" + String.valueOf(expression.getValue()) + ")");
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        value = CExpression.relocatable("INT64_C(" + String.valueOf(expression.getValue()) + ")");
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        if (Float.isInfinite(expression.getValue())) {
            value = CExpression.relocatable(expression.getValue() < 0 ? "-INFINITY" : "INFINITY");
        } else if (Float.isNaN(expression.getValue())) {
            value = CExpression.relocatable("NAN");
        } else {
            value = CExpression.relocatable(Float.toHexString(expression.getValue()) + "F");
        }
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        if (Double.isInfinite(expression.getValue())) {
            value = CExpression.relocatable(expression.getValue() < 0 ? "-INFINITY" : "INFINITY");
        } else if (Double.isNaN(expression.getValue())) {
            value = CExpression.relocatable("NAN");
        } else {
            value = CExpression.relocatable(Double.toHexString(expression.getValue()));
        }
    }

    @Override
    public void visit(WasmGetLocal expression) {
        value = new CExpression(getVariableName(expression.getLocal()));
    }

    @Override
    public void visit(WasmSetLocal expression) {
        CExpression result = new CExpression();
        requiredType = expression.getLocal().getType();
        expression.getValue().acceptVisitor(this);
        result.getLines().addAll(value.getLines());

        result.addLine(getVariableName(expression.getLocal()) + " = " + value.getText() + ";",
                expression.getLocation());

        value = result;
    }

    @Override
    public void visit(WasmIntBinary expression) {
        WasmType type = requiredType;
        WasmType opType = asWasmType(expression.getType());
        CExpression result = new CExpression();

        requiredType = opType;
        expression.getFirst().acceptVisitor(this);
        CExpression first = value;

        requiredType = opType;
        expression.getSecond().acceptVisitor(this);
        CExpression second = value;

        if (type == null) {
            result.getLines().addAll(first.getLines());
            result.getLines().addAll(second.getLines());
        } else {
            result.getLines().addAll(first.getLines());
            if (!second.getLines().isEmpty()) {
                first = cacheIfNeeded(opType, first, result);
                result.getLines().addAll(second.getLines());
            }

            String firstOp = first.getText();
            String secondOp = second.getText();
            String typeText = mapType(opType);
            String unsignedType = "u" + typeText;
            String firstOpUnsigned = "(" + unsignedType + ") " + firstOp;
            String secondOpUnsigned = "(" + unsignedType + ") " + secondOp;
            switch (expression.getOperation()) {
                case ADD:
                    result.setText("(" + firstOp + " + " + secondOp + ")");
                    break;
                case SUB:
                    result.setText("(" + firstOp + " - " + secondOp + ")");
                    break;
                case MUL:
                    result.setText("(" + firstOp + " * " + secondOp + ")");
                    break;
                case DIV_SIGNED:
                    result.setText("(" + firstOp + " / " + secondOp + ")");
                    break;
                case DIV_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " / " + secondOpUnsigned + ")");
                    break;
                case REM_SIGNED:
                    result.setText("(" + firstOp + " % " + secondOp + ")");
                    break;
                case REM_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " % " + secondOpUnsigned + ")");
                    break;
                case AND:
                    result.setText("(" + firstOp + " & " + secondOp + ")");
                    break;
                case OR:
                    result.setText("(" + firstOp + " | " + secondOp + ")");
                    break;
                case XOR:
                    result.setText("(" + firstOp + " ^ " + secondOp + ")");
                    break;
                case SHL:
                    result.setText("(" + firstOp + " << " + secondOp + ")");
                    break;
                case SHR_SIGNED:
                    result.setText("(" + firstOp + " >> " + secondOp + ")");
                    break;
                case SHR_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " >> " + secondOp + ")");
                    break;
                case EQ:
                    result.setText("(" + firstOp + " == " + secondOp + ")");
                    break;
                case NE:
                    result.setText("(" + firstOp + " != " + secondOp + ")");
                    break;
                case GT_SIGNED:
                    result.setText("(" + firstOp + " > " + secondOp + ")");
                    break;
                case GT_UNSIGNED:
                    result.setText("(" + firstOpUnsigned + " > " + secondOpUnsigned + ")");
                    break;
                case GE_SIGNED:
                    result.setText("(" + firstOp + " >= " + secondOp + ")");
                    break;
                case GE_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " >= " + secondOpUnsigned + ")");
                    break;
                case LT_SIGNED:
                    result.setText("(" + firstOp + " < " + secondOp + ")");
                    break;
                case LT_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " < " + secondOpUnsigned + ")");
                    break;
                case LE_SIGNED:
                    result.setText("(" + firstOp + " <= " + secondOp + ")");
                    break;
                case LE_UNSIGNED:
                    result.setText("(" + typeText + ") (" + firstOpUnsigned + " <= " + secondOpUnsigned + ")");
                    break;
                case ROTL:
                    result.setText("rotl(" + firstOp + ", " + secondOp + ")");
                    break;
                case ROTR:
                    result.setText("rotr(" + firstOp + ", " + secondOp + ")");
                    break;
            }
            result.setRelocatable(first.isRelocatable() && second.isRelocatable());
        }

        value = result;
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        WasmType type = requiredType;
        WasmType opType = asWasmType(expression.getType());
        CExpression result = new CExpression();

        requiredType = opType;
        expression.getFirst().acceptVisitor(this);
        CExpression first = value;

        requiredType = opType;
        expression.getSecond().acceptVisitor(this);
        CExpression second = value;

        if (type == null) {
            result.getLines().addAll(first.getLines());
            result.getLines().addAll(second.getLines());
        } else {
            result.getLines().addAll(first.getLines());
            if (!second.getLines().isEmpty()) {
                first = cacheIfNeeded(opType, first, result);
                result.getLines().addAll(second.getLines());
            }

            String firstOp = first.getText();
            String secondOp = second.getText();

            switch (expression.getOperation()) {
                case ADD:
                    result.setText("(" + firstOp + " + " + secondOp + ")");
                    break;
                case SUB:
                    result.setText("(" + firstOp + " - " + secondOp + ")");
                    break;
                case MUL:
                    result.setText("(" + firstOp + " * " + secondOp + ")");
                    break;
                case DIV:
                    result.setText("(" + firstOp + " / " + secondOp + ")");
                    break;
                case EQ:
                    result.setText("(" + firstOp + " == " + secondOp + ")");
                    break;
                case NE:
                    result.setText("(" + firstOp + " != " + secondOp + ")");
                    break;
                case GT:
                    result.setText("(" + firstOp + " > " + secondOp + ")");
                    break;
                case GE:
                    result.setText("(" + firstOp + " >= " + secondOp + ")");
                    break;
                case LT:
                    result.setText("(" + firstOp + " < " + secondOp + ")");
                    break;
                case LE:
                    result.setText("(" + firstOp + " <= " + secondOp + ")");
                    break;
                case MIN: {
                    String function = expression.getType() == WasmFloatType.FLOAT32 ? "fminf" : "fmin";
                    result.setText(function + "(" + firstOp + ", " + secondOp + ")");
                    break;
                }
                case MAX: {
                    String function = expression.getType() == WasmFloatType.FLOAT32 ? "fmaxf" : "fmax";
                    result.setText(function + "(" + firstOp + ", " + secondOp + ")");
                    break;
                }
            }
            result.setRelocatable(first.isRelocatable() && second.isRelocatable());
        }

        value = result;
    }

    private CExpression cacheIfNeeded(WasmType type, CExpression expression, CExpression target) {
        if (expression.isRelocatable()) {
            return expression;
        }
        String var = "tmp_" + temporaryIndex++;
        target.getLines().add(new CSingleLine(mapType(type) + " " + var + " = " + expression.getText() + ";"));
        return CExpression.relocatable(var);
    }

    @Override
    public void visit(WasmIntUnary expression) {
        WasmType type = requiredType;
        WasmType opType = asWasmType(expression.getType());
        CExpression result = new CExpression();

        requiredType = opType;
        expression.getOperand().acceptVisitor(this);
        CExpression operand = value;

        result.getLines().addAll(operand.getLines());
        if (type != null) {
            switch (expression.getOperation()) {
                case POPCNT:
                    result.setText("popcnt(" + operand.getText() + ")");
                    break;
                case CLZ:
                    result.setText("clz(" + operand.getText() + ")");
                    break;
                case CTZ:
                    result.setText("ctz(" + operand.getText() + ")");
                    break;
            }
            result.setRelocatable(operand.isRelocatable());
        }

        value = result;
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        WasmType type = requiredType;
        WasmType opType = asWasmType(expression.getType());
        CExpression result = new CExpression();

        requiredType = opType;
        expression.getOperand().acceptVisitor(this);
        CExpression operand = value;

        result.getLines().addAll(operand.getLines());
        if (type != null) {
            switch (expression.getOperation()) {
                case ABS: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "fabsf" : "fabs";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case CEIL: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "ceilf" : "ceil";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case FLOOR: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "floorf" : "floor";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case TRUNC: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "truncf" : "trunc";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case NEAREST: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "roundf" : "round";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case SQRT: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "sqrtf" : "sqrt";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
                case NEG:
                    result.setText("(-" + operand.getText() + ")");
                    break;
                case COPYSIGN: {
                    String functionName = expression.getType() == WasmFloatType.FLOAT32 ? "copysignf" : "copysign";
                    result.setText(functionName + "(" + operand.getText() + ")");
                    break;
                }
            }
            result.setRelocatable(operand.isRelocatable());
        }

        value = result;
    }

    @Override
    public void visit(WasmConversion expression) {
        CExpression result = new CExpression();

        WasmType type = requiredType;
        expression.getOperand().acceptVisitor(this);
        CExpression operand = value;

        result.getLines().addAll(operand.getLines());
        if (type != null && expression.getSourceType() != expression.getTargetType()) {
            switch (expression.getTargetType()) {
                case INT32:
                    if (expression.getSourceType() == WasmType.FLOAT32 && expression.isReinterpret()) {
                        result.setText("reinterpret_float32(" + operand.getText() + ")");
                    } else if (expression.isSigned()) {
                        result.setText("(int32_t) " + operand.getText());
                    } else {
                        result.setText("(uint32_t) " + operand.getText());
                    }
                    break;
                case INT64:
                    if (expression.getSourceType() == WasmType.FLOAT64 && expression.isReinterpret()) {
                        result.setText("reinterpret_float64(" + operand.getText() + ")");
                    } else if (expression.isSigned()) {
                        result.setText("(int64_t) " + operand.getText());
                    } else {
                        result.setText("(uint64_t) " + operand.getText());
                    }
                    break;
                case FLOAT32:
                    if (expression.getSourceType() == WasmType.INT32 && expression.isReinterpret()) {
                        result.setText("reinterpret_int32(" + operand.getText() + ")");
                    } else if (expression.getSourceType() == WasmType.FLOAT64) {
                        result.setText("(float) " + operand.getText());
                    } else if (expression.isSigned()) {
                        result.setText("(float) (int64_t) " + operand.getText());
                    } else {
                        result.setText("(float) (uint64_t) " + operand.getText());
                    }
                    break;
                case FLOAT64:
                    if (expression.getSourceType() == WasmType.INT64 && expression.isReinterpret()) {
                        result.setText("reinterpret_int64(" + operand.getText() + ")");
                    } else if (expression.getSourceType() == WasmType.FLOAT32) {
                        result.setText("(double) " + operand.getText());
                    } else if (expression.isSigned()) {
                        result.setText("(double) (int64_t) " + operand.getText());
                    } else {
                        result.setText("(double) (uint64_t) " + operand.getText());
                    }
                    break;
            }
        }

        value = result;
    }

    @Override
    public void visit(WasmCall expression) {
        WasmFunction function = module.getFunctions().get(expression.getFunctionName());
        if (function == null) {
            value = new CExpression("0");
            return;
        }

        CExpression result = new CExpression();
        WasmType type = requiredType;

        StringBuilder sb = new StringBuilder();
        if (expression.isImported()) {
            sb.append(!function.getImportModule().isEmpty()
                    ? function.getImportModule() + "_" + function.getImportName()
                    : function.getImportName());
        } else {
            sb.append(expression.getFunctionName());
        }
        sb.append('(');
        translateArguments(expression.getArguments(), function.getParameters(), result, sb);
        sb.append(')');
        result.setText(sb.toString());

        if (type == null) {
            result.addLine(result.getText() + ";", expression.getLocation());
            result.setText(null);
        }
        value = result;
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        CExpression result = new CExpression();
        WasmType type = requiredType;
        StringBuilder sb = new StringBuilder();

        sb.append("(*(" + mapType(expression.getReturnType()) + " (*)(");
        for (int i = 0; i < expression.getParameterTypes().size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(mapType(expression.getParameterTypes().get(i)));
        }
        sb.append(")) ");

        requiredType = WasmType.INT32;
        expression.getSelector().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        value = cacheIfNeeded(WasmType.INT32, value, result);
        sb.append("wasm_table[" + value.getText() + "])(");
        translateArguments(expression.getArguments(), expression.getParameterTypes(), result, sb);
        sb.append(")");
        result.setText(sb.toString());

        if (type == null) {
            result.addLine(result.getText() + ";", expression.getLocation());
            result.setText(null);
        }
        value = result;
    }

    private void translateArguments(List<WasmExpression> wasmArguments, List<WasmType> signature,
            CExpression result, StringBuilder sb) {
        if (wasmArguments.isEmpty()) {
            return;
        }
        List<CExpression> arguments = new ArrayList<>();
        int needsCachingUntil = 0;
        for (int i = wasmArguments.size() - 1; i >= 0; --i) {
            requiredType = signature.get(i);
            wasmArguments.get(i).acceptVisitor(this);
            arguments.add(value);
            if (!value.getLines().isEmpty() && needsCachingUntil == 0) {
                needsCachingUntil = i;
            }
        }
        Collections.reverse(arguments);

        for (int i = 0; i < arguments.size(); ++i) {
            CExpression argument = arguments.get(i);
            result.getLines().addAll(argument.getLines());
            if (i < needsCachingUntil) {
                argument = cacheIfNeeded(signature.get(i), argument, result);
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(argument.getText());
        }

        value = result;
    }

    @Override
    public void visit(WasmDrop expression) {
        CExpression result = new CExpression();
        requiredType = null;
        expression.getOperand().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        value = result;
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        CExpression result = new CExpression();
        WasmType type = requiredType;

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);
        if (type == null) {
            value = index;
            return;
        }

        result.getLines().addAll(index.getLines());
        String base = "&wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]";
        switch (expression.getConvertFrom()) {
            case INT8:
                result.setText("(int32_t) (int8_t) " + base.substring(1));
                break;
            case UINT8:
                result.setText("(int32_t) (uint8_t) " + base.substring(1));
                break;
            case INT16:
                result.setText("(int32_t) *((int16_t *) " + base + ")");
                break;
            case UINT16:
                result.setText("(int32_t) *((uint16_t *) " + base + ")");
                break;
            case INT32:
                result.setText("*((int32_t *) " + base + ")");
                break;
        }

        value = result;
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        CExpression result = new CExpression();
        WasmType type = requiredType;

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);
        if (type == null) {
            value = index;
            return;
        }

        result.getLines().addAll(index.getLines());
        String base = "&wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]";
        switch (expression.getConvertFrom()) {
            case INT8:
                result.setText("(int64_t) (int8_t) " + base.substring(1));
                break;
            case UINT8:
                result.setText("(int64_t) (uint8_t) " + base.substring(1));
                break;
            case INT16:
                result.setText("(int64_t) *((int16_t *) " + base + ")");
                break;
            case UINT16:
                result.setText("(int64_t) *((uint16_t *) " + base + ")");
                break;
            case INT32:
                result.setText("(int64_t) *((int32_t *) " + base + ")");
                break;
            case UINT32:
                result.setText("(int64_t) *((uint32_t *) " + base + ")");
                break;
            case INT64:
                result.setText("*((int64_t *) " + base + ")");
                break;
        }

        value = result;
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        CExpression result = new CExpression();
        WasmType type = requiredType;

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);
        if (type == null) {
            value = index;
            return;
        }

        result.getLines().addAll(index.getLines());
        result.setText("*((float *) &wasm_heap[" + index.getText() + " + " + expression.getOffset() + "])");

        value = result;
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        CExpression result = new CExpression();
        WasmType type = requiredType;

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);
        if (type == null) {
            value = index;
            return;
        }

        result.getLines().addAll(index.getLines());
        result.setText("*((double *) &wasm_heap[" + index.getText() + " + " + expression.getOffset() + "])");

        value = result;
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);

        requiredType = WasmType.INT32;
        expression.getValue().acceptVisitor(this);
        CExpression valueToStore = value;

        result.getLines().addAll(index.getLines());
        result.getLines().addAll(valueToStore.getLines());

        String line;
        String base = "&wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]";
        switch (expression.getConvertTo()) {
            case INT8:
                line = base.substring(1) + " = " + valueToStore.getText() + ";";
                break;
            case UINT8:
                line = "*((uint8_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case INT16:
                line = "*((int16_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case UINT16:
                line = "*((uint16_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case INT32:
                line = "*((int32_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            default:
                throw new AssertionError(expression.getConvertTo().toString());
        }
        result.addLine(line, expression.getLocation());

        value = result;
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);

        requiredType = WasmType.INT64;
        expression.getValue().acceptVisitor(this);
        CExpression valueToStore = value;

        result.getLines().addAll(index.getLines());
        result.getLines().addAll(valueToStore.getLines());

        String line;
        String base = "&wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]";
        switch (expression.getConvertTo()) {
            case INT8:
                line = base.substring(1) + " = " + valueToStore.getText();
                break;
            case UINT8:
                line = "*((uint8_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case INT16:
                line = "*((int16_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case UINT16:
                line = "*((uint16_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case INT32:
                line = "*((int32_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case UINT32:
                line = "*((uint32_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            case INT64:
                line = "*((int64_t *) " + base + ") = " + valueToStore.getText() + ";";
                break;
            default:
                throw new AssertionError();
        }

        result.addLine(line, expression.getLocation());

        value = result;
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);

        requiredType = WasmType.FLOAT32;
        expression.getValue().acceptVisitor(this);
        CExpression valueToStore = value;

        result.getLines().addAll(index.getLines());
        result.getLines().addAll(valueToStore.getLines());

        result.addLine("*((float *) &wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]) = "
                + valueToStore.getText() + ";", expression.getLocation());

        value = result;
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getIndex().acceptVisitor(this);
        CExpression index = checkAddress(value);

        requiredType = WasmType.FLOAT64;
        expression.getValue().acceptVisitor(this);
        CExpression valueToStore = value;

        result.getLines().addAll(index.getLines());
        result.getLines().addAll(valueToStore.getLines());

        result.addLine("*((double *) &wasm_heap[" + index.getText() + " + " + expression.getOffset() + "]) = "
                + valueToStore.getText() + ";", expression.getLocation());

        value = result;
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getAmount().acceptVisitor(this);
        CExpression amount = value;

        if (amount.getLines().isEmpty()) {
            result.addLine("wasm_heap_size += 65536 * (" + amount.getText() + ");", expression.getLocation());
        } else {
            result.addLine("wasm_heap_size += 65536 * (" + amount.getText(), expression.getLocation());
            result.getLines().addAll(amount.getLines());
            result.addLine(");", expression.getLocation());
        }

        result.addLine("wasm_heap = realloc(wasm_heap, wasm_heap_size);");
        value = result;
    }

    private CExpression checkAddress(CExpression index) {
        if (!memoryAccessChecked) {
            return index;
        }

        CExpression checked = new CExpression();
        checked.getLines().addAll(index.getLines());
        String var;
        if (!index.isRelocatable()) {
            var = "tmp_" + temporaryIndex++;
            checked.addLine("int32_t " + var + " = " + index.getText() + ";");
        } else {
            var = index.getText();
        }
        checked.addLine("assert(" + var + " < " + module.getMinMemorySize() * 65536 + ");");
        checked.setText(var);
        checked.setRelocatable(index.isRelocatable());

        return checked;
    }

    private CLine declareVariable(String name, WasmType type) {
        return new CSingleLine(mapType(type) + " " + name + ";");
    }

    static String mapType(WasmType type) {
        if (type == null) {
            return "void";
        }
        switch (type) {
            case INT32:
                return "int32_t";
            case INT64:
                return "int64_t";
            case FLOAT32:
                return "float";
            case FLOAT64:
                return "double";
        }
        throw new AssertionError(type.toString());
    }

    private static WasmType asWasmType(WasmIntType type) {
        switch (type) {
            case INT32:
                return WasmType.INT32;
            case INT64:
                return WasmType.INT64;
        }
        throw new AssertionError(type.toString());
    }

    private static WasmType asWasmType(WasmFloatType type) {
        switch (type) {
            case FLOAT32:
                return WasmType.FLOAT32;
            case FLOAT64:
                return WasmType.FLOAT64;
        }
        throw new AssertionError(type.toString());
    }

    private static class BlockInfo {
        int index;
        String label;
        String temporaryVariable;
        WasmType type;
    }

    String getVariableName(WasmLocal local) {
        String result = localVariableNames[local.getIndex()];
        if (result == null) {
            if (local.getName() != null) {
                result = "_" + local.getName();
            } else {
                result = "localVar" + local.getIndex();
            }
            if (!usedVariableNames.add(result)) {
                String base = result;
                int suffix = 1;
                while (!usedVariableNames.add(base + suffix)) {
                    ++suffix;
                }
                result = base + suffix;
            }
            localVariableNames[local.getIndex()] = result;
        }
        return result;
    }
}
