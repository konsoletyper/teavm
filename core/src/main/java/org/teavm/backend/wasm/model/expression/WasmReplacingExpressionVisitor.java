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
import java.util.function.Function;
import org.teavm.backend.wasm.model.WasmFunction;

public class WasmReplacingExpressionVisitor implements WasmExpressionVisitor {
    private Function<WasmExpression, WasmExpression> mapper;

    public WasmReplacingExpressionVisitor(
            Function<WasmExpression, WasmExpression> mapper) {
        this.mapper = mapper;
    }

    public void replace(WasmFunction function) {
        replaceExpressions(function.getBody());
    }

    private void replaceExpressions(List<WasmExpression> expressions) {
        for (int i = 0; i < expressions.size(); ++i) {
            WasmExpression part = expressions.get(i);
            part.acceptVisitor(this);
            part = mapper.apply(part);
            expressions.set(i, part);
        }
    }

    @Override
    public void visit(WasmBlock expression) {
        replaceExpressions(expression.getBody());
    }

    @Override
    public void visit(WasmBranch expression) {
        expression.getCondition().acceptVisitor(this);
        expression.setCondition(mapper.apply(expression.getCondition()));
        if (expression.getResult() != null) {
            expression.getResult().acceptVisitor(this);
            expression.setResult(mapper.apply(expression.getResult()));
        }
    }

    @Override
    public void visit(WasmBreak expression) {
        if (expression.getResult() != null) {
            expression.getResult().acceptVisitor(this);
            expression.setResult(mapper.apply(expression.getResult()));
        }
    }

    @Override
    public void visit(WasmSwitch expression) {
        expression.getSelector().acceptVisitor(this);
        expression.setSelector(mapper.apply(expression.getSelector()));
    }

    @Override
    public void visit(WasmConditional expression) {
        expression.getCondition().acceptVisitor(this);
        expression.setCondition(mapper.apply(expression.getCondition()));
        replaceExpressions(expression.getThenBlock().getBody());
        replaceExpressions(expression.getElseBlock().getBody());
    }

    @Override
    public void visit(WasmReturn expression) {
        if (expression.getValue() != null) {
            expression.getValue().acceptVisitor(this);
            expression.setValue(mapper.apply(expression.getValue()));
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
    public void visit(WasmGetLocal expression) {
    }

    @Override
    public void visit(WasmSetLocal expression) {
        expression.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(WasmIntBinary expression) {
        expression.getFirst().acceptVisitor(this);
        expression.setFirst(mapper.apply(expression.getFirst()));
        expression.getSecond().acceptVisitor(this);
        expression.setSecond(mapper.apply(expression.getSecond()));
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        expression.getFirst().acceptVisitor(this);
        expression.setFirst(mapper.apply(expression.getFirst()));
        expression.getSecond().acceptVisitor(this);
        expression.setSecond(mapper.apply(expression.getSecond()));
    }

    @Override
    public void visit(WasmIntUnary expression) {
        expression.getOperand().acceptVisitor(this);
        expression.setOperand(mapper.apply(expression.getOperand()));
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        expression.getOperand().acceptVisitor(this);
        expression.setOperand(mapper.apply(expression.getOperand()));
    }

    @Override
    public void visit(WasmConversion expression) {
        expression.getOperand().acceptVisitor(this);
        expression.setOperand(mapper.apply(expression.getOperand()));
    }

    @Override
    public void visit(WasmCall expression) {
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        expression.getSelector().acceptVisitor(this);
        expression.setSelector(mapper.apply(expression.getSelector()));
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmDrop expression) {
        expression.getOperand().acceptVisitor(this);
        expression.setOperand(mapper.apply(expression.getOperand()));
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
        expression.getValue().acceptVisitor(this);
        expression.setValue(mapper.apply(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
        expression.getValue().acceptVisitor(this);
        expression.setValue(mapper.apply(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
        expression.getValue().acceptVisitor(this);
        expression.setValue(mapper.apply(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.setIndex(mapper.apply(expression.getIndex()));
        expression.getValue().acceptVisitor(this);
        expression.setValue(mapper.apply(expression.getValue()));
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        expression.getAmount().acceptVisitor(this);
        expression.setAmount(mapper.apply(expression.getAmount()));
    }
}
