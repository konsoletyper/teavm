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
import java.util.function.Consumer;
import org.teavm.newir.decl.IrDeclaration;
import org.teavm.newir.decl.IrFunction;
import org.teavm.newir.decl.IrMethod;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrExprTags;
import org.teavm.newir.expr.IrFunctionCallTarget;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetGlobalExpr;
import org.teavm.newir.expr.IrGetVariableExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrMethodCallTarget;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrVariable;

public class IrExprPrinter extends RecursiveIrExprVisitor {
    private ObjectIntMap<IrVariable> vars = new ObjectIntHashMap<>();
    private int refGenerator;
    private PrintWriter writer;
    private Consumer<PrintWriter> exprPrinter;
    private NameGenerator<IrFunction> functionNames = new NameGenerator<>(IrDeclaration::getNameHint);
    private NameGenerator<IrMethod> methodNames = new NameGenerator<>(IrDeclaration::getNameHint);
    private IrExprTags<Node> nodes = new IrExprTags<>();

    public IrExprPrinter(PrintWriter writer) {
        this.writer = writer;
    }

    public void print(IrExpr expr) {
        Node node = printExpr(null, expr);
        while (node != null) {
            node.line.accept(writer);
            if (node.ref > 0) {
                writer.print(" (");
                writer.print(node.ref);
                writer.print(")");
            }
            writer.println();
            node = node.next;
        }

        nodes.cleanup();
        refGenerator = 0;
    }

    private Node printExpr(Node last, IrExpr expr) {
        while (true) {
            Node node = nodes.get(expr);
            if (node != null && node.line != null) {
                if (node.ref == 0) {
                    node.ref = ++refGenerator;
                }
                int refId = node.ref;

                node = new Node();
                node.line = w -> {
                    w.print("[ref ");
                    w.print(refId);
                    w.print("]");
                };
                node.next = last;
                last = node;
                break;
            }

            if (node == null) {
                node = new Node();
                nodes.set(expr, node);
            }

            expr.acceptVisitor(this);
            if (exprPrinter == null) {
                exprPrinter = w -> w.print("unknown");
            }
            Consumer<PrintWriter> notWrapped = exprPrinter;
            Consumer<PrintWriter> mainPrinter = w -> {
                w.print("[");
                notWrapped.accept(w);
                w.print("]");
            };
            exprPrinter = null;
            node.line = mainPrinter;
            node.next = last;
            last = node;

            int inputCount = expr.getDependencyCount();
            if (inputCount == 0) {
                break;
            }

            for (int i = 1; i < inputCount; ++i) {
                if (i > 1) {
                    node = new Node();
                    node.line = w -> w.print("  │");
                    node.next = last;
                    last = node;
                }

                Node next = printExpr(last, expr.getDependency(i));
                if (next != last) {
                    node = next;
                    while (node.next != last) {
                        node.line = prepend("  │  ", node.line);
                        node = node.next;
                    }
                    node.line = prepend("  ├──", node.line);
                }
                last = next;
            }

            if (inputCount == 1) {
                node = new Node();
                node.line = w -> w.print("  │");
                node.next = last;
                last = node;
            }

            expr = expr.getDependency(0);
        }

        return last;
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
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
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
        int id = exprId(expr.getLoop());
        exprPrinter = w -> {
            w.print("exit loop ");
            w.print(id);
        };
    }

    private int exprId(IrExpr expr) {
        Node node = nodes.get(expr);
        if (node == null) {
            node = new Node();
            nodes.set(expr, node);
        }
        if (node.ref == 0) {
            node.ref = ++refGenerator;
        }
        return node.ref;
    }

    private int variableId(IrVariable var) {
        int id = vars.getOrDefault(var, -1);
        if (id < 0) {
            id = vars.size();
            vars.put(var, id);
        }
        return id;
    }

    static class Node {
        Node next;
        Consumer<PrintWriter> line;
        int ref;
    }
}
