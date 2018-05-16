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
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
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

class WasmRenderingVisitor implements WasmExpressionVisitor {
    StringBuilder sb = new StringBuilder();
    private Map<WasmBlock, String> blockIdentifiers = new HashMap<>();
    private int indentLevel;
    private boolean lfDeferred;
    boolean lineNumbersEmitted;
    List<WasmSignature> signatureList = new ArrayList<>();
    private Map<WasmSignature, Integer> signatureMap = new HashMap<>();

    void preprocess(WasmExpression expression) {
        expression.acceptVisitor(new WasmDefaultExpressionVisitor() {
            @Override
            public void visit(WasmBranch expression) {
                super.visit(expression);
                register(expression.getTarget());
            }

            @Override
            public void visit(WasmBreak expression) {
                super.visit(expression);
                register(expression.getTarget());
            }

            @Override
            public void visit(WasmSwitch expression) {
                super.visit(expression);
                for (WasmBlock target : expression.getTargets()) {
                    register(target);
                }
                register(expression.getDefaultTarget());
            }

            private void register(WasmBlock block) {
                blockIdentifiers.computeIfAbsent(block, key -> "block_" + blockIdentifiers.size());
            }
        });
    }

    void clear() {
        blockIdentifiers.clear();
    }

    WasmRenderingVisitor append(String text) {
        if (lfDeferred) {
            lfDeferred = false;
            sb.append("\n");
            for (int i = 0; i < indentLevel; ++i) {
                sb.append("  ");
            }
        }
        sb.append(text);
        return this;
    }

    WasmRenderingVisitor append(WasmType type) {
        return append(type(type));
    }

    private WasmRenderingVisitor append(WasmExpression expression) {
        expression.acceptVisitor(this);
        return this;
    }

    WasmRenderingVisitor line(WasmExpression expression) {
        if (expression.getLocation() != null && lineNumbersEmitted) {
            lf().append(";; " + expression.getLocation().getFileName() + ":" + expression.getLocation().getLine());
        }
        lf().append(expression);
        return this;
    }

    WasmRenderingVisitor indent() {
        indentLevel++;
        return this;
    }

    WasmRenderingVisitor outdent() {
        indentLevel--;
        return this;
    }

    WasmRenderingVisitor lf() {
        if (lfDeferred) {
            sb.append("\n");
        }
        lfDeferred = true;
        return this;
    }

    WasmRenderingVisitor open() {
        append("(").indent();
        return this;
    }

    WasmRenderingVisitor close() {
        outdent().append(")");
        return this;
    }

    @Override
    public void visit(WasmBlock expression) {
        renderBlock(expression, expression.isLoop() ? "loop" : "block", true);
    }

    private void renderBlock(WasmBlock block, String name, boolean signature) {
        open().append(name);
        String id = blockIdentifiers.get(block);
        if (id != null) {
            append(" $" + id);
        }
        if (signature && block.getType() != null) {
            append(" " + type(block.getType()));
        }
        for (WasmExpression part : block.getBody()) {
            line(part);
        }
        close();
    }

    @Override
    public void visit(WasmBranch expression) {
        String id = blockIdentifiers.get(expression.getTarget());
        open().append("br_if $" + id);
        if (expression.getResult() != null) {
            line(expression.getResult());
        }
        line(expression.getCondition());
        close();
    }

    @Override
    public void visit(WasmBreak expression) {
        String id = blockIdentifiers.get(expression.getTarget());
        open().append("br $").append(id);
        if (expression.getResult() != null) {
            line(expression.getResult());
        }
        close();
    }

    @Override
    public void visit(WasmSwitch expression) {
        open().append("br_table ");
        for (WasmBlock target : expression.getTargets()) {
            append("$" + blockIdentifiers.get(target)).append(" ");
        }
        append("$" + blockIdentifiers.get(expression.getDefaultTarget()));
        line(expression.getSelector());
        close();
    }

    @Override
    public void visit(WasmConditional expression) {
        open().append("if");

        if (expression.getType() != null) {
            append(" " + type(expression.getType()));
        }

        line(expression.getCondition());

        lf();
        renderBlock(expression.getThenBlock(), "then", false);

        if (!expression.getElseBlock().getBody().isEmpty()) {
            lf();
            renderBlock(expression.getElseBlock(), "else", false);
        }

        close();
    }

    @Override
    public void visit(WasmReturn expression) {
        open().append("return");
        if (expression.getValue() != null) {
            line(expression.getValue());
        }
        close();
    }

