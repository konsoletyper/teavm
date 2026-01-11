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
package org.teavm.backend.wasm.model.expression;

import java.util.List;

public class WasmDefaultExpressionVisitor implements WasmExpressionVisitor {
    @Override
    public void visit(WasmBlock expression) {
        visitMany(expression.getBody());
    }

    @Override
    public void visit(WasmBranch expression) {
        visitDefault(expression.getCondition());
        if (expression.getResult() != null) {
            visitDefault(expression.getResult());
        }
    }

    @Override
    public void visit(WasmNullBranch expression) {
        visitDefault(expression.getValue());
        if (expression.getResult() != null) {
            visitDefault(expression.getResult());
        }
    }

    @Override
    public void visit(WasmCastBranch expression) {
        visitDefault(expression.getValue());
        if (expression.getResult() != null) {
            visitDefault(expression.getResult());
        }
    }

    @Override
    public void visit(WasmBreak expression) {
        if (expression.getResult() != null) {
            visitDefault(expression.getResult());
        }
    }

    @Override
    public void visit(WasmSwitch expression) {
        visitDefault(expression.getSelector());
    }

    @Override
    public void visit(WasmConditional expression) {
        visitDefault(expression.getCondition());
        visitMany(expression.getThenBlock().getBody());
        visitMany(expression.getElseBlock().getBody());
    }

    @Override
    public void visit(WasmReturn expression) {
        if (expression.getValue() != null) {
            visitDefault(expression.getValue());
        }
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
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmGetLocal expression) {
    }

    @Override
    public void visit(WasmSetLocal expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmGetGlobal expression) {
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmIntBinary expression) {
        visitDefault(expression.getFirst());
        visitDefault(expression.getSecond());
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        visitDefault(expression.getFirst());
        visitDefault(expression.getSecond());
    }

    @Override
    public void visit(WasmIntUnary expression) {
        visitDefault(expression.getOperand());
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        visitDefault(expression.getOperand());
    }

    @Override
    public void visit(WasmConversion expression) {
        visitDefault(expression.getOperand());
    }

    @Override
    public void visit(WasmCall expression) {
        visitMany(expression.getArguments());
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        visitDefault(expression.getSelector());
        visitMany(expression.getArguments());
    }

    @Override
    public void visit(WasmCallReference expression) {
        visitDefault(expression.getFunctionReference());
        visitMany(expression.getArguments());
    }

    @Override
    public void visit(WasmDrop expression) {
        visitDefault(expression.getOperand());
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        visitDefault(expression.getIndex());
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        visitDefault(expression.getIndex());
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        visitDefault(expression.getIndex());
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        visitDefault(expression.getIndex());
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        visitDefault(expression.getAmount());
    }

    @Override
    public void visit(WasmFill expression) {
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
        visitDefault(expression.getCount());
    }

    @Override
    public void visit(WasmCopy expression) {
        visitDefault(expression.getDestinationIndex());
        visitDefault(expression.getSourceIndex());
        visitDefault(expression.getCount());
    }

    @Override
    public void visit(WasmTry expression) {
        visitMany(expression.getBody());
        for (var catchClause : expression.getCatches()) {
            visitMany(catchClause.getBody());
        }
    }

    @Override
    public void visit(WasmThrow expression) {
        visitMany(expression.getArguments());
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        visitDefault(expression.getFirst());
        visitDefault(expression.getSecond());
    }

    @Override
    public void visit(WasmCast expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmTest expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmExternConversion expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmStructNew expression) {
        visitMany(expression.getInitializers());
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
    }

    @Override
    public void visit(WasmStructGet expression) {
        visitDefault(expression.getInstance());
    }

    @Override
    public void visit(WasmStructSet expression) {
        visitDefault(expression.getInstance());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        visitDefault(expression.getLength());
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        visitMany(expression.getElements());
    }

    @Override
    public void visit(WasmArrayGet expression) {
        visitDefault(expression.getInstance());
        visitDefault(expression.getIndex());
    }

    @Override
    public void visit(WasmArraySet expression) {
        visitDefault(expression.getInstance());
        visitDefault(expression.getIndex());
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmArrayLength expression) {
        visitDefault(expression.getInstance());
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        visitDefault(expression.getTargetArray());
        visitDefault(expression.getTargetIndex());
        visitDefault(expression.getSourceArray());
        visitDefault(expression.getSourceIndex());
        visitDefault(expression.getSize());
    }

    @Override
    public void visit(WasmFunctionReference expression) {
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmInt31Get expression) {
        visitDefault(expression.getValue());
    }

    @Override
    public void visit(WasmPush expression) {
        visitDefault(expression.getArgument());
    }

    @Override
    public void visit(WasmPop expression) {
    }

    public void visitMany(List<WasmExpression> expressions) {
        for (var expression : expressions) {
            visitDefault(expression);
        }
    }

    public void visitDefault(WasmExpression expression) {
        expression.acceptVisitor(this);
    }
}
