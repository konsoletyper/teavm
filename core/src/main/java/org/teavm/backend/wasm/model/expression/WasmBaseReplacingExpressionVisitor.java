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
import org.teavm.backend.wasm.model.WasmFunction;

public class WasmBaseReplacingExpressionVisitor implements WasmExpressionVisitor {
    private WasmExpression replacement;

    public void replace(WasmFunction function) {
        replaceExpressions(function.getBody());
    }

    protected void replaceCurrent(WasmExpression expression) {
        this.replacement = expression;
    }

    private void replaceExpressions(List<WasmExpression> expressions) {
        for (int i = 0; i < expressions.size(); ++i) {
            expressions.set(i, replace(expressions.get(i)));
        }
    }

    @Override
    public void visit(WasmBlock expression) {
        replaceExpressions(expression.getBody());
    }

    @Override
    public void visit(WasmBranch expression) {
        expression.setCondition(replace(expression.getCondition()));
        expression.setResult(replace(expression.getResult()));
    }

    @Override
    public void visit(WasmNullBranch expression) {
        expression.setValue(replace(expression.getValue()));
        expression.setResult(replace(expression.getResult()));
    }

    @Override
    public void visit(WasmCastBranch expression) {
        expression.setValue(replace(expression.getValue()));
        expression.setResult(replace(expression.getResult()));
    }

    @Override
    public void visit(WasmBreak expression) {
        expression.setResult(replace(expression.getResult()));
    }

    @Override
    public void visit(WasmSwitch expression) {
        expression.setSelector(replace(expression.getSelector()));
    }

    @Override
    public void visit(WasmConditional expression) {
        expression.setCondition(replace(expression.getCondition()));
        replaceExpressions(expression.getThenBlock().getBody());
        replaceExpressions(expression.getElseBlock().getBody());
    }

    @Override
    public void visit(WasmReturn expression) {
        expression.setValue(replace(expression.getValue()));
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
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmGetLocal expression) {
    }

    @Override
    public void visit(WasmSetLocal expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmGetGlobal expression) {
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmIntBinary expression) {
        expression.setFirst(replace(expression.getFirst()));
        expression.setSecond(replace(expression.getSecond()));
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        expression.setFirst(replace(expression.getFirst()));
        expression.setSecond(replace(expression.getSecond()));
    }

    @Override
    public void visit(WasmIntUnary expression) {
        expression.setOperand(replace(expression.getOperand()));
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        expression.setOperand(replace(expression.getOperand()));
    }

    @Override
    public void visit(WasmConversion expression) {
        expression.setOperand(replace(expression.getOperand()));
    }

    @Override
    public void visit(WasmCall expression) {
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        expression.setSelector(replace(expression.getSelector()));
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmCallReference expression) {
        expression.setFunctionReference(replace(expression.getFunctionReference()));
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmDrop expression) {
        expression.setOperand(replace(expression.getOperand()));
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        expression.setIndex(replace(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        expression.setIndex(replace(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        expression.setIndex(replace(expression.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        expression.setIndex(replace(expression.getIndex()));
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        expression.setAmount(replace(expression.getAmount()));
    }

    @Override
    public void visit(WasmFill expression) {
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
        expression.setCount(replace(expression.getCount()));
    }

    @Override
    public void visit(WasmCopy expression) {
        expression.setSourceIndex(replace(expression.getSourceIndex()));
        expression.setDestinationIndex(replace(expression.getDestinationIndex()));
        expression.setCount(replace(expression.getCount()));
    }

    @Override
    public void visit(WasmTry expression) {
        replaceExpressions(expression.getBody());
        for (var catchClause : expression.getCatches()) {
            replaceExpressions(catchClause.getBody());
        }
    }

    @Override
    public void visit(WasmThrow expression) {
        replaceExpressions(expression.getArguments());
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        expression.setFirst(replace(expression.getFirst()));
        expression.setSecond(replace(expression.getSecond()));
    }

    @Override
    public void visit(WasmCast expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmTest expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmExternConversion expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmStructNew expression) {
        replaceExpressions(expression.getInitializers());
    }

    @Override
    public void visit(WasmStructNewDefault expression) {
    }

    @Override
    public void visit(WasmStructGet expression) {
        expression.setInstance(replace(expression.getInstance()));
    }

    @Override
    public void visit(WasmStructSet expression) {
        expression.setInstance(replace(expression.getInstance()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        expression.setLength(replace(expression.getLength()));
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        replaceExpressions(expression.getElements());
    }

    @Override
    public void visit(WasmArrayGet expression) {
        expression.setInstance(replace(expression.getInstance()));
        expression.setIndex(replace(expression.getIndex()));
    }

    @Override
    public void visit(WasmArraySet expression) {
        expression.setInstance(replace(expression.getInstance()));
        expression.setIndex(replace(expression.getIndex()));
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmArrayLength expression) {
        expression.setInstance(replace(expression.getInstance()));
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        expression.setSourceArray(replace(expression.getSourceArray()));
        expression.setSourceIndex(replace(expression.getSourceIndex()));
        expression.setTargetArray(replace(expression.getTargetArray()));
        expression.setTargetIndex(replace(expression.getTargetIndex()));
        expression.setSize(replace(expression.getSize()));
    }

    @Override
    public void visit(WasmFunctionReference expression) {
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmInt31Get expression) {
        expression.setValue(replace(expression.getValue()));
    }

    @Override
    public void visit(WasmPush expression) {
    }

    @Override
    public void visit(WasmPop expression) {
    }

    private WasmExpression replace(WasmExpression expression) {
        if (expression == null) {
            return null;
        }
        visitDefault(expression);
        var result = replacement;
        if (result == null) {
            result = expression;
        } else {
            replacement = null;
        }
        return result;
    }

    protected void visitDefault(WasmExpression expression) {
        expression.acceptVisitor(this);
    }
}
