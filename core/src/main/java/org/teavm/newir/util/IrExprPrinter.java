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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.newir.expr.IrBinaryExpr;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrExitLoopExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetStaticFieldExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrNullaryExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSequenceExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrUnaryExpr;
import org.teavm.newir.expr.IrVariable;
import org.teavm.newir.expr.IrVariableExpr;
import org.teavm.newir.expr.RecursiveIrExprVisitor;

public class IrExprPrinter extends RecursiveIrExprVisitor {
    private Map<IrExpr, Node> nodes = new HashMap<>();
    private ObjectIntMap<IrVariable> vars = new ObjectIntHashMap<>();
    private int refGenerator;
    private PrintWriter writer;
    private Consumer<PrintWriter> exprPrinter;
    private Node root;

    public IrExprPrinter(PrintWriter writer) {
        this.writer = writer;
    }

    public void print(IrExpr expr) {
        printExpr(expr);
        Node root = null;
        Node node = this.root;
        while (node != null) {
            Node next = node.next;
            node.next = root;
            root = node;
            node = next;
        }

        node = root;
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

        this.root = null;
        nodes.clear();
        refGenerator = 0;
    }

    private void printExpr(IrExpr expr) {
        Node existing = nodes.get(expr);
        if (existing != null && existing.line != null) {
            if (existing.ref == 0) {
                existing.ref = ++refGenerator;
            }
            Node next = root;
            root = new Node();
            root.next = next;
            root.line = w -> {
                w.print("[ref ");
                w.print(existing.ref);
                w.print("]");
            };
            return;
        }

        Node newRoot = existing != null ? existing : new Node();
        nodes.put(expr, newRoot);

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

        int inputCount = expr.getInputCount();
        if (inputCount > 0) {
            printExpr(expr.getInput(0));
            Node formerRoot = root;
            for (int i = 1; i < inputCount; ++i) {
                printExpr(expr.getInput(i));
                Node node = root;
                node.line = prepend("  ├──", node.line);
                node = node.next;
                while (node != formerRoot) {
                    node.line = prepend("  │  ", node.line);
                    node = node.next;
                }
                formerRoot = root;
            }
            if (inputCount == 1) {
                Node emptyLine = new Node();
                emptyLine.next = root;
                emptyLine.line = w -> w.print("  │");
                root = emptyLine;
            }
        }

        exprPrinter = null;
        newRoot.next = root;
        root = newRoot;
        root.line = mainPrinter;
        nodes.put(expr, root);
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
    public void visit(IrSequenceExpr expr) {
        exprPrinter = w -> w.print(";");
    }

    @Override
    public void visit(IrCallExpr expr) {
        exprPrinter = w -> w.append("call ").append(expr.getMethod().getName());
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
    public void visit(IrBinaryExpr expr) {
        exprPrinter = w -> w.print(expr.getOperation().name());
    }

    @Override
    public void visit(IrUnaryExpr expr) {
        exprPrinter = w -> w.print(expr.getOperation().name());
    }

    @Override
    public void visit(IrNullaryExpr expr) {
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
    public void visit(IrVariableExpr expr) {
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
        exprPrinter = w -> {
            w.print("field ");
            w.print(expr.getField().getFieldName());
        };
    }

    @Override
    public void visit(IrGetStaticFieldExpr expr) {
        exprPrinter = w -> {
            w.print("static field ");
            w.print(expr.getField().getFieldName());
        };
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
            nodes.put(expr, node);
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
