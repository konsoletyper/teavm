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
package org.teavm.backend.wasm.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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
import org.teavm.backend.wasm.model.expression.WasmDefaultExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmFill;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt31Get;
import org.teavm.backend.wasm.model.expression.WasmInt31Reference;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
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
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmTry;

public class SuspensionPointCollector extends WasmDefaultExpressionVisitor {
    private Set<WasmExpression> suspending = new HashSet<>();

    public boolean isSuspending(WasmExpression expr) {
        return suspending.contains(expr);
    }

    @Override
    public void visit(WasmCall expression) {
        super.visit(expression);
        if (expression.isSuspensionPoint()) {
            suspending.add(expression);
        } else {
            for (var arg : expression.getArguments()) {
                if (isSuspending(arg)) {
                    suspending.add(expression);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        super.visit(expression);
        if (expression.isSuspensionPoint()) {
            suspending.add(expression);
        } else {
            if (isSuspending(expression.getSelector())) {
                suspending.add(expression);
            } else {
                for (var arg : expression.getArguments()) {
                    if (isSuspending(arg)) {
                        suspending.add(expression);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void visit(WasmCallReference expression) {
        super.visit(expression);
        if (expression.isSuspensionPoint()) {
            suspending.add(expression);
        } else {
            if (isSuspending(expression.getFunctionReference())) {
                suspending.add(expression);
            } else {
                for (var arg : expression.getArguments()) {
                    if (isSuspending(arg)) {
                        suspending.add(expression);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void visit(WasmBlock expression) {
        super.visit(expression);
        visitExpr(expression, WasmBlock::getBody);
    }

    @Override
    public void visit(WasmTry expression) {
        super.visit(expression);
        visitExpr(expression, e -> {
            var result = new ArrayList<>(e.getBody());
            for (var catchClause : expression.getCatches()) {
                result.addAll(catchClause.getBody());
            }
            return result;
        });
    }

    @Override
    public void visit(WasmThrow expression) {
        super.visit(expression);
        visitExpr(expression, WasmThrow::getArguments);
    }

    @Override
    public void visit(WasmBreak expression) {
        super.visit(expression);
        visitExpr(expression, e -> Arrays.asList(e.getResult()));
    }

    @Override
    public void visit(WasmBranch expression) {
        super.visit(expression);
        visitExpr(expression, e -> Arrays.asList(e.getCondition(), e.getResult()));
    }

    @Override
    public void visit(WasmCastBranch expression) {
        super.visit(expression);
        visitExpr(expression, e -> Arrays.asList(e.getResult(), e.getValue()));
    }

    @Override
    public void visit(WasmNullBranch expression) {
        super.visit(expression);
        visitExpr(expression, e -> Arrays.asList(e.getResult(), e.getValue()));
    }

    @Override
    public void visit(WasmSwitch expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getSelector()));
    }

    @Override
    public void visit(WasmConditional expression) {
        super.visit(expression);
        visitExpr(expression.getThenBlock(), WasmBlock::getBody);
        visitExpr(expression.getElseBlock(), WasmBlock::getBody);
        visitExpr(expression, e -> List.of(e.getCondition(), e.getThenBlock(), e.getElseBlock()));
    }

    @Override
    public void visit(WasmReturn expression) {
        super.visit(expression);
        visitExpr(expression, e -> Arrays.asList(e.getValue()));
    }

    @Override
    public void visit(WasmDrop expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getOperand()));
    }

    @Override
    public void visit(WasmCast expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmTest expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmIsNull expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmReferencesEqual expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getFirst(), e.getSecond()));
    }

    @Override
    public void visit(WasmCopy expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getSourceIndex(), e.getDestinationIndex(), e.getCount()));
    }

    @Override
    public void visit(WasmFill expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex(), e.getValue(), e.getCount()));
    }

    @Override
    public void visit(WasmStructNew expression) {
        super.visit(expression);
        visitExpr(expression, WasmStructNew::getInitializers);
    }

    @Override
    public void visit(WasmStructGet expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getInstance()));
    }

    @Override
    public void visit(WasmStructSet expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getInstance(), e.getValue()));
    }

    @Override
    public void visit(WasmArrayNewFixed expression) {
        super.visit(expression);
        visitExpr(expression, WasmArrayNewFixed::getElements);
    }

    @Override
    public void visit(WasmArrayNewDefault expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getLength()));
    }

    @Override
    public void visit(WasmArrayLength expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getInstance()));
    }

    @Override
    public void visit(WasmArrayGet expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getInstance(), e.getIndex()));
    }

    @Override
    public void visit(WasmArraySet expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getInstance(), e.getIndex(), e.getValue()));
    }

    @Override
    public void visit(WasmArrayCopy expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getSourceArray(), e.getSourceArray(), e.getTargetArray(),
                e.getTargetIndex(), e.getSize()));
    }

    @Override
    public void visit(WasmInt31Get expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmInt31Reference expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmIntUnary expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getOperand()));
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getOperand()));
    }

    @Override
    public void visit(WasmIntBinary expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getFirst(), e.getSecond()));
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getFirst(), e.getSecond()));
    }

    @Override
    public void visit(WasmConversion expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getOperand()));
    }

    @Override
    public void visit(WasmExternConversion expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmSetLocal expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmSetGlobal expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getValue()));
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex()));
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex()));
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex()));
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex(), e.getValue()));
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex(), e.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex(), e.getValue()));
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getIndex(), e.getValue()));
    }

    @Override
    public void visit(WasmMemoryGrow expression) {
        super.visit(expression);
        visitExpr(expression, e -> List.of(e.getAmount()));
    }

    private <T extends WasmExpression> void visitExpr(T expr, Function<T, List<WasmExpression>> argsExtractor) {
        for (var arg : argsExtractor.apply(expr)) {
            if (arg != null && isSuspending(arg)) {
                suspending.add(expr);
                break;
            }
        }
    }
}
