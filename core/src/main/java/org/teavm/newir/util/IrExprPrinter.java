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
package org.teavm.newir.util;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.newir.decl.IrDeclaration;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.decl.IrMethod;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrFunctionCallTarget;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetGlobalExpr;
import org.teavm.newir.expr.IrGetVariableExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrLoopHeaderExpr;
import org.teavm.newir.expr.IrMethodCallTarget;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrVariable;

public class IrExprPrinter extends RecursiveIrExprVisitor {
    private ObjectIntMap<IrVariable> vars = new ObjectIntHashMap<>();
    private PrintWriter writer;
    private Consumer<PrintWriter> exprPrinter;
    private boolean noBackref;
    private NameGenerator<IrFunction> functionNames = new NameGenerator<>(IrDeclaration::getNameHint);
    private NameGenerator<IrMethod> methodNames = new NameGenerator<>(IrDeclaration::getNameHint);
    private ScheduledIrExprTags<Line> tags = new ScheduledIrExprTags<>(expr -> new Line());
    private List<Line> lines = new ArrayList<>();

    public IrExprPrinter(PrintWriter writer) {
        this.writer = writer;
    }

    public void print(IrExpr expr) {
        tags.fill(expr);
        printExpr(tags.first(expr));

        int index = 1;
        for (Line line : lines) {
            if (line.ref > 0) {
                line.ref = index++;
            }
        }

        for (Line line : lines) {
            line.content.accept(writer);
            if (line.ref > 0) {
                writer.print(" (");
                writer.print(line.ref);
                writer.print(")");
            }
            writer.println();
        }

        lines.clear();
        tags.cleanup();
    }

    private void printExpr(IrExpr expr) {
        if (expr.getDependencyCount() > 0) {
            printRef(expr.getDependency(0));
        }

        while (expr != null) {
            int depCount = expr.getDependencyCount();
            for (int i = 1; i < depCount; ++i) {
                int lastIndex = lines.size();
                printInput(expr.getDependency(i));
                for (int j = lastIndex; j < lines.size() - 1; ++j) {
                    Line line = lines.get(j);
                    line.content = prepend("  │  ", line.content);
                }
                Line lastList = lines.get(lines.size() - 1);
                lastList.content = prepend("  ├──", lastList.content);
            }

            if (depCount == 1) {
                lines.add(new Line(w -> w.print("  │")));
            }

            Line line = tags.get(expr);
            line.content = printOpcode(expr);
            line.noBackref = noBackref;
            lines.add(line);
            exprPrinter = null;
            noBackref = false;

            expr = tags.next(expr);
        }
    }

    private Consumer<PrintWriter> printOpcode(IrExpr expr) {
        expr.acceptVisitor(this);
        if (exprPrinter == null) {
            exprPrinter = w -> w.print("unknown");
        }
        Consumer<PrintWriter> notWrapped = exprPrinter;
        return w -> {
            w.print("[");
            notWrapped.accept(w);
            w.print("]");
        };
    }

    private void printInput(IrExpr expr) {
        if (tags.isLast(expr)) {
            printExpr(tags.first(expr));
        } else {
            printRef(expr);
        }
    }

    private void printRef(IrExpr expr) {
        Line line = tags.get(expr);
        if (line.noBackref) {
            lines.add(new Line(printOpcode(expr)));
        } else {
            line.ref = 1;
            lines.add(new Line(w -> {
                w.print("[ref ");
                w.print(line.ref);
                w.print("]");
            }));
        }
    }

    private Consumer<PrintWriter> prepend(String prefix, Consumer<PrintWriter> fragment) {
        return w -> {
            w.append(prefix);
            fragment.accept(w);
        };
    }

    @Override
    protected void visitDefault(IrExpr expr) {
    }

    @Override
    public void visit(IrCallExpr expr) {
        switch (expr.getTarget().getType()) {
            case FUNCTION: {
                IrFunction function = ((IrFunctionCallTarget) expr.getTarget()).getCallable();
                String name = functionNames.getName(function);
                exprPrinter = w -> w.append("call-function ").append(name);
                break;
            }
            case METHOD: {
                IrMethod method = ((IrMethodCallTarget) expr.getTarget()).getCallable();
                String name = methodNames.getName(method);
                exprPrinter = w -> w.append("call-method ").append(name);
                break;
            }
        }
    }

    @Override
    public void visit(IrLoopExpr expr) {
        exprPrinter = w -> w.append("loop");
    }

    @Override
    public void visit(IrBlockExpr expr) {
        exprPrinter = w -> w.print("block");
    }

    @Override
    public void visit(IrOperationExpr expr) {
        exprPrinter = w -> w.print(expr.getOperation().name());
        switch (expr.getOperation()) {
            case NULL:
            case VOID:
            case START:
                noBackref = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
        tags.get(expr).noBackref = true;
        exprPrinter = w -> {
            w.print("int ");
            w.print(expr.getValue());
        };
    }

    @Override
    public void visit(IrGetVariableExpr expr) {
        int id = variableId(expr.getVariable());
        exprPrinter = w -> {
            w.print("var ");
            w.print(id);
        };
    }

    @Override
    public void visit(IrSetVariableExpr expr) {
        int id = variableId(expr.getVariable());
        exprPrinter = w -> {
            w.print("set var ");
            w.print(id);
        };
    }

    @Override
    public void visit(IrParameterExpr expr) {
        exprPrinter = w -> {
            w.print("param");
            w.print(expr.getParameter().getIndex());
        };
    }

    @Override
    public void visit(IrGetFieldExpr expr) {

    }

    @Override
    public void visit(IrGetGlobalExpr expr) {

    }

    @Override
    public void visit(IrConditionalExpr expr) {
        exprPrinter = w -> w.print("if");
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        Line targetLine = tags.get(expr.getLoop());
        targetLine.ref = 1;
        exprPrinter = w -> {
            w.print("exit loop ");
            w.print(targetLine.ref);
        };
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
        Line targetLine = tags.get(expr.getLoop());
        targetLine.ref = 1;
        exprPrinter = w -> {
            w.print("loop header ");
            w.print(targetLine.ref);
        };
    }

    private int variableId(IrVariable var) {
        int id = vars.getOrDefault(var, -1);
        if (id < 0) {
            id = vars.size();
            vars.put(var, id);
        }
        return id;
    }

    static class Line {
        Consumer<PrintWriter> content;
        int ref;
        boolean noBackref;

        Line() {
            this(null);
        }

        Line(Consumer<PrintWriter> content) {
            this.content = content;
        }
    }
}
