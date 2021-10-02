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
package org.teavm.newir.test;

import static org.teavm.newir.builder.IrExprBuilder.add;
import static org.teavm.newir.builder.IrExprBuilder.callVirtual;
import static org.teavm.newir.builder.IrExprBuilder.function;
import static org.teavm.newir.builder.IrExprBuilder.get;
import static org.teavm.newir.builder.IrExprBuilder.greaterEq;
import static org.teavm.newir.builder.IrExprBuilder.ifCond;
import static org.teavm.newir.builder.IrExprBuilder.intVar;
import static org.teavm.newir.builder.IrExprBuilder.loop;
import static org.teavm.newir.builder.IrExprBuilder.set;
import java.io.PrintStream;
import java.io.PrintWriter;
import org.teavm.model.MethodReference;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrFunction;
import org.teavm.newir.expr.IrType;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.interpreter.Interpreter;
import org.teavm.newir.util.IrExprPrinter;

public final class Fib {
    private Fib() {
    }

    public static IrFunction createFibonacci() {
        return function(new IrType[] { IrType.INT }, parameters -> {
            IrVariable a = intVar();
            IrVariable b = intVar();
            IrVariable c = intVar();
            IrVariable i = intVar();
            set(a, 0);
            set(b, 1);
            set(i, 0);
            IrExpr n = get(parameters[0]);
            loop(mainLoop -> {
                ifCond(greaterEq(get(i), n), mainLoop::breakLoop);
                set(c, add(get(a), get(b)));
                set(a, get(b));
                set(b, get(c));
                set(i, add(get(i), 1));
                IrExpr out = get(System.class, "out", PrintStream.class);
                callVirtual(new MethodReference(PrintStream.class, "println", int.class, void.class), out, get(c));
            });
        });
    }

    public static void main(String[] args) {
        IrFunction fib = createFibonacci();
        PrintWriter writer = new PrintWriter(System.out);
        IrExprPrinter printer = new IrExprPrinter(writer);
        printer.print(fib.getBody());
        writer.flush();
        Interpreter interpreter = new Interpreter(fib);
        interpreter.setIntParameter(fib.getParameter(0), 5);
        interpreter.run();
    }
}
