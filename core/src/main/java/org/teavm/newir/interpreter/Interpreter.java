/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.interpreter;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import org.teavm.newir.expr.IrFunction;
import org.teavm.newir.expr.IrParameter;
import org.teavm.newir.expr.IrVariable;

public class Interpreter {
    private ExprInterpreterContext ctx;
    InterpreterInstruction[] instructions;

    public Interpreter(IrFunction function) {
        ObjectIntMap<IrParameter> parameterMap = new ObjectIntHashMap<>();
        ObjectIntMap<IrVariable> variableMap = new ObjectIntHashMap<>();
        int intIndex = 0;
        int longIndex = 0;
        int floatIndex = 0;
        int doubleIndex = 0;
        int objectIndex = 0;

        for (int i = 0; i < function.getParameterCount(); ++i) {
            IrParameter parameter = function.getParameter(i);
            switch (parameter.getType().getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                    parameterMap.put(parameter, intIndex++);
                    break;
                case LONG:
                    parameterMap.put(parameter, longIndex++);
                    break;
                case FLOAT:
                    parameterMap.put(parameter, floatIndex++);
                    break;
                case DOUBLE:
                    parameterMap.put(parameter, doubleIndex++);
                    break;
                default:
                    break;
            }
        }
        for (IrVariable variable : function.getVariables()) {
            switch (variable.getType().getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                    variableMap.put(variable, intIndex++);
                    break;
                case LONG:
                    variableMap.put(variable, longIndex++);
                    break;
                case FLOAT:
                    variableMap.put(variable, floatIndex++);
                    break;
                case DOUBLE:
                    variableMap.put(variable, doubleIndex++);
                    break;
                default:
                    break;
            }
        }

        InterpreterBuilderVisitor visitor = new InterpreterBuilderVisitor(intIndex, longIndex, floatIndex,
                doubleIndex, objectIndex, parameterMap, variableMap);
        function.getBody().acceptVisitor(visitor);

        ctx = new ExprInterpreterContext(intIndex, longIndex, floatIndex, doubleIndex, objectIndex);
        instructions = visitor.instructions.toArray(new InterpreterInstruction[0]);
    }

    Interpreter(int intValueCount, int longValueCount, int floatValueCount, int doubleValueCount,
            int objectValueCount, InterpreterInstruction[] instructions) {
        ctx = new ExprInterpreterContext(intValueCount, longValueCount, floatValueCount, doubleValueCount,
                objectValueCount);
        this.instructions = instructions;
    }

    public void run() {
        ctx.resume();
        while (!ctx.stopped) {
            instructions[ctx.ptr].accept(ctx);
        }
    }
}
