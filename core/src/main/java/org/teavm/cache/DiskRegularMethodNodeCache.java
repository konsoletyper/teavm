/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.ast.cache.MethodNodeCache;
import org.teavm.model.MethodReference;
import org.teavm.parsing.ClassDateProvider;

public class DiskRegularMethodNodeCache implements MethodNodeCache {
    private final File directory;
    private final AstIO astIO;
    private final ClassDateProvider classDateProvider;
    private final Map<MethodReference, Item> cache = new HashMap<>();
    private final Map<MethodReference, AsyncItem> asyncCache = new HashMap<>();
    private final Set<MethodReference> newMethods = new HashSet<>();
    private final Set<MethodReference> newAsyncMethods = new HashSet<>();

    public DiskRegularMethodNodeCache(File directory, SymbolTable symbolTable, SymbolTable fileTable,
            ClassDateProvider classDateProvider) {
        this.directory = directory;
        astIO = new AstIO(symbolTable, fileTable);
        this.classDateProvider = classDateProvider;
    }

    @Override
    public RegularMethodNode get(MethodReference methodReference) {
        Item item = cache.get(methodReference);
        if (item == null) {
            item = new Item();
            cache.put(methodReference, item);
            File file = getMethodFile(methodReference, false);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    if (!checkIfDependenciesChanged(input, file)) {
                        item.node = astIO.read(input, methodReference);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.node;
    }

    @Override
    public void store(MethodReference methodReference, RegularMethodNode node) {
        Item item = new Item();
        item.node = node;
        cache.put(methodReference, item);
        newMethods.add(methodReference);
    }

    @Override
    public AsyncMethodNode getAsync(MethodReference methodReference) {
        AsyncItem item = asyncCache.get(methodReference);
        if (item == null) {
            item = new AsyncItem();
            asyncCache.put(methodReference, item);
            File file = getMethodFile(methodReference, true);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    if (!checkIfDependenciesChanged(input, file)) {
                        item.node = astIO.readAsync(input, methodReference);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.node;
    }

    private boolean checkIfDependenciesChanged(DataInput input, File file) throws IOException {
        int depCount = input.readShort();
        for (int i = 0; i < depCount; ++i) {
            String depClass = input.readUTF();
            Date depDate = classDateProvider.getModificationDate(depClass);
            if (depDate == null || depDate.after(new Date(file.lastModified()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeAsync(MethodReference methodReference, AsyncMethodNode node) {
        AsyncItem item = new AsyncItem();
        item.node = node;
        asyncCache.put(methodReference, item);
        newAsyncMethods.add(methodReference);
    }

    public void flush() throws IOException {
        for (MethodReference method : newMethods) {
            File file = getMethodFile(method, true);
            AstDependencyAnalyzer analyzer = new AstDependencyAnalyzer();
            RegularMethodNode node = cache.get(method).node;
            node.getBody().acceptVisitor(analyzer);
            analyzer.dependencies.add(method.getClassName());
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                output.writeShort(analyzer.dependencies.size());
                for (String dependency : analyzer.dependencies) {
                    output.writeUTF(dependency);
                }
                astIO.write(output, node);
            }
        }
        for (MethodReference method : newAsyncMethods) {
            File file = getMethodFile(method, true);
            AstDependencyAnalyzer analyzer = new AstDependencyAnalyzer();
            AsyncMethodNode node = asyncCache.get(method).node;
            for (AsyncMethodPart part : node.getBody()) {
                part.getStatement().acceptVisitor(analyzer);
            }
            analyzer.dependencies.add(method.getClassName());
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
                output.writeShort(analyzer.dependencies.size());
                for (String dependency : analyzer.dependencies) {
                    output.writeUTF(dependency);
                }
                astIO.writeAsync(output, node);
            }
        }
    }

    private File getMethodFile(MethodReference method, boolean async) {
        File dir = new File(directory, method.getClassName().replace('.', '/'));
        return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-ast"
                + (async ? "-async" : ""));
    }

    private static class AstDependencyAnalyzer implements StatementVisitor, ExprVisitor {
        final Set<String> dependencies = new HashSet<>();

        private void visitSequence(List<Statement> statements) {
            for (Statement stmt : statements) {
                stmt.acceptVisitor(this);
            }
        }

        @Override
        public void visit(AssignmentStatement statement) {
            if (statement.getLeftValue() != null) {
                statement.getLeftValue().acceptVisitor(this);
            }
            statement.getRightValue().acceptVisitor(this);
        }

        @Override
        public void visit(SequentialStatement statement) {
            visitSequence(statement.getSequence());
        }

        @Override
        public void visit(ConditionalStatement statement) {
            statement.getCondition().acceptVisitor(this);
            visitSequence(statement.getConsequent());
            visitSequence(statement.getAlternative());
        }

        @Override
        public void visit(SwitchStatement statement) {
            statement.getValue().acceptVisitor(this);
            for (SwitchClause clause : statement.getClauses()) {
                visitSequence(clause.getBody());
            }
            visitSequence(statement.getDefaultClause());
        }

        @Override
        public void visit(WhileStatement statement) {
            if (statement.getCondition() != null) {
                statement.getCondition().acceptVisitor(this);
            }
            visitSequence(statement.getBody());
        }

        @Override
        public void visit(BlockStatement statement) {
            visitSequence(statement.getBody());
        }

        @Override
        public void visit(BreakStatement statement) {
        }

        @Override
        public void visit(ContinueStatement statement) {
        }

        @Override
        public void visit(ReturnStatement statement) {
            if (statement.getResult() != null) {
                statement.getResult().acceptVisitor(this);
            }
        }

        @Override
        public void visit(ThrowStatement statement) {
            statement.getException().acceptVisitor(this);
        }

        @Override
        public void visit(InitClassStatement statement) {
        }

        @Override
        public void visit(TryCatchStatement statement) {
            visitSequence(statement.getProtectedBody());
            visitSequence(statement.getHandler());
        }

        @Override
        public void visit(BinaryExpr expr) {
            expr.getFirstOperand().acceptVisitor(this);
            expr.getSecondOperand().acceptVisitor(this);
        }

        @Override
        public void visit(UnaryExpr expr) {
            expr.getOperand().acceptVisitor(this);
        }

        @Override
        public void visit(ConditionalExpr expr) {
            expr.getCondition().acceptVisitor(this);
            expr.getConsequent().acceptVisitor(this);
            expr.getAlternative().acceptVisitor(this);
        }

        @Override
        public void visit(ConstantExpr expr) {
        }

        @Override
        public void visit(VariableExpr expr) {
        }

        @Override
        public void visit(SubscriptExpr expr) {
            expr.getArray().acceptVisitor(this);
            expr.getIndex().acceptVisitor(this);
        }

        @Override
        public void visit(UnwrapArrayExpr expr) {
            expr.getArray().acceptVisitor(this);
        }

        @Override
        public void visit(InvocationExpr expr) {
            dependencies.add(expr.getMethod().getClassName());
            for (Expr argument : expr.getArguments()) {
                argument.acceptVisitor(this);
            }
        }

        @Override
        public void visit(QualificationExpr expr) {
            dependencies.add(expr.getField().getClassName());
            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
            }
        }

        @Override
        public void visit(NewExpr expr) {
        }

        @Override
        public void visit(NewArrayExpr expr) {
            expr.getLength().acceptVisitor(this);
        }

        @Override
        public void visit(NewMultiArrayExpr expr) {
            for (Expr dimension : expr.getDimensions()) {
                dimension.acceptVisitor(this);
            }
        }

        @Override
        public void visit(CastExpr expr) {
            expr.getValue().acceptVisitor(this);
        }

        @Override
        public void visit(PrimitiveCastExpr expr) {
            expr.getValue().acceptVisitor(this);
        }

        @Override
        public void visit(InstanceOfExpr expr) {
            expr.getExpr().acceptVisitor(this);
        }

        @Override
        public void visit(GotoPartStatement statement) {
        }

        @Override
        public void visit(MonitorEnterStatement statement) {
        }

        @Override
        public void visit(MonitorExitStatement statement) {
        }
    }

    private static class Item {
        RegularMethodNode node;
    }

    private static class AsyncItem {
        AsyncMethodNode node;
    }
}
