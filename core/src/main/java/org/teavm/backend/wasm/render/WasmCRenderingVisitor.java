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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public WasmCRenderingVisitor(WasmType functionType) {
        this.functionType = functionType;
    }

    public CExpression getValue() {
        return value;
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
                    result.getLines().add(new CSingleLine(info.temporaryVariable + " = " + result.getText()));
                    result.setText(info.temporaryVariable);
                } else {
                    result.setText(value.getText());
                }
            }

            if (info.temporaryVariable != null) {
                result.getLines().add(0, declareVariable(info.temporaryVariable, info.type));
            }

            if (info.label != null || expression.isLoop()) {
                List<CLine> lines = new ArrayList<>();
                String header = expression.isLoop() ? "for(;;) " : "";
                lines.add(new CSingleLine(info.label + ": " + header + "{"));
                lines.add(new CBlock(result.getLines()));
                lines.add(new CSingleLine("}"));
                result.getLines().clear();
                result.getLines().addAll(lines);
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
        reportLocation(expression.getLocation(), result.getLines());
        result.getLines().add(new CSingleLine("if (" + value.getText() + ") {"));
        CBlock breakBlock = new CBlock(generateBreak(expression.getResult(), expression.getTarget()));
        result.getLines().add(breakBlock);
        result.getLines().add(new CSingleLine("}"));

        value = result;
    }

    @Override
    public void visit(WasmBreak expression) {
        CExpression result = new CExpression();
        result.getLines().addAll(generateBreak(expression.getResult(), expression.getTarget()));
        value = result;
    }

    private List<CLine> generateBreak(WasmExpression result, WasmBlock target) {
        List<CLine> lines = new ArrayList<>();
        BlockInfo targetInfo = blockInfoMap.get(target);

        if (result != null) {
            if (targetInfo.temporaryVariable == null) {
                targetInfo.temporaryVariable = "tmp_" + temporaryIndex++;
            }
            if (targetInfo.label == null) {
                targetInfo.label = "block_" + blockIndex++;
            }
            requiredType = targetInfo.type;
            result.acceptVisitor(this);
            lines.addAll(value.getLines());
            reportLocation(result.getLocation(), lines);
            lines.add(new CSingleLine(targetInfo.temporaryVariable + " = " + value.getText() + ";"));
        }
        reportLocation(result.getLocation(), lines);
        if (target.isLoop()) {
            lines.add(new CSingleLine("break " + targetInfo.label + ";"));
        } else {
            lines.add(new CSingleLine("continue " + targetInfo.label + ";"));
        }

        return lines;
    }

    @Override
    public void visit(WasmSwitch expression) {
        CExpression result = new CExpression();

        requiredType = WasmType.INT32;
        expression.getSelector().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        reportLocation(expression.getLocation(), result.getLines());
        result.getLines().add(new CSingleLine("switch (" + value.getLines() + ") {"));

        CBlock switchBlock = new CBlock();
        result.getLines().add(switchBlock);
        for (int i = 0; i < expression.getTargets().size(); ++i) {
            BlockInfo targetInfo = blockInfoMap.get(expression.getTargets().get(i));
            if (targetInfo.label == null) {
                targetInfo.label = "block_" + blockIndex++;
            }
            switchBlock.getLines().add(new CSingleLine("case " + i + ": break " + targetInfo.label + ";"));
        }

        BlockInfo defaultTargetInfo = blockInfoMap.get(expression.getDefaultTarget());
        if (defaultTargetInfo.label == null) {
            defaultTargetInfo.label = "block_" + blockIndex++;
        }
        switchBlock.getLines().add(new CSingleLine("default: break " + defaultTargetInfo.label + ";"));

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
                thenBlock.getLines().add(new CSingleLine(temporary + " = " + thenExpression.getText()));
            }
            result.getLines().add(thenBlock);

            if (!elseExpression.getText().isEmpty() || elseExpression.getText() != null) {
                result.getLines().add(new CSingleLine("} else {"));
                CBlock elseBlock = new CBlock();
                elseBlock.getLines().addAll(elseBlock.getLines());
                if (temporary != null) {
                    elseBlock.getLines().add(new CSingleLine(temporary + " = " + elseExpression.getText()));
                }
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
            reportLocation(expression.getLocation(), result.getLines());
            result.getLines().add(new CSingleLine("return " + value.getText() + ";"));
        } else {
            reportLocation(expression.getLocation(), result.getLines());
            result.getLines().add(new CSingleLine("return;"));
        }

        value = result;
    }

    @Override
    public void visit(WasmUnreachable expression) {
        CExpression result = new CExpression();
        reportLocation(expression.getLocation(), result.getLines());
        result.getLines().add(new CSingleLine("assert(false);"));
        value = result;
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        value = new CExpression("INT32_C(" + String.valueOf(expression.getValue()) + ")");
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        value = new CExpression("INT64_C(" + String.valueOf(expression.getValue()) + ")");
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        value = new CExpression(Float.toHexString(expression.getValue()) + "F");
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        value = new CExpression(Double.toHexString(expression.getValue()));
    }

    @Override
    public void visit(WasmGetLocal expression) {
        value = new CExpression("var_" + expression.getLocal().getIndex());
    }

    @Override
    public void visit(WasmSetLocal expression) {
        CExpression result = new CExpression();
        requiredType = expression.getLocal().getType();
        expression.getValue().acceptVisitor(this);
        result.getLines().addAll(value.getLines());

        reportLocation(expression.getLocation(), result.getLines());

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
            String firstOp = first.getText();
            String secondOp = second.getText();
            result.getLines().addAll(first.getLines());
            if (!second.getLines().isEmpty()) {
                firstOp = "tmp_" + temporaryIndex;
                result.getLines().add(new CSingleLine(mapType(opType) + " " + firstOp + " = "
                        + first.getText() + ";"));
                result.getLines().addAll(second.getLines());
            }

            String unsingedType = "u" + mapType(opType);
            String firstOpUnsinged = "(" + unsingedType + ") " + firstOp;
            String secondOpUnsigned = "(" + unsingedType + ") " + secondOp;
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
                    result.setText("(" + firstOpUnsinged + " / " + secondOpUnsigned + ")");
                    break;
                case REM_SIGNED:
                    result.setText("(" + firstOp + " % " + secondOp + ")");
                    break;
                case REM_UNSIGNED:
                    result.setText("(" + firstOpUnsinged + " % " + secondOpUnsigned + ")");
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
                    result.setText("(" + firstOpUnsinged + " >> " + secondOp + ")");
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
                    result.setText("(" + firstOpUnsinged + " > " + secondOpUnsigned + ")");
                    break;
                case GE_SIGNED:
                    result.setText("(" + firstOp + " >= " + secondOp + ")");
                    break;
                case GE_UNSIGNED:
                    result.setText("(" + firstOpUnsinged + " >= " + secondOpUnsigned + ")");
                    break;
                case LT_SIGNED:
                    result.setText("(" + firstOp + " < " + secondOp + ")");
                    break;
                case LT_UNSIGNED:
                    result.setText("(" + firstOpUnsinged + " < " + secondOpUnsigned + ")");
                    break;
                case LE_SIGNED:
                    result.setText("(" + firstOp + " <= " + secondOp + ")");
                    break;
                case LE_UNSIGNED:
                    result.setText("(" + firstOpUnsinged + " <= " + secondOpUnsigned + ")");
                    break;
                case ROTL:
                    result.setText("rotl(" + firstOp + ", " + secondOp + ")");
                    break;
                case ROTR:
                    result.setText("rotr(" + firstOp + ", " + secondOp + ")");
                    break;
            }
        }

        value = result;
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
        CExpression result = new CExpression();

        StringBuilder sb = new StringBuilder();
        sb.append(expression.getFunctionName()).append('(');
        sb.append(')');
        result.setText(sb.toString());

        if (requiredType == null) {
            reportLocation(expression.getLocation(), result.getLines());
            result.getLines().add(new CSingleLine(result.getText()));
            result.setText(null);
        }
        value = result;
    }

    @Override
    public void visit(WasmIndirectCall expression) {

    }

    @Override
    public void visit(WasmDrop expression) {
        CExpression result = new CExpression();
        requiredType = null;
        reportLocation(expression.getLocation(), result.getLines());
        expression.getOperand().acceptVisitor(this);
        result.getLines().addAll(value.getLines());
        value = result;
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

    private CLine declareVariable(String name, WasmType type) {
        return new CSingleLine(mapType(type) + " " + name + ";");
    }

    private void reportLocation(TextLocation location, List<CLine> lines) {
        if (location != null) {
            lines.add(new CSingleLine("#line " + location.getFileName() + " " + location.getLine()));
        }
    }

    private static String mapType(WasmType type) {
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

    static class BlockInfo {
        int index;
        String label;
        String temporaryVariable;
        WasmType type;
    }
}