    @Override
    public void visit(WasmUnreachable expression) {
        open().append("unreachable").close();
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        open().append("i32.const " + expression.getValue()).close();
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        open().append("i64.const " + expression.getValue()).close();
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        open().append("f32.const " + Float.toHexString(expression.getValue())).close();
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        open().append("f64.const " + Double.toHexString(expression.getValue())).close();
    }

    @Override
    public void visit(WasmGetLocal expression) {
        open().append("get_local " + asString(expression.getLocal())).close();
    }

    @Override
    public void visit(WasmSetLocal expression) {
        open().append("set_local " + asString(expression.getLocal())).line(expression.getValue()).close();
    }

    private String asString(WasmLocal local) {
        return String.valueOf(local.getIndex());
    }

    @Override
    public void visit(WasmIntBinary expression) {
        open().append(type(expression.getType()) + "." + operation(expression.getOperation()));
        line(expression.getFirst());
        line(expression.getSecond());
        close();
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        open().append(type(expression.getType()) + "." + operation(expression.getOperation()));
        line(expression.getFirst());
        line(expression.getSecond());
        close();
    }

    @Override
    public void visit(WasmIntUnary expression) {
        open().append(type(expression.getType()) + "." + operation(expression.getOperation()));
        line(expression.getOperand());
        close();
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        open().append(type(expression.getType()) + "." + operation(expression.getOperation()));
        line(expression.getOperand());
        close();
    }

    @Override
    public void visit(WasmConversion expression) {
        String name = null;
        switch (expression.getSourceType()) {
            case INT32:
                switch (expression.getTargetType()) {
                    case INT32:
                        break;
                    case INT64:
                        name = expression.isSigned() ? "extend_s" : "extend_u";
                        break;
                    case FLOAT32:
                    case FLOAT64:
                        if (expression.isReinterpret()) {
                            name = "reinterpret";
                        } else {
                            name = expression.isSigned() ? "convert_s" : "convert_u";
                        }
                        break;
                }
                break;
            case INT64:
                switch (expression.getTargetType()) {
                    case INT32:
                        name = "wrap";
                        break;
                    case INT64:
                        break;
                    case FLOAT32:
                    case FLOAT64:
                        if (expression.isReinterpret()) {
                            name = "reinterpret";
                        } else {
                            name = expression.isSigned() ? "convert_s" : "convert_u";
                        }
                        break;
                }
                break;
            case FLOAT32:
                switch (expression.getTargetType()) {
                    case INT32:
                    case INT64:
                        if (expression.isReinterpret()) {
                            name = "reinterpret";
                        } else {
                            name = expression.isSigned() ? "trunc_s" : "trunc_u";
                        }
                        break;
                    case FLOAT32:
                        break;
                    case FLOAT64:
                        name = "promote";
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getTargetType()) {
                    case INT32:
                    case INT64:
                        if (expression.isReinterpret()) {
                            name = "reinterpret";
                        } else {
                            name = expression.isSigned() ? "trunc_s" : "trunc_u";
                        }
                        break;
                    case FLOAT32:
                        name = "demote";
                        break;
                    case FLOAT64:
                        break;
                }
                break;
        }

        if (name == null) {
            append(expression.getOperand());
        } else {
            open().append(type(expression.getTargetType()) + "." + name + "/" + type(expression.getSourceType()));
            line(expression.getOperand());
            close();
        }
    }

    @Override
    public void visit(WasmCall expression) {
        open().append("call").append(" $" + expression.getFunctionName());
        for (WasmExpression argument : expression.getArguments()) {
            line(argument);
        }
        close();
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        WasmType[] types = new WasmType[expression.getParameterTypes().size() + 1];
        types[0] = expression.getReturnType();
        for (int i = 0; i < expression.getParameterTypes().size(); ++i) {
            types[i + 1] = expression.getParameterTypes().get(i);
        }

        open().append("call_indirect").append(" $type" + getSignatureIndex(new WasmSignature(types)));
        line(expression.getSelector());
        for (WasmExpression argument : expression.getArguments()) {
            line(argument);
        }
        close();
    }

    int getSignatureIndex(WasmSignature signature) {
        return signatureMap.computeIfAbsent(signature, key -> {
            signatureList.add(key);
            return signatureMap.size();
        });
    }

