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
package org.teavm.ast.optimization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BoundCheckExpr;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
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
import org.teavm.ast.UnaryOperation;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

class OptimizingVisitor implements StatementVisitor, ExprVisitor {
    private static final int MAX_DEPTH = 20;
    private Expr resultExpr;
    Statement resultStmt;
    private final boolean[] preservedVars;
    private final int[] writeFrequencies;
    private final int[] initialWriteFrequencies;
    private final int[] readFrequencies;
    private final Object[] constants;
    private List<Statement> resultSequence;
    private boolean friendlyToDebugger;
    private TextLocation currentLocation;
    private Deque<TextLocation> locationStack = new LinkedList<>();
    private Deque<TextLocation> notNullLocationStack = new ArrayDeque<>();
    private List<ArrayOptimization> pendingArrayOptimizations;

    OptimizingVisitor(boolean[] preservedVars, int[] writeFrequencies, int[] readFrequencies, Object[] constants,
            boolean friendlyToDebugger) {
        this.preservedVars = preservedVars;
        this.writeFrequencies = writeFrequencies;
        this.initialWriteFrequencies = writeFrequencies.clone();
        this.readFrequencies = readFrequencies;
        this.constants = constants;
        this.friendlyToDebugger = friendlyToDebugger;
    }

    private static boolean isZero(Expr expr) {
        return expr instanceof ConstantExpr && Integer.valueOf(0).equals(((ConstantExpr) expr).getValue());
    }

    private static boolean isComparison(Expr expr) {
        return expr instanceof BinaryExpr && ((BinaryExpr) expr).getOperation() == BinaryOperation.COMPARE;
    }

    private void pushLocation(TextLocation location) {
        locationStack.push(location);
        if (location != null) {
            if (currentLocation != null) {
                notNullLocationStack.push(currentLocation);
            }
            currentLocation = location;
        }
    }

