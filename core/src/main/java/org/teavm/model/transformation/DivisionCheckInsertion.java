/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.model.transformation;

import static org.teavm.model.transformation.IntegerRanges.EMPTY;
import static org.teavm.model.transformation.IntegerRanges.INT_FULL;
import com.carrotsearch.hppc.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.util.DominatorWalker;
import org.teavm.model.util.DominatorWalkerCallback;
import org.teavm.model.util.DominatorWalkerContext;

/**
 * Replaces integer {@link BinaryOperation#DIVIDE} and {@link BinaryOperation#MODULO} with
 * {@link BinaryOperation#DIVIDE_CHECKED} and {@link BinaryOperation#MODULO_CHECKED} correspondingly, unless
 * it can prove that divisor is never zero and that the division never overflows
 * (i.e. it's never {@code MIN_VALUE / -1}).
 *
 * <p>To prove this, the transformation computes a set of possible values for each integer variable.
 * The set is represented as a union of a limited number of closed ranges (see {@link IntegerRanges}).
 * Ranges are computed in a single walk over the dominator tree from definitions of variables,
 * intersected against conditions of branches when entering conditional blocks and united in phis.
 * Phi inputs that are not computed yet when visiting a phi (i.e. ones that come via back edges of loops)
 * are considered unknown, which makes the whole phi unknown.</p>
 */
public class DivisionCheckInsertion {
    private static final MethodReference AE_CONSTRUCTOR = new MethodReference(ArithmeticException.class,
            "<init>", void.class);

