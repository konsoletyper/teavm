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
import static org.teavm.newir.builder.IrExprBuilder.call;
import static org.teavm.newir.builder.IrExprBuilder.get;
import static org.teavm.newir.builder.IrExprBuilder.greaterEq;
import static org.teavm.newir.builder.IrExprBuilder.ifCond;
import static org.teavm.newir.builder.IrExprBuilder.intVar;
import static org.teavm.newir.builder.IrExprBuilder.loop;
import static org.teavm.newir.builder.IrExprBuilder.set;
import java.io.PrintWriter;
import java.util.Arrays;
import org.teavm.newir.binary.IrPackedProgram;
import org.teavm.newir.binary.IrSerializer;
import org.teavm.newir.builder.IrExprBuilder;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrProgram;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.type.IrType;
import org.teavm.newir.util.IrExprPrinter;

public final class Fib {
    private static IrFunction printFunction = new IrFunction(IrType.VOID, IrType.INT);

    private Fib() {
    }

    private static IrProgram createFibonacci() {
        return IrExprBuilder.program(new IrType[] { IrType.INT }, parameters -> {
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
                call(printFunction, get(c));
            });
        });
    }

    public static void main(String[] args) {
        IrProgram fib = createFibonacci();
        PrintWriter writer = new PrintWriter(System.out);
        IrExprPrinter printer = new IrExprPrinter(writer);
        printer.print(fib.getBody());
        writer.flush();

        IrSerializer serializer = new IrSerializer();
        IrPackedProgram packed = serializer.serialize(fib);

        System.err.println(Arrays.toString(packed.getData()));

        /*
        Interpreter interpreter = new Interpreter(fib);
        interpreter.setIntParameter(fib.getParameter(0), 5);
        interpreter.run();
         */
    }
}