    private void popLocation() {
        if (locationStack.pop() != null) {
            currentLocation = notNullLocationStack.pollFirst();
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        pushLocation(expr.getLocation());
        try {
            switch (expr.getOperation()) {
                case AND:
                case OR:
                    resultExpr = expr;
                    return;
                default:
                    break;
            }
            Expr b = expr.getSecondOperand();
            Expr a = expr.getFirstOperand();

            int barrierPosition = 0;
            if (isSideEffectFree(a)) {
                barrierPosition++;
                if (isSideEffectFree(b)) {
                    barrierPosition++;
                }
            }

            Statement barrier = addBarrier();

            if (barrierPosition == 2) {
                removeBarrier(barrier);
            }
            b.acceptVisitor(this);
            b = resultExpr;
            if (b instanceof ConstantExpr && expr.getOperation() == BinaryOperation.SUBTRACT) {
                if (tryMakePositive((ConstantExpr) b)) {
                    expr.setOperation(BinaryOperation.ADD);
                }
            }

            if (barrierPosition == 1) {
                removeBarrier(barrier);
            }
            a.acceptVisitor(this);
            a = resultExpr;

            if (barrierPosition == 0) {
                removeBarrier(barrier);
            }

            Expr p = a;
            Expr q = b;
            boolean invert = false;
            if (isZero(p)) {
                Expr tmp = p;
                p = q;
                q = tmp;
                invert = true;
            }
            if (isComparison(p) && isZero(q)) {
                switch (expr.getOperation()) {
                    case EQUALS:
                    case NOT_EQUALS:
                    case LESS:
                    case LESS_OR_EQUALS:
                    case GREATER:
                    case GREATER_OR_EQUALS: {
                        BinaryExpr comparison = (BinaryExpr) p;
                        Expr result = BinaryExpr.binary(expr.getOperation(), comparison.getType(),
                                comparison.getFirstOperand(), comparison.getSecondOperand());
                        result.setLocation(comparison.getLocation());
                        if (invert) {
                            result = ExprOptimizer.invert(result);
                        }
                        resultExpr = result;
                        return;
                    }
                    default:
                        break;
                }
            }
            expr.setFirstOperand(a);
            expr.setSecondOperand(b);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getOperand().acceptVisitor(this);
            Expr operand = resultExpr;
            if (expr.getOperation() == UnaryOperation.NEGATE && operand instanceof ConstantExpr) {
                ConstantExpr constantExpr = (ConstantExpr) operand;
                if (tryMakePositive(constantExpr)) {
                    resultExpr = expr;
                    return;
                }
            }
            expr.setOperand(operand);
            resultExpr = expr;
        } finally {
             popLocation();
        }
    }

    private boolean tryMakePositive(ConstantExpr constantExpr) {
        Object value = constantExpr.getValue();
        if (value instanceof Integer && (Integer) value < 0) {
            constantExpr.setValue(-(Integer) value);
            return true;
        } else if (value instanceof Float && (Float) value < 0) {
            constantExpr.setValue(-(Float) value);
            return true;
        } else if (value instanceof Byte && (Byte) value < 0) {
            constantExpr.setValue(-(Byte) value);
            return true;
        } else if (value instanceof Short && (Short) value < 0) {
            constantExpr.setValue(-(Short) value);
            return true;
        } else if (value instanceof Long && (Long) value < 0) {
            constantExpr.setValue(-(Long) value);
            return true;
        } else if (value instanceof Double && (Double) value < 0) {
            constantExpr.setValue(-(Double) value);
            return true;
        }
        return false;
    }

    @Override
    public void visit(ConditionalExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getCondition().acceptVisitor(this);
            Expr cond = optimizeCondition(resultExpr);

            Statement barrier = addBarrier();
            expr.getConsequent().acceptVisitor(this);
            Expr consequent = resultExpr;
            expr.getAlternative().acceptVisitor(this);
            Expr alternative = resultExpr;
            removeBarrier(barrier);

            expr.setCondition(cond);
            expr.setConsequent(consequent);
            expr.setAlternative(alternative);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(VariableExpr expr) {
        pushLocation(expr.getLocation());
        try {
            int index = expr.getIndex();
            resultExpr = expr;
            if (writeFrequencies[index] != 1) {
                return;
            }

            if (!preservedVars[index] && initialWriteFrequencies[index] == 1 && constants[index] != null) {
                ConstantExpr constantExpr = new ConstantExpr();
                constantExpr.setValue(constants[index]);
                constantExpr.setLocation(expr.getLocation());
                resultExpr = constantExpr;
                return;
            }

            if (locationStack.size() > MAX_DEPTH) {
                return;
            }
            if (readFrequencies[index] != 1 || preservedVars[index]) {
                return;
            }
            if (resultSequence.isEmpty()) {
                return;
            }
            Statement last = resultSequence.get(resultSequence.size() - 1);
            if (!(last instanceof AssignmentStatement)) {
                return;
            }
            AssignmentStatement assignment = (AssignmentStatement) last;
            if (assignment.isAsync()) {
                return;
            }
            if (!(assignment.getLeftValue() instanceof VariableExpr)) {
                return;
            }
            VariableExpr var = (VariableExpr) assignment.getLeftValue();
            if (friendlyToDebugger) {
                if (currentLocation != null && assignment.getLocation() != null
                        && !assignment.getLocation().equals(currentLocation)) {
                    return;
                }
            }
            if (var.getIndex() == index) {
                resultSequence.remove(resultSequence.size() - 1);
                assignment.getRightValue().setLocation(assignment.getLocation());
                assignment.getRightValue().acceptVisitor(this);
            }
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        pushLocation(expr.getLocation());
        try {
            Expr index = expr.getIndex();
            Expr array = expr.getArray();

            int barrierPosition = 0;
            if (isSideEffectFree(array)) {
                ++barrierPosition;
                if (isSideEffectFree(index)) {
                    ++barrierPosition;
                }
            }

            Statement barrier = addBarrier();

            if (barrierPosition == 2) {
                removeBarrier(barrier);
            }

            expr.getIndex().acceptVisitor(this);
            index = resultExpr;

            if (barrierPosition == 1) {
                removeBarrier(barrier);
            }

            expr.getArray().acceptVisitor(this);
            array = resultExpr;

            if (barrierPosition == 0) {
                removeBarrier(barrier);
            }

            expr.setArray(array);
            expr.setIndex(index);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getArray().acceptVisitor(this);
            Expr arrayExpr = resultExpr;
            expr.setArray(arrayExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        pushLocation(expr.getLocation());
        try {
            Expr[] args = expr.getArguments().toArray(new Expr[0]);
            int barrierPos;

            for (barrierPos = 0; barrierPos < args.length; ++barrierPos) {
                if (!isSideEffectFree(args[barrierPos])) {
                    break;
                }
            }

            Statement barrier = addBarrier();

            if (barrierPos == args.length) {
                removeBarrier(barrier);
            }

            for (int i = args.length - 1; i >= 0; --i) {
                args[i].acceptVisitor(this);
                args[i] = resultExpr;
                if (i == barrierPos) {
                    removeBarrier(barrier);
                }
            }

            for (int i = 0; i < args.length; ++i) {
                expr.getArguments().set(i, args[i]);
            }

            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    private boolean tryApplyConstructor(InvocationExpr expr) {
        if (!expr.getMethod().getName().equals("<init>")) {
            return false;
        }
        if (resultSequence == null || resultSequence.isEmpty()) {
            return false;
        }
        Statement last = resultSequence.get(resultSequence.size() - 1);
        if (!(last instanceof AssignmentStatement)) {
            return false;
        }
        AssignmentStatement assignment = (AssignmentStatement) last;
        if (!(assignment.getLeftValue() instanceof VariableExpr)) {
            return false;
        }
        VariableExpr var = (VariableExpr) assignment.getLeftValue();
        if (!(expr.getArguments().get(0) instanceof VariableExpr)) {
            return false;
        }
        VariableExpr target = (VariableExpr) expr.getArguments().get(0);
        if (target.getIndex() != var.getIndex()) {
            return false;
        }
        if (!(assignment.getRightValue() instanceof NewExpr)) {
            return false;
        }
        NewExpr constructed = (NewExpr) assignment.getRightValue();
        if (!constructed.getConstructedClass().equals(expr.getMethod().getClassName())) {
            return false;
        }
        Expr[] args = expr.getArguments().toArray(new Expr[0]);
        args = Arrays.copyOfRange(args, 1, args.length);
        InvocationExpr constructrExpr = Expr.constructObject(expr.getMethod(), args);
        constructrExpr.setLocation(expr.getLocation());
        assignment.setRightValue(constructrExpr);
        readFrequencies[var.getIndex()]--;
        return true;
    }

    @Override
    public void visit(QualificationExpr expr) {
        pushLocation(expr.getLocation());
        try {
            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
                Expr qualified = resultExpr;
                expr.setQualified(qualified);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(NewExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getLength().acceptVisitor(this);
            Expr length = resultExpr;
            expr.setLength(length);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            for (int i = 0; i < expr.getDimensions().size(); ++i) {
                Expr dimension = expr.getDimensions().get(i);
                dimension.acceptVisitor(this);
                expr.getDimensions().set(i, resultExpr);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        pushLocation(expr.getLocation());
        try {
            for (int i = 0; i < expr.getData().size(); ++i) {
                Expr element = expr.getData().get(i);
                element.acceptVisitor(this);
                expr.getData().set(i, resultExpr);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getExpr().acceptVisitor(this);
            expr.setExpr(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(CastExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getValue().acceptVisitor(this);
            expr.setValue(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getValue().acceptVisitor(this);
            expr.setValue(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(AssignmentStatement statement) {
        pushLocation(statement.getLocation());
        try {
            if (statement.getLeftValue() == null) {
                statement.getRightValue().acceptVisitor(this);
                if (resultExpr instanceof InvocationExpr && tryApplyConstructor((InvocationExpr) resultExpr)) {
                    resultStmt = new SequentialStatement();
                } else {
                    statement.setRightValue(resultExpr);
                    resultStmt = statement;
                }
            } else {
                statement.getRightValue().acceptVisitor(this);
                Expr right = resultExpr;
                Expr left = statement.getLeftValue();
                if (!(statement.getLeftValue() instanceof VariableExpr)) {
                    statement.getLeftValue().acceptVisitor(this);
                    left = resultExpr;
                } else {
                    int varIndex = ((VariableExpr) statement.getLeftValue()).getIndex();
                    if (!preservedVars[varIndex] && initialWriteFrequencies[varIndex] == 1
                            && constants[varIndex] != null) {
                        resultStmt = new SequentialStatement();
                        return;
                    }
                }
                statement.setLeftValue(left);
                statement.setRightValue(right);
                resultStmt = statement;
            }
        } finally {
            popLocation();
        }
    }

    private List<Statement> processSequence(List<Statement> statements) {
        List<Statement> backup = resultSequence;
        resultSequence = new ArrayList<>();
        List<ArrayOptimization> pendingArrayOptimizationsBackup = pendingArrayOptimizations;
        pendingArrayOptimizations = new ArrayList<>();
        processSequenceImpl(statements);
        wieldTryCatch(resultSequence);
        List<Statement> result = resultSequence.stream().filter(part -> part != null).collect(Collectors.toList());
        resultSequence = backup;
        pendingArrayOptimizations = pendingArrayOptimizationsBackup;
        return result;
    }

    private boolean processSequenceImpl(List<Statement> statements) {
        for (Statement part : statements) {
            if (part instanceof SequentialStatement) {
                if (!processSequenceImpl(((SequentialStatement) part).getSequence())) {
                    return false;
                }
                continue;
            }
            part.acceptVisitor(this);
            part = resultStmt;
            if (part instanceof SequentialStatement) {
                if (!processSequenceImpl(((SequentialStatement) part).getSequence())) {
                    return false;
                }
                continue;
            }
            resultSequence.add(part);
            tryArrayOptimization();
            if (part instanceof BreakStatement) {
                return false;
            }
        }
        return true;
    }

    private void tryArrayOptimization() {
        Statement statement;
        while (!pendingArrayOptimizations.isEmpty()) {
            statement = resultSequence.get(resultSequence.size() - 1);
            int i = pendingArrayOptimizations.size() - 1;
            if (!tryArrayUnwrap(pendingArrayOptimizations.get(i), statement)
                    || tryArraySet(pendingArrayOptimizations.get(i), statement)) {
                pendingArrayOptimizations.remove(i);
            } else {
                break;
            }
        }
        statement = resultSequence.get(resultSequence.size() - 1);
        tryArrayConstruction(statement);
    }

    private void tryArrayConstruction(Statement statement) {
        if (!(statement instanceof AssignmentStatement)) {
            return;
        }
        AssignmentStatement assign = (AssignmentStatement) statement;

        if (!(assign.getLeftValue() instanceof VariableExpr)) {
            return;
        }
        int constructedArrayVariable = ((VariableExpr) assign.getLeftValue()).getIndex();

        if (!(assign.getRightValue() instanceof NewArrayExpr)) {
            return;
        }
        NewArrayExpr constructedArray = (NewArrayExpr) assign.getRightValue();
        if (!(constructedArray.getLength() instanceof ConstantExpr)) {
            return;
        }

        Object sizeConst = ((ConstantExpr) constructedArray.getLength()).getValue();
        if (!(sizeConst instanceof Integer)) {
            return;
        }

        int constructedArraySize = (int) sizeConst;
        ArrayOptimization optimization = new ArrayOptimization();
        optimization.index = resultSequence.size() - 1;
        optimization.arrayVariable = constructedArrayVariable;
        optimization.arraySize = constructedArraySize;
        optimization.array = constructedArray;
        pendingArrayOptimizations.add(optimization);
    }

    private boolean tryArrayUnwrap(ArrayOptimization optimization, Statement statement) {
        if (optimization.unwrappedArray != null) {
            return true;
        }

        if (!(statement instanceof AssignmentStatement)) {
            return false;
        }
        AssignmentStatement assign = (AssignmentStatement) statement;

        if (!(assign.getLeftValue() instanceof VariableExpr)) {
            return false;
        }
        optimization.unwrappedArrayVariable = ((VariableExpr) assign.getLeftValue()).getIndex();
        if (writeFrequencies[optimization.unwrappedArrayVariable] != 1) {
            return false;
        }

        if (!(assign.getRightValue() instanceof UnwrapArrayExpr)) {
            return false;
        }
        optimization.unwrappedArray = (UnwrapArrayExpr) assign.getRightValue();

        if (!(optimization.unwrappedArray.getArray() instanceof VariableExpr)) {
            return false;
        }

        VariableExpr arrayVar = (VariableExpr) optimization.unwrappedArray.getArray();
        if (arrayVar.getIndex() != optimization.arrayVariable) {
            return false;
        }

        if (!matchArrayType(optimization.array.getType(), optimization.unwrappedArray.getElementType())) {
            return false;
        }

        if (optimization.arraySize != readFrequencies[optimization.unwrappedArrayVariable]) {
            return false;
        }

        optimization.arrayElementIndex = 0;
        return true;
    }

    private boolean tryArraySet(ArrayOptimization optimization, Statement statement) {
        int expectedIndex = optimization.index + 2 + optimization.arrayElementIndex;
        if (resultSequence.size() - 1 != expectedIndex) {
            return false;
        }

        if (!(statement instanceof AssignmentStatement)) {
            return false;
        }
        AssignmentStatement assign = (AssignmentStatement) statement;

        if (!(assign.getLeftValue() instanceof SubscriptExpr)) {
            return false;
        }
        SubscriptExpr subscript = (SubscriptExpr) assign.getLeftValue();

        if (subscript.getType() != optimization.unwrappedArray.getElementType()) {
            return false;
        }

        if (!(subscript.getArray() instanceof VariableExpr)) {
            return false;
        }
        if (((VariableExpr) subscript.getArray()).getIndex() != optimization.unwrappedArrayVariable) {
            return false;
        }

        if (!(subscript.getIndex() instanceof ConstantExpr)) {
            return false;
        }
        Object constantValue = ((ConstantExpr) subscript.getIndex()).getValue();
        if (!Integer.valueOf(optimization.arrayElementIndex).equals(constantValue)) {
            return false;
        }

        optimization.elements.add(assign.getRightValue());
        if (++optimization.arrayElementIndex == optimization.arraySize) {
            applyArrayOptimization(optimization);
            return true;
        }
        return false;
    }

    private void applyArrayOptimization(ArrayOptimization optimization) {
        AssignmentStatement assign = (AssignmentStatement) resultSequence.get(optimization.index);
        ArrayFromDataExpr arrayFromData = new ArrayFromDataExpr();
        arrayFromData.setLocation(optimization.array.getLocation());
        arrayFromData.setType(optimization.array.getType());
        arrayFromData.getData().addAll(optimization.elements);
        assign.setRightValue(arrayFromData);
        readFrequencies[optimization.arrayVariable]--;
        resultSequence.subList(optimization.index + 1, resultSequence.size()).clear();
    }

    private void wieldTryCatch(List<Statement> statements) {
        for (int i = 0; i < statements.size() - 1; ++i) {
            if (statements.get(i) instanceof TryCatchStatement && statements.get(i + 1) instanceof TryCatchStatement) {
                TryCatchStatement first = (TryCatchStatement) statements.get(i);
                TryCatchStatement second = (TryCatchStatement) statements.get(i + 1);
                if (Objects.equals(first.getExceptionType(), second.getExceptionType())
                        && Objects.equals(first.getExceptionVariable(), second.getExceptionVariable())
                        && briefStatementComparison(first.getHandler(), second.getHandler())) {
                    first.getProtectedBody().addAll(second.getProtectedBody());
                    statements.remove(i + 1);
                    wieldTryCatch(first.getProtectedBody());
                    --i;
                }
            }
        }
    }

    private boolean briefStatementComparison(List<Statement> firstSeq, List<Statement> secondSeq) {
        if (firstSeq.isEmpty() && secondSeq.isEmpty()) {
            return true;
        }
        if (firstSeq.size() != 1 || secondSeq.size() != 1) {
            return false;
        }
        Statement first = firstSeq.get(0);
        Statement second = secondSeq.get(0);
        if (first instanceof BreakStatement && second instanceof BreakStatement) {
            BreakStatement firstBreak = (BreakStatement) first;
            BreakStatement secondBreak = (BreakStatement) second;
            return firstBreak.getTarget() == secondBreak.getTarget();
        }
        return false;
    }

    private static boolean matchArrayType(ValueType type, ArrayType arrayType) {
        switch (arrayType) {
            case BYTE:
                return type == ValueType.BYTE || type == ValueType.BOOLEAN;
            case SHORT:
                return type == ValueType.SHORT;
            case CHAR:
                return type == ValueType.CHARACTER;
            case INT:
                return type == ValueType.INTEGER;
            case LONG:
                return type == ValueType.LONG;
            case FLOAT:
                return type == ValueType.FLOAT;
            case DOUBLE:
                return type == ValueType.DOUBLE;
            case OBJECT:
                return type instanceof ValueType.Object || type instanceof ValueType.Array;
            default:
                return false;
        }
    }

    private void eliminateRedundantBreaks(List<Statement> statements, IdentifiedStatement exit) {
        if (statements.isEmpty()) {
            return;
        }
        Statement last = statements.get(statements.size() - 1);
        if (last instanceof BreakStatement && exit != null) {
            IdentifiedStatement target = ((BreakStatement) last).getTarget();
            if (exit == target) {
                statements.remove(statements.size() - 1);
            }
        }
        if (statements.isEmpty()) {
            return;
        }

        boolean shouldOptimizeBreaks = !hitsRedundantBreakThreshold(statements, exit);

        for (int i = 0; i < statements.size(); ++i) {
            Statement stmt = statements.get(i);
            if (stmt instanceof ConditionalStatement) {
                ConditionalStatement cond = (ConditionalStatement) stmt;
                check_conditional: if (shouldOptimizeBreaks) {
                    last = cond.getConsequent().isEmpty() ? null
                            : cond.getConsequent().get(cond.getConsequent().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement) last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            cond.getConsequent().remove(cond.getConsequent().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getAlternative().addAll(remaining);
                            remaining.clear();
                            break check_conditional;
                        }
                    }
                    last = cond.getAlternative().isEmpty() ? null
                            : cond.getAlternative().get(cond.getAlternative().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement) last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            cond.getAlternative().remove(cond.getAlternative().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getConsequent().addAll(remaining);
                            remaining.clear();
                        }
                    }
                }
                if (i == statements.size() - 1) {
                    eliminateRedundantBreaks(cond.getConsequent(), exit);
                    eliminateRedundantBreaks(cond.getAlternative(), exit);
                }
                normalizeConditional(cond);
                if (cond.getConsequent().size() == 1 && cond.getConsequent().get(0) instanceof ConditionalStatement) {
                    ConditionalStatement innerCond = (ConditionalStatement) cond.getConsequent().get(0);
                    if (innerCond.getAlternative().isEmpty()) {
                        if (cond.getAlternative().isEmpty()) {
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(innerCond.getConsequent());
                            cond.setCondition(Expr.binary(BinaryOperation.AND, null, cond.getCondition(),
                                    innerCond.getCondition(), cond.getCondition().getLocation()));
                            --i;
                        } else if (cond.getAlternative().size() != 1
                                || !(cond.getAlternative().get(0) instanceof ConditionalStatement)) {
                            cond.setCondition(ExprOptimizer.invert(cond.getCondition()));
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(cond.getAlternative());
                            cond.getAlternative().clear();
                            cond.getAlternative().add(innerCond);
                            --i;
                        }
                    }
                }
            } else if (stmt instanceof BlockStatement) {
                BlockStatement nestedBlock = (BlockStatement) stmt;
                eliminateRedundantBreaks(nestedBlock.getBody(), nestedBlock);
            } else if (stmt instanceof WhileStatement) {
                WhileStatement whileStmt = (WhileStatement) stmt;
                eliminateRedundantBreaks(whileStmt.getBody(), null);
            } else if (stmt instanceof SwitchStatement) {
                SwitchStatement switchStmt = (SwitchStatement) stmt;
                for (SwitchClause clause : switchStmt.getClauses()) {
                    eliminateRedundantBreaks(clause.getBody(), null);
                }
                eliminateRedundantBreaks(switchStmt.getDefaultClause(), null);
            }
        }
    }

    private boolean hitsRedundantBreakThreshold(List<Statement> statements, IdentifiedStatement exit) {
        int count = 0;
        for (int i = 0; i < statements.size(); ++i) {
            Statement stmt = statements.get(i);
            if (!(stmt instanceof ConditionalStatement)) {
                continue;
            }

            ConditionalStatement conditional = (ConditionalStatement) stmt;
            if (!conditional.getConsequent().isEmpty() && !conditional.getAlternative().isEmpty()) {
                continue;
            }
            List<Statement> innerStatements = !conditional.getConsequent().isEmpty()
                    ? conditional.getConsequent() : conditional.getAlternative();
            if (innerStatements.isEmpty()) {
                continue;
            }

            Statement last = innerStatements.get(innerStatements.size() - 1);
            if (!(last instanceof BreakStatement)) {
                continue;
            }

            BreakStatement breakStmt = (BreakStatement) last;
            if (exit != null && exit == breakStmt.getTarget() && ++count == 8) {
                return true;
            }
        }

        return false;
    }

    private void normalizeConditional(ConditionalStatement stmt) {
        if (stmt.getConsequent().isEmpty()) {
            if (stmt.getAlternative().isEmpty()) {
                return;
            }
            stmt.getConsequent().addAll(stmt.getAlternative());
            stmt.getAlternative().clear();
            stmt.setCondition(ExprOptimizer.invert(stmt.getCondition()));
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        List<Statement> statements = processSequence(statement.getSequence());
        if (statements.size() == 1) {
            resultStmt = statements.get(0);
        } else {
            statement.getSequence().clear();
            statement.getSequence().addAll(statements);
            resultStmt = statement;
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        statement.setCondition(optimizeCondition(resultExpr));
        List<Statement> consequent = processSequence(statement.getConsequent());
        List<Statement> alternative = processSequence(statement.getAlternative());
        if (consequent.isEmpty()) {
            consequent.addAll(alternative);
            alternative.clear();
            statement.setCondition(ExprOptimizer.invert(statement.getCondition()));
        }
        if (consequent.isEmpty()) {
            SequentialStatement sequentialStatement = new SequentialStatement();
            resultStmt = sequentialStatement;
            statement.getCondition().acceptVisitor(new ExpressionSideEffectDecomposer(
                    sequentialStatement.getSequence()));
            return;
        }
        statement.getConsequent().clear();
        statement.getConsequent().addAll(consequent);
        statement.getAlternative().clear();
        statement.getAlternative().addAll(alternative);

        Statement asConditional = tryConvertToConditionalExpression(statement);
        if (asConditional != null) {
            asConditional.acceptVisitor(this);
        } else {
            resultStmt = statement;
        }
    }

    private Statement tryConvertToConditionalExpression(ConditionalStatement statement) {
        if (statement.getConsequent().size() != 1 || statement.getAlternative().size() != 1) {
            return null;
        }

        Statement first = statement.getConsequent().get(0);
        Statement second = statement.getAlternative().get(0);
        if (!(first instanceof AssignmentStatement) || !(second instanceof AssignmentStatement)) {
            return null;
        }

        AssignmentStatement firstAssignment = (AssignmentStatement) first;
        AssignmentStatement secondAssignment = (AssignmentStatement) second;
        if (firstAssignment.getLeftValue() == null || secondAssignment.getRightValue() == null) {
            return null;
        }
        if (firstAssignment.isAsync() || secondAssignment.isAsync()) {
            return null;
        }

        if (!(firstAssignment.getLeftValue() instanceof VariableExpr)
                || !(secondAssignment.getLeftValue() instanceof VariableExpr)) {
            return null;
        }
        VariableExpr firstLhs = (VariableExpr) firstAssignment.getLeftValue();
        VariableExpr secondLhs = (VariableExpr) secondAssignment.getLeftValue();
        if (firstLhs.getIndex() == secondLhs.getIndex()) {
            ConditionalExpr conditionalExpr = new ConditionalExpr();
            conditionalExpr.setCondition(statement.getCondition());
            conditionalExpr.setConsequent(firstAssignment.getRightValue());
            conditionalExpr.setAlternative(secondAssignment.getRightValue());
            conditionalExpr.setLocation(statement.getCondition().getLocation());
            AssignmentStatement assignment = new AssignmentStatement();
            assignment.setLocation(conditionalExpr.getLocation());
            VariableExpr lhs = new VariableExpr();
            lhs.setIndex(firstLhs.getIndex());
            assignment.setLeftValue(lhs);
            assignment.setRightValue(conditionalExpr);
            writeFrequencies[lhs.getIndex()]--;
            return assignment;
        }

        return null;
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        statement.setValue(resultExpr);
        for (SwitchClause clause : statement.getClauses()) {
            List<Statement> newBody = processSequence(clause.getBody());
            clause.getBody().clear();
            clause.getBody().addAll(newBody);
        }
        List<Statement> newDefault = processSequence(statement.getDefaultClause());
        statement.getDefaultClause().clear();
        statement.getDefaultClause().addAll(newDefault);

        if (statement.getClauses().isEmpty()) {
            SequentialStatement seq = new SequentialStatement();
            seq.getSequence().addAll(statement.getDefaultClause());
            resultStmt = seq;
        } else {
            resultStmt = statement;
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getBody().size() == 1 && statement.getBody().get(0) instanceof WhileStatement) {
            WhileStatement innerLoop = (WhileStatement) statement.getBody().get(0);
            BreakToContinueReplacer replacer = new BreakToContinueReplacer(innerLoop, statement);
            replacer.visit(innerLoop.getBody());
            statement.getBody().clear();
            statement.getBody().addAll(innerLoop.getBody());
        }
        List<Statement> statements = processSequence(statement.getBody());
        for (int i = 0; i < statements.size(); ++i) {
            if (statements.get(i) instanceof ContinueStatement) {
                ContinueStatement continueStmt = (ContinueStatement) statements.get(i);
                if (continueStmt.getTarget() == statement) {
                    statements.subList(i, statements.size()).clear();
                    break;
                }
            }
        }
        statement.getBody().clear();
        statement.getBody().addAll(statements);
        if (statement.getCondition() != null) {
            List<Statement> sequenceBackup = resultSequence;
            resultSequence = new ArrayList<>();
            statement.getCondition().acceptVisitor(this);
            statement.setCondition(resultExpr);
            resultSequence = sequenceBackup;
        }
        while (true) {
            if (!statement.getBody().isEmpty() && statement.getBody().get(0) instanceof ConditionalStatement) {
                ConditionalStatement cond = (ConditionalStatement) statement.getBody().get(0);
                if (cond.getConsequent().size() == 1 && cond.getConsequent().get(0) instanceof BreakStatement) {
                    BreakStatement breakStmt = (BreakStatement) cond.getConsequent().get(0);
                    if (breakStmt.getTarget() == statement) {
                        statement.getBody().remove(0);
                        if (statement.getCondition() != null) {
                            Expr newCondition = Expr.binary(BinaryOperation.AND, null, statement.getCondition(),
                                    ExprOptimizer.invert(cond.getCondition()));
                            newCondition.setLocation(statement.getCondition().getLocation());
                            statement.setCondition(newCondition);
                        } else {
                            statement.setCondition(ExprOptimizer.invert(cond.getCondition()));
                        }
                        continue;
                    }
                }
            }
            break;
        }
        resultStmt = statement;
    }

    @Override
    public void visit(BlockStatement statement) {
        List<Statement> statements = processSequence(statement.getBody());
        eliminateRedundantBreaks(statements, statement);
        BlockCountVisitor usageCounter = new BlockCountVisitor(statement);
        usageCounter.visit(statements);
        if (usageCounter.getCount() == 0) {
            SequentialStatement result = new SequentialStatement();
            result.getSequence().addAll(statements);
            resultStmt = result;
        } else {
            statement.getBody().clear();
            statement.getBody().addAll(statements);
            resultStmt = statement;
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(ContinueStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(ReturnStatement statement) {
        pushLocation(statement.getLocation());
        try {
            if (statement.getResult() != null) {
                statement.getResult().acceptVisitor(this);
                statement.setResult(resultExpr);
            }
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getException().acceptVisitor(this);
            statement.setException(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InitClassStatement statement) {
        pushLocation(statement.getLocation());
        try {
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(TryCatchStatement statement) {
        List<Statement> statements = processSequence(statement.getProtectedBody());
        statement.getProtectedBody().clear();
        statement.getProtectedBody().addAll(statements);
        statements = processSequence(statement.getHandler());
        statement.getHandler().clear();
        statement.getHandler().addAll(statements);
        resultStmt = statement;
    }

    @Override
    public void visit(GotoPartStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getObjectRef().acceptVisitor(this);
            statement.setObjectRef(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getObjectRef().acceptVisitor(this);
            statement.setObjectRef(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getIndex().acceptVisitor(this);
            Expr index = resultExpr;

            Expr array = null;
            if (expr.getArray() != null) {
                expr.getArray().acceptVisitor(this);
                array = resultExpr;
            }

            expr.setIndex(index);
            expr.setArray(array);

            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    private Statement addBarrier() {
        SequentialStatement barrier = new SequentialStatement();
        resultSequence.add(barrier);
        return barrier;
    }

    private void removeBarrier(Statement barrier) {
        Statement removedBarrier = resultSequence.remove(resultSequence.size() - 1);
        if (removedBarrier != barrier) {
            throw new AssertionError();
        }
    }

    private boolean isSideEffectFree(Expr expr) {
        if (expr == null) {
            return true;
        }

        if (expr instanceof VariableExpr || expr instanceof ConstantExpr) {
            return true;
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            return isSideEffectFree(binary.getFirstOperand()) && isSideEffectFree(binary.getSecondOperand());
        }

        if (expr instanceof UnaryExpr) {
            return isSideEffectFree(((UnaryExpr) expr).getOperand());
        }

        if (expr instanceof InstanceOfExpr) {
            return isSideEffectFree(((InstanceOfExpr) expr).getExpr());
        }

        if (expr instanceof PrimitiveCastExpr) {
            return isSideEffectFree(((PrimitiveCastExpr) expr).getValue());
        }

        if (expr instanceof NewExpr) {
            return true;
        }

        return false;
    }

    private Expr optimizeCondition(Expr expr) {
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (isZero(((BinaryExpr) expr).getSecondOperand())) {
                switch (binary.getOperation()) {
                    case EQUALS:
                        return ExprOptimizer.invert(binary.getFirstOperand());
                    case NOT_EQUALS:
                        return binary.getFirstOperand();
                }
            }
        }
        return expr;
    }

    static class ArrayOptimization {
        int index;
        NewArrayExpr array;
        int arrayVariable;
        UnwrapArrayExpr unwrappedArray;
        int unwrappedArrayVariable;
        int arrayElementIndex;
        int arraySize;
        List<Expr> elements = new ArrayList<>();
    }
}