    public static void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        dependencyAnalyzer.setDivisionChecked(true);
        dependencyAnalyzer.linkMethod(AE_CONSTRUCTOR)
                .propagateClass(0, ArithmeticException.class.getName())
                .use();
    }

    public void transformProgram(Program program, MethodReference methodReference) {
        if (program.basicBlockCount() == 0 || !hasIntegerDivisions(program)) {
            return;
        }
        new Analysis(program).run();
    }

    private static boolean hasIntegerDivisions(Program program) {
        for (var block : program.getBasicBlocks()) {
            for (var insn : block) {
                if (insn instanceof BinaryInstruction) {
                    var binary = (BinaryInstruction) insn;
                    if (isDivision(binary.getOperation()) && isInteger(binary.getOperandType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isDivision(BinaryOperation op) {
        switch (op) {
            case DIVIDE:
            case MODULO:
            case DIVIDE_CHECKED:
            case MODULO_CHECKED:
                return true;
            default:
                return false;
        }
    }

    private static boolean isInteger(NumericOperandType type) {
        return type == NumericOperandType.INT || type == NumericOperandType.LONG;
    }

    private enum Relation {
        LT, LE, GT, GE, EQ, NE;

        Relation negate() {
            switch (this) {
                case LT:
                    return GE;
                case LE:
                    return GT;
                case GT:
                    return LE;
                case GE:
                    return LT;
                case EQ:
                    return NE;
                case NE:
                    return EQ;
            }
            throw new AssertionError();
        }

        Relation swap() {
            switch (this) {
                case LT:
                    return GT;
                case LE:
                    return GE;
                case GT:
                    return LT;
                case GE:
                    return LE;
                default:
                    return this;
            }
        }
    }

    static class Undo {
        IntArrayList variables = new IntArrayList();
        List<long[]> values = new ArrayList<>();
    }

    static class Constraints {
        IntArrayList variables = new IntArrayList();
        List<long[]> values = new ArrayList<>();

        void add(int variable, long[] value) {
            for (int i = 0; i < variables.size(); ++i) {
                if (variables.get(i) == variable) {
                    values.set(i, IntegerRanges.intersect(values.get(i), value));
                    return;
                }
            }
            variables.add(variable);
            values.add(value);
        }

        long[] apply(int variable, long[] value) {
            for (int i = 0; i < variables.size(); ++i) {
                if (variables.get(i) == variable) {
                    value = IntegerRanges.intersect(value, values.get(i));
                }
            }
            return value;
        }
    }

    static class Analysis extends AbstractInstructionVisitor implements DominatorWalkerCallback<Undo> {
        private Program program;
        private DominatorWalker walker;
        private DominatorWalkerContext context;

        // Ranges of variables in the current context, i.e. taking into account branch conditions
        private long[][] ranges;
        // Blocks whose instructions are already processed, so definitions of their variables are known
        private boolean[] entered;
        private int[] comparisonLeft;
        private int[] comparisonRight;
        private NumericOperandType[] comparisonType;

        private Undo undo;

        Analysis(Program program) {
            this.program = program;
            walker = new DominatorWalker(program);
            int count = program.variableCount();
            ranges = new long[count][];
            entered = new boolean[program.basicBlockCount()];
            comparisonLeft = new int[count];
            Arrays.fill(comparisonLeft, -1);
            comparisonRight = new int[count];
            comparisonType = new NumericOperandType[count];
        }

        void run() {
            walker.walk(this);
        }

        @Override
        public void setContext(DominatorWalkerContext context) {
            this.context = context;
        }

        @Override
        public Undo visit(BasicBlock block) {
            undo = new Undo();

            int predecessor = singlePredecessor(block);
            if (predecessor >= 0) {
                var constraints = edgeConstraints(program.basicBlockAt(predecessor), block);
                if (constraints != null) {
                    for (int i = 0; i < constraints.variables.size(); ++i) {
                        int variable = constraints.variables.get(i);
                        refine(variable, IntegerRanges.intersect(ranges[variable], constraints.values.get(i)));
                    }
                }
            }

            for (var phi : block.getPhis()) {
                define(phi.getReceiver().getIndex(), evaluatePhi(phi));
            }
            entered[block.getIndex()] = true;

            Instruction next;
            for (var insn = block.getFirstInstruction(); insn != null; insn = next) {
                next = insn.getNext();
                insn.acceptVisitor(this);
            }

            return undo;
        }

        private int singlePredecessor(BasicBlock block) {
            int predecessor = context.getSinglePredecessor(block.getIndex());
            if (predecessor >= 0) {
                return predecessor;
            }

            // Switch may have multiple edges to the same block, that's fine, since constraints
            // for switch take all of them into account
            var predecessors = context.getControlFlowGraph().incomingEdges(block.getIndex() * 2);
            if (predecessors.length == 1 && predecessors[0] % 2 == 1) {
                predecessor = predecessors[0] / 2;
                if (program.basicBlockAt(predecessor).getLastInstruction() instanceof SwitchInstruction) {
                    return predecessor;
                }
            }
            return -1;
        }

        @Override
        public void endVisit(BasicBlock block, Undo state) {
            for (int i = state.variables.size() - 1; i >= 0; --i) {
                ranges[state.variables.get(i)] = state.values.get(i);
            }
        }

        private void define(int variable, long[] value) {
            ranges[variable] = value;
        }

        private void refine(int variable, long[] value) {
            if (IntegerRanges.equals(ranges[variable], value)) {
                return;
            }
            undo.variables.add(variable);
            undo.values.add(ranges[variable]);
            ranges[variable] = value;
        }

        private long[] get(Variable variable, NumericOperandType type) {
            return get(variable.getIndex(), type);
        }

        private long[] get(int variable, NumericOperandType type) {
            var value = ranges[variable];
            return type == NumericOperandType.INT ? IntegerRanges.intersect(value, INT_FULL) : value;
        }

        private long[] evaluatePhi(Phi phi) {
            var block = phi.getBasicBlock();
            long[] result = EMPTY;
            for (Incoming incoming : phi.getIncomings()) {
                var source = incoming.getSource();
                if (!entered[source.getIndex()]) {
                    // Back edge of a loop (or irreducible control flow), the input is not known yet
                    return null;
                }

                // Variable is defined in a block that dominates the source, so it is already computed.
                // Current context is the context of immediate dominator of the phi's block,
                // which also dominates the source, so all refinements in current context hold in the source.
                int value = incoming.getValue().getIndex();
                var range = ranges[value];
                if (!isExceptionHandler(source, block)) {
                    var constraints = edgeConstraints(source, block);
                    if (constraints != null) {
                        range = constraints.apply(value, range);
                    }
                }
                result = IntegerRanges.union(result, range);
                if (result == null) {
                    break;
                }
            }
            return result;
        }

        private static boolean isExceptionHandler(BasicBlock source, BasicBlock target) {
            for (TryCatchBlock tryCatch : source.getTryCatchBlocks()) {
                if (tryCatch.getHandler() == target) {
                    return true;
                }
            }
            return false;
        }

        private Constraints edgeConstraints(BasicBlock source, BasicBlock target) {
            var last = source.getLastInstruction();
            if (last instanceof BranchingInstruction) {
                return branchingConstraints((BranchingInstruction) last, target);
            } else if (last instanceof BinaryBranchingInstruction) {
                return binaryBranchingConstraints((BinaryBranchingInstruction) last, target);
            } else if (last instanceof SwitchInstruction) {
                return switchConstraints((SwitchInstruction) last, target);
            }
            return null;
        }

        private Constraints branchingConstraints(BranchingInstruction insn, BasicBlock target) {
            if (insn.getConsequent() == insn.getAlternative()) {
                return null;
            }
            Relation relation;
            switch (insn.getCondition()) {
                case EQUAL:
                    relation = Relation.EQ;
                    break;
                case NOT_EQUAL:
                    relation = Relation.NE;
                    break;
                case LESS:
                    relation = Relation.LT;
                    break;
                case LESS_OR_EQUAL:
                    relation = Relation.LE;
                    break;
                case GREATER:
                    relation = Relation.GT;
                    break;
                case GREATER_OR_EQUAL:
                    relation = Relation.GE;
                    break;
                default:
                    return null;
            }
            if (target != insn.getConsequent()) {
                relation = relation.negate();
            }

            var constraints = new Constraints();
            int operand = insn.getOperand().getIndex();
            var zero = IntegerRanges.point(0);
            constraints.add(operand, refine(relation, get(operand, NumericOperandType.INT), zero));

            int left = comparisonLeft[operand];
            if (left >= 0) {
                int right = comparisonRight[operand];
                var type = comparisonType[operand];
                var leftRange = get(left, type);
                var rightRange = get(right, type);
                constraints.add(left, refine(relation, leftRange, rightRange));
                constraints.add(right, refine(relation.swap(), rightRange, leftRange));
            }
            return constraints;
        }

        private Constraints binaryBranchingConstraints(BinaryBranchingInstruction insn, BasicBlock target) {
            if (insn.getConsequent() == insn.getAlternative()) {
                return null;
            }
            Relation relation;
            switch (insn.getCondition()) {
                case EQUAL:
                    relation = Relation.EQ;
                    break;
                case NOT_EQUAL:
                    relation = Relation.NE;
                    break;
                default:
                    return null;
            }
            if (target != insn.getConsequent()) {
                relation = relation.negate();
            }
            int first = insn.getFirstOperand().getIndex();
            int second = insn.getSecondOperand().getIndex();
            var firstRange = get(first, NumericOperandType.INT);
            var secondRange = get(second, NumericOperandType.INT);
            var constraints = new Constraints();
            constraints.add(first, refine(relation, firstRange, secondRange));
            constraints.add(second, refine(relation, secondRange, firstRange));
            return constraints;
        }

        private Constraints switchConstraints(SwitchInstruction insn, BasicBlock target) {
            int condition = insn.getCondition().getIndex();
            var range = get(condition, NumericOperandType.INT);
            if (target == insn.getDefaultTarget()) {
                for (SwitchTableEntry entry : insn.getEntries()) {
                    if (entry.getTarget() == target) {
                        return null;
                    }
                }
                for (SwitchTableEntry entry : insn.getEntries()) {
                    range = IntegerRanges.exclude(range, entry.getCondition());
                }
            } else {
                long[] values = EMPTY;
                for (SwitchTableEntry entry : insn.getEntries()) {
                    if (entry.getTarget() == target) {
                        values = IntegerRanges.union(values, IntegerRanges.point(entry.getCondition()));
                    }
                }
                range = IntegerRanges.intersect(range, values);
            }
            var constraints = new Constraints();
            constraints.add(condition, range);
            return constraints;
        }

        /**
         * Given that {@code a relation b} holds, computes the subset of {@code a}.
         */
        private static long[] refine(Relation relation, long[] a, long[] b) {
            if (IntegerRanges.isEmpty(b)) {
                return EMPTY;
            }
            switch (relation) {
                case LT: {
                    long max = IntegerRanges.max(b);
                    return max == Long.MIN_VALUE ? EMPTY : IntegerRanges.intersect(a, Long.MIN_VALUE, max - 1);
                }
                case LE:
                    return IntegerRanges.intersect(a, Long.MIN_VALUE, IntegerRanges.max(b));
                case GT: {
                    long min = IntegerRanges.min(b);
                    return min == Long.MAX_VALUE ? EMPTY : IntegerRanges.intersect(a, min + 1, Long.MAX_VALUE);
                }
                case GE:
                    return IntegerRanges.intersect(a, IntegerRanges.min(b), Long.MAX_VALUE);
                case EQ:
                    return IntegerRanges.intersect(a, b);
                case NE:
                    return IntegerRanges.isPoint(b) ? IntegerRanges.exclude(a, b[0]) : a;
            }
            throw new AssertionError();
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            define(insn.getReceiver().getIndex(), IntegerRanges.point(insn.getConstant()));
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            define(insn.getReceiver().getIndex(), IntegerRanges.point(insn.getConstant()));
        }

        @Override
        public void visit(AssignInstruction insn) {
            define(insn.getReceiver().getIndex(), ranges[insn.getAssignee().getIndex()]);
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            define(insn.getReceiver().getIndex(), IntegerRanges.of(0, Integer.MAX_VALUE));
        }

        @Override
        public void visit(BoundCheckInstruction insn) {
            var range = get(insn.getIndex(), NumericOperandType.INT);
            if (insn.isLower()) {
                range = IntegerRanges.intersect(range, 0, Long.MAX_VALUE);
            }
            if (insn.getArray() != null) {
                range = IntegerRanges.intersect(range, Long.MIN_VALUE, Integer.MAX_VALUE - 1);
            }
            define(insn.getReceiver().getIndex(), range);
            if (insn.getReceiver() != insn.getIndex()) {
                refine(insn.getIndex().getIndex(), range);
            }
        }

        @Override
        public void visit(NegateInstruction insn) {
            if (!isInteger(insn.getOperandType())) {
                define(insn.getReceiver().getIndex(), null);
                return;
            }
            var type = insn.getOperandType();
            define(insn.getReceiver().getIndex(), negate(get(insn.getOperand(), type), type));
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            long[] result;
            switch (insn.getTargetType()) {
                case INT:
                    switch (insn.getSourceType()) {
                        case INT:
                        case LONG: {
                            var source = get(insn.getValue(), insn.getSourceType());
                            result = IntegerRanges.isSubset(source, Integer.MIN_VALUE, Integer.MAX_VALUE)
                                    ? source
                                    : INT_FULL;
                            break;
                        }
                        default:
                            result = INT_FULL;
                            break;
                    }
                    break;
                case LONG:
                    switch (insn.getSourceType()) {
                        case INT:
                        case LONG:
                            result = get(insn.getValue(), insn.getSourceType());
                            break;
                        default:
                            result = null;
                            break;
                    }
                    break;
                default:
                    result = null;
                    break;
            }
            define(insn.getReceiver().getIndex(), result);
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            var subtypeRange = subtypeRange(insn.getTargetType());
            var source = get(insn.getValue(), NumericOperandType.INT);
            long[] result;
            if (insn.getDirection() == CastIntegerDirection.FROM_INTEGER) {
                result = IntegerRanges.isSubset(source, subtypeRange[0], subtypeRange[1]) ? source : subtypeRange;
            } else {
                result = IntegerRanges.intersect(source, subtypeRange);
            }
            define(insn.getReceiver().getIndex(), result);
        }

        private static long[] subtypeRange(IntegerSubtype subtype) {
            switch (subtype) {
                case BYTE:
                    return IntegerRanges.of(Byte.MIN_VALUE, Byte.MAX_VALUE);
                case SHORT:
                    return IntegerRanges.of(Short.MIN_VALUE, Short.MAX_VALUE);
                case CHAR:
                    return IntegerRanges.of(Character.MIN_VALUE, Character.MAX_VALUE);
            }
            throw new AssertionError();
        }

        @Override
        public void visit(BinaryInstruction insn) {
            int receiver = insn.getReceiver().getIndex();
            var type = insn.getOperandType();
            var op = insn.getOperation();
            if (!isInteger(type)) {
                define(receiver, op == BinaryOperation.COMPARE_LESS || op == BinaryOperation.COMPARE_GREATER
                        ? IntegerRanges.of(-1, 1)
                        : null);
                return;
            }

            var a = get(insn.getFirstOperand(), type);
            var b = get(insn.getSecondOperand(), type);
            if (op == BinaryOperation.COMPARE_LESS || op == BinaryOperation.COMPARE_GREATER) {
                comparisonLeft[receiver] = insn.getFirstOperand().getIndex();
                comparisonRight[receiver] = insn.getSecondOperand().getIndex();
                comparisonType[receiver] = type;
            }

            if (isDivision(op)) {
                long min = type == NumericOperandType.INT ? Integer.MIN_VALUE : Long.MIN_VALUE;
                boolean needsCheck = IntegerRanges.isEmpty(b) || IntegerRanges.contains(b, 0)
                        || (IntegerRanges.contains(b, -1) && IntegerRanges.contains(a, min));
                updateDivision(insn, needsCheck);
            }

            define(receiver, compute(op, type, a, b));

            if (isDivision(op)) {
                // Normal execution after this instruction is only possible if divisor is non-zero
                refine(insn.getSecondOperand().getIndex(), IntegerRanges.exclude(b, 0));
            }
        }

        private void updateDivision(BinaryInstruction insn, boolean needsCheck) {
            BinaryOperation newOp;
            switch (insn.getOperation()) {
                case DIVIDE:
                case DIVIDE_CHECKED:
                    newOp = needsCheck ? BinaryOperation.DIVIDE_CHECKED : BinaryOperation.DIVIDE;
                    break;
                case MODULO:
                case MODULO_CHECKED:
                    newOp = needsCheck ? BinaryOperation.MODULO_CHECKED : BinaryOperation.MODULO;
                    break;
                default:
                    return;
            }
            if (newOp == insn.getOperation()) {
                return;
            }
            var replacement = new BinaryInstruction(newOp, insn.getOperandType());
            replacement.setFirstOperand(insn.getFirstOperand());
            replacement.setSecondOperand(insn.getSecondOperand());
            replacement.setReceiver(insn.getReceiver());
            replacement.setLocation(insn.getLocation());
            insn.replace(replacement);
        }

        private static long[] compute(BinaryOperation op, NumericOperandType type, long[] a, long[] b) {
            if (IntegerRanges.isEmpty(a) || IntegerRanges.isEmpty(b)) {
                return EMPTY;
            }
            long[] result;
            switch (op) {
                case ADD:
                    result = add(a, b);
                    break;
                case SUBTRACT:
                    result = subtract(a, b);
                    break;
                case MULTIPLY:
                    result = multiply(a, b);
                    break;
                case DIVIDE:
                case DIVIDE_CHECKED:
                    result = divide(a, b, type);
                    break;
                case MODULO:
                case MODULO_CHECKED:
                    result = remainder(a, b, type);
                    break;
                case COMPARE_LESS:
                case COMPARE_GREATER:
                    return IntegerRanges.of(-1, 1);
                case AND:
                    result = and(a, b);
                    break;
                case OR:
                case XOR:
                    result = orXor(a, b);
                    break;
                case SHIFT_RIGHT:
                    result = shiftRight(a, b, type);
                    break;
                case SHIFT_RIGHT_UNSIGNED:
                    result = shiftRightUnsigned(a, b, type);
                    break;
                default:
                    result = null;
                    break;
            }
            if (type == NumericOperandType.INT) {
                // When result exceeds int range, it wraps around, so we can't say anything about it
                if (!IntegerRanges.isEmpty(result)
                        && !IntegerRanges.isSubset(result, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
                    result = INT_FULL;
                }
            }
            return result;
        }

        private static long[] bounds(long[] ranges) {
            return ranges != null ? ranges : new long[] { Long.MIN_VALUE, Long.MAX_VALUE };
        }

        private static long[] add(long[] a, long[] b) {
            a = bounds(a);
            b = bounds(b);
            var result = new long[a.length * b.length];
            int size = 0;
            try {
                for (int i = 0; i < a.length; i += 2) {
                    for (int j = 0; j < b.length; j += 2) {
                        result[size++] = Math.addExact(a[i], b[j]);
                        result[size++] = Math.addExact(a[i + 1], b[j + 1]);
                    }
                }
            } catch (ArithmeticException e) {
                return null;
            }
            return IntegerRanges.normalize(result, size);
        }

        private static long[] subtract(long[] a, long[] b) {
            a = bounds(a);
            b = bounds(b);
            var result = new long[a.length * b.length];
            int size = 0;
            try {
                for (int i = 0; i < a.length; i += 2) {
                    for (int j = 0; j < b.length; j += 2) {
                        result[size++] = Math.subtractExact(a[i], b[j + 1]);
                        result[size++] = Math.subtractExact(a[i + 1], b[j]);
                    }
                }
            } catch (ArithmeticException e) {
                return null;
            }
            return IntegerRanges.normalize(result, size);
        }

        private static long[] multiply(long[] a, long[] b) {
            a = bounds(a);
            b = bounds(b);
            var result = new long[a.length * b.length];
            int size = 0;
            try {
                for (int i = 0; i < a.length; i += 2) {
                    for (int j = 0; j < b.length; j += 2) {
                        long p1 = Math.multiplyExact(a[i], b[j]);
                        long p2 = Math.multiplyExact(a[i], b[j + 1]);
                        long p3 = Math.multiplyExact(a[i + 1], b[j]);
                        long p4 = Math.multiplyExact(a[i + 1], b[j + 1]);
                        result[size++] = Math.min(Math.min(p1, p2), Math.min(p3, p4));
                        result[size++] = Math.max(Math.max(p1, p2), Math.max(p3, p4));
                    }
                }
            } catch (ArithmeticException e) {
                return null;
            }
            return IntegerRanges.normalize(result, size);
        }

        private static long[] negate(long[] a, NumericOperandType type) {
            if (IntegerRanges.isEmpty(a)) {
                return EMPTY;
            }
            long min = type == NumericOperandType.INT ? Integer.MIN_VALUE : Long.MIN_VALUE;
            if (IntegerRanges.contains(a, min)) {
                return type == NumericOperandType.INT ? INT_FULL : null;
            }
            var result = new long[a.length];
            for (int i = 0; i < a.length; i += 2) {
                result[i] = -a[i + 1];
                result[i + 1] = -a[i];
            }
            return IntegerRanges.normalize(result, result.length);
        }

        private static long[] divide(long[] a, long[] b, NumericOperandType type) {
            long typeMin = type == NumericOperandType.INT ? Integer.MIN_VALUE : Long.MIN_VALUE;
            if (IntegerRanges.isPoint(b) && b[0] != 0) {
                long divisor = b[0];
                if (divisor == -1) {
                    return negate(a, type);
                }
                a = bounds(a);
                var result = new long[a.length];
                for (int i = 0; i < a.length; i += 2) {
                    if (divisor > 0) {
                        result[i] = a[i] / divisor;
                        result[i + 1] = a[i + 1] / divisor;
                    } else {
                        result[i] = a[i + 1] / divisor;
                        result[i + 1] = a[i] / divisor;
                    }
                }
                return IntegerRanges.normalize(result, result.length);
            }

            // In general, |a / b| <= |a|, and the sign of result is defined by signs of operands
            long minA = IntegerRanges.min(a);
            long maxA = IntegerRanges.max(a);
            if (IntegerRanges.min(b) > 0) {
                return IntegerRanges.of(Math.min(minA, 0), Math.max(maxA, 0));
            }
            if (minA >= 0 && IntegerRanges.min(b) >= 0) {
                return IntegerRanges.of(0, maxA);
            }
            if (minA <= typeMin) {
                return null;
            }
            long abs = Math.max(Math.abs(minA), Math.abs(maxA));
            return IntegerRanges.of(-abs, abs);
        }

        private static long[] remainder(long[] a, long[] b, NumericOperandType type) {
            long typeMin = type == NumericOperandType.INT ? Integer.MIN_VALUE : Long.MIN_VALUE;
            long typeMax = type == NumericOperandType.INT ? Integer.MAX_VALUE : Long.MAX_VALUE;

            // |a % b| < |b|
            long minB = IntegerRanges.min(b);
            long maxB = IntegerRanges.max(b);
            long limit = minB <= typeMin ? typeMax : Math.max(Math.abs(minB), Math.abs(maxB)) - 1;
            limit = Math.max(limit, 0);

            // |a % b| <= |a|, and the sign of result is the sign of a
            long minA = IntegerRanges.min(a);
            long maxA = IntegerRanges.max(a);
            long lo = minA >= 0 ? 0 : Math.max(minA, -limit);
            long hi = maxA <= 0 ? 0 : Math.min(maxA, limit);
            return IntegerRanges.of(lo, hi);
        }

        private static long[] and(long[] a, long[] b) {
            boolean aNonNegative = IntegerRanges.min(a) >= 0;
            boolean bNonNegative = IntegerRanges.min(b) >= 0;
            if (aNonNegative && bNonNegative) {
                return IntegerRanges.of(0, Math.min(IntegerRanges.max(a), IntegerRanges.max(b)));
            } else if (aNonNegative) {
                return IntegerRanges.of(0, IntegerRanges.max(a));
            } else if (bNonNegative) {
                return IntegerRanges.of(0, IntegerRanges.max(b));
            }
            return null;
        }

        private static long[] orXor(long[] a, long[] b) {
            if (IntegerRanges.min(a) >= 0 && IntegerRanges.min(b) >= 0) {
                long max = Math.max(IntegerRanges.max(a), IntegerRanges.max(b));
                long mask = max == 0 ? 0 : (Long.highestOneBit(max) << 1) - 1;
                return IntegerRanges.of(0, mask);
            }
            return null;
        }

        private static long[] shiftRight(long[] a, long[] b, NumericOperandType type) {
            if (IntegerRanges.isPoint(b)) {
                int shift = (int) b[0] & (type == NumericOperandType.INT ? 31 : 63);
                a = bounds(a);
                var result = new long[a.length];
                for (int i = 0; i < a.length; ++i) {
                    result[i] = a[i] >> shift;
                }
                return IntegerRanges.normalize(result, result.length);
            }
            // Result is between a and 0
            return IntegerRanges.of(Math.min(IntegerRanges.min(a), 0), Math.max(IntegerRanges.max(a), 0));
        }

        private static long[] shiftRightUnsigned(long[] a, long[] b, NumericOperandType type) {
            boolean nonNegative = IntegerRanges.min(a) >= 0;
            if (IntegerRanges.isPoint(b)) {
                int shift = (int) b[0] & (type == NumericOperandType.INT ? 31 : 63);
                if (shift == 0) {
                    return a;
                }
                if (nonNegative) {
                    var result = new long[a.length];
                    for (int i = 0; i < a.length; ++i) {
                        result[i] = a[i] >>> shift;
                    }
                    return IntegerRanges.normalize(result, result.length);
                }
                long allBits = type == NumericOperandType.INT ? 0xFFFFFFFFL : -1L;
                return IntegerRanges.of(0, allBits >>> shift);
            }
            return nonNegative ? IntegerRanges.of(0, IntegerRanges.max(a)) : null;
        }
    }
}