    @Override
    public void visit(WasmDrop expression) {
        open().append("drop").lf();
        append(expression.getOperand());
        close();
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        open();
        switch (expression.getConvertFrom()) {
            case INT8:
                append("i32.load8_s");
                break;
            case UINT8:
                append("i32.load8_u");
                break;
            case INT16:
                append("i32.load16_s");
                break;
            case UINT16:
                append("i32.load16_u");
                break;
            case INT32:
                append("i32.load");
                break;
        }
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        close();
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        open();
        switch (expression.getConvertFrom()) {
            case INT8:
                append("i64.load8_s");
                break;
            case UINT8:
                append("i64.load8_u");
                break;
            case INT16:
                append("i64.load16_s");
                break;
            case UINT16:
                append("i64.load16_u");
                break;
            case INT32:
                append("i64.load32_s");
                break;
            case UINT32:
                append("i64.load32_u");
                break;
            case INT64:
                append("i64.load");
                break;
        }
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        close();
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        open().append("f32.load");
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        close();
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        open().append("f64.load");
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        close();
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        open();
        switch (expression.getConvertTo()) {
            case INT8:
            case UINT8:
                append("i32.store8");
                break;
            case INT16:
            case UINT16:
                append("i32.store16");
                break;
            case INT32:
                append("i32.store");
                break;
        }
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        line(expression.getValue());
        close();
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        open();
        switch (expression.getConvertTo()) {
            case INT8:
            case UINT8:
                append("i64.store8");
                break;
            case INT16:
            case UINT16:
                append("i64.store16");
                break;
            case INT32:
            case UINT32:
                append("i64.store32");
                break;
            case INT64:
                append("i64.store");
                break;
        }
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        line(expression.getValue());
        close();
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        open().append("f32.store");
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        line(expression.getValue());
        close();
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        open().append("f64.store");
        if (expression.getOffset() > 0) {
            append(" offset=" + expression.getOffset());
        }
        append(" align=" + expression.getAlignment());
        line(expression.getIndex());
        line(expression.getValue());
        close();
    }

    private String type(WasmType type) {
        switch (type) {
            case INT32:
                return "i32";
            case INT64:
                return "i64";
            case FLOAT32:
                return "f32";
            case FLOAT64:
                return "f64";
        }
        throw new AssertionError(type.toString());
    }

    private String type(WasmIntType type) {
        switch (type) {
            case INT32:
                return "i32";
            case INT64:
                return "i64";
        }
        throw new AssertionError(type.toString());
    }

    private String type(WasmFloatType type) {
        switch (type) {
            case FLOAT32:
                return "f32";
            case FLOAT64:
                return "f64";
        }
        throw new AssertionError(type.toString());
    }

    private String operation(WasmIntBinaryOperation operation) {
        switch (operation) {
            case ADD:
                return "add";
            case SUB:
                return "sub";
            case MUL:
                return "mul";
            case DIV_SIGNED:
                return "div_s";
            case DIV_UNSIGNED:
                return "div_u";
            case REM_SIGNED:
                return "rem_s";
            case REM_UNSIGNED:
                return "rem_u";
            case AND:
                return "and";
            case OR:
                return "or";
            case XOR:
                return "xor";
            case EQ:
                return "eq";
            case NE:
                return "ne";
            case GT_SIGNED:
                return "gt_s";
            case GT_UNSIGNED:
                return "gt_u";
            case GE_SIGNED:
                return "ge_s";
            case GE_UNSIGNED:
                return "ge_u";
            case LT_SIGNED:
                return "lt_s";
            case LT_UNSIGNED:
                return "lt_u";
            case LE_SIGNED:
                return "le_s";
            case LE_UNSIGNED:
                return "le_u";
            case SHL:
                return "shl";
            case SHR_SIGNED:
                return "shr_s";
            case SHR_UNSIGNED:
                return "shr_u";
            case ROTL:
                return "rotl";
            case ROTR:
                return "rotr";
        }
        throw new AssertionError(operation.toString());
    }

    private String operation(WasmIntUnaryOperation operation) {
        switch (operation) {
            case CLZ:
                return "clz";
            case CTZ:
                return "ctz";
            case POPCNT:
                return "popcnt";
        }
        throw new AssertionError(operation.toString());
    }

    private String operation(WasmFloatBinaryOperation operation) {
        switch (operation) {
            case ADD:
                return "add";
            case SUB:
                return "sub";
            case MUL:
                return "mul";
            case DIV:
                return "div";
            case EQ:
                return "eq";
            case NE:
                return "ne";
            case GT:
                return "gt";
            case GE:
                return "ge";
            case LT:
                return "lt";
            case LE:
                return "le";
            case MIN:
                return "min";
            case MAX:
                return "max";
        }
        throw new AssertionError(operation.toString());
    }

    private String operation(WasmFloatUnaryOperation operation) {
        switch (operation) {
            case ABS:
                return "abs";
            case NEG:
                return "neg";
            case COPYSIGN:
                break;
            case CEIL:
                return "ceil";
            case FLOOR:
                return "floor";
            case TRUNC:
                return "trunc";
            case NEAREST:
                return "nearest";
            case SQRT:
                return "sqrt";
        }
        throw new AssertionError(operation.toString());
    }
}
