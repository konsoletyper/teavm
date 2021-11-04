/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.ast.decompilation.test;

import static org.junit.Assert.assertEquals;
import static org.teavm.ast.Expr.addInt;
import static org.teavm.ast.Expr.and;
import static org.teavm.ast.Expr.constant;
import static org.teavm.ast.Expr.divInt;
import static org.teavm.ast.Expr.invokeStatic;
import static org.teavm.ast.Expr.less;
import static org.teavm.ast.Expr.or;
import static org.teavm.ast.Expr.var;
import static org.teavm.ast.Statement.assign;
import static org.teavm.ast.Statement.block;
import static org.teavm.ast.Statement.cond;
import static org.teavm.ast.Statement.exitBlock;
import static org.teavm.ast.Statement.exitFunction;
import static org.teavm.ast.Statement.loopWhile;
import static org.teavm.ast.Statement.sequence;
import static org.teavm.ast.Statement.statementExpr;
import static org.teavm.ast.Statement.switchClause;
import static org.teavm.ast.Statement.switchStatement;
import static org.teavm.model.builder.ProgramBuilder.build;
import static org.teavm.model.builder.ProgramBuilder.exit;
import static org.teavm.model.builder.ProgramBuilder.ifLessThanZero;
import static org.teavm.model.builder.ProgramBuilder.intNum;
import static org.teavm.model.builder.ProgramBuilder.invokeStaticMethod;
import static org.teavm.model.builder.ProgramBuilder.jump;
import static org.teavm.model.builder.ProgramBuilder.label;
import static org.teavm.model.builder.ProgramBuilder.put;
import static org.teavm.model.builder.ProgramBuilder.set;
import static org.teavm.model.builder.ProgramBuilder.switchEntry;
import static org.teavm.model.builder.ProgramBuilder.tableSwitch;
import static org.teavm.model.builder.ProgramBuilder.var;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.teavm.ast.Statement;
import org.teavm.ast.decompilation.NewDecompiler;
import org.teavm.ast.util.AstPrinter;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.text.ListingBuilder;

public class NewDecompilerTest {
    private static final MethodReference PRINT = new MethodReference(NewDecompilerTest.class, "print", void.class);
    private static final MethodReference PRINT_2 = new MethodReference(NewDecompilerTest.class, "print2", void.class);
    private static final MethodReference PRINT_3 = new MethodReference(NewDecompilerTest.class, "print3", void.class);
    private static final MethodReference PRINT_4 = new MethodReference(NewDecompilerTest.class, "print4", void.class);
    private static final MethodReference PRINT_NUM = new MethodReference(NewDecompilerTest.class, "print",
            int.class, void.class);
    private static final MethodReference SUPPLY_INT_1 = new MethodReference(NewDecompilerTest.class,
            "supplyInt1", int.class);
    private static final MethodReference SUPPLY_INT_2 = new MethodReference(NewDecompilerTest.class,
            "supplyInt2", int.class);
    private AstPrinter astPrinter = new AstPrinter();
    private ListingBuilder listingBuilder = new ListingBuilder();
    private NewDecompiler decompiler = new NewDecompiler();
    private Program program;
    private Statement statement;

    @Test
    public void simple() {
        decompile(() -> {
            set(var("a")).constant(23);
            exit(var("a"));
        });
        expect(exitFunction(constant(23)));
    }

    @Test
    public void expression() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).constant(3);
            set(var("c")).add(intNum(), var("a"), var("b"));
            exit(var("c"));
        });
        expect(exitFunction(addInt(constant(2), constant(3))));
    }

    @Test
    public void complexExpression() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).constant(3);
            set(var("c")).constant(4);
            set(var("d")).add(intNum(), var("a"), var("b"));
            set(var("e")).add(intNum(), var("d"), var("c"));
            exit(var("e"));
        });
        expect(exitFunction(
                addInt(
                    addInt(constant(2), constant(3)),
                    constant(4)
                )
        ));
    }

    @Test
    public void sharedNonConstant() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).constant(3);
            set(var("c")).add(intNum(), var("a"), var("b"));
            set(var("d")).add(intNum(), var("c"), var("c"));
            exit(var("d"));
        });
        expect(sequence(
                assign(var(2), addInt(constant(2), constant(3))),
                exitFunction(addInt(var(2), var(2)))
        ));
    }

    @Test
    public void sharedConstant() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).add(intNum(), var("a"), var("a"));
            exit(var("b"));
        });
        expect(exitFunction(addInt(constant(2), constant(2))));
    }

    @Test
    public void relocatableOperationWithBarrier() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).constant(3);
            set(var("c")).add(intNum(), var("a"), var("b"));
            invokeStaticMethod(PRINT);
            exit(var("c"));
        });
        expect(sequence(
                statementExpr(invokeStatic(PRINT)),
                exitFunction(addInt(constant(2), constant(3)))
        ));
    }

    @Test
    public void nonRelocatableOperationWithBarrier() {
        decompile(() -> {
            set(var("a")).constant(2);
            set(var("b")).constant(3);
            set(var("c")).div(intNum(), var("a"), var("b"));
            invokeStaticMethod(PRINT);
            exit(var("c"));
        });
        expect(sequence(
                assign(var(2), divInt(constant(2), constant(3))),
                statementExpr(invokeStatic(PRINT)),
                exitFunction(var(2))
        ));
    }

    @Test
    public void properOrderOfArguments() {
        decompile(() -> {
            set(var("a")).invokeStatic(SUPPLY_INT_1);
            set(var("b")).invokeStatic(SUPPLY_INT_2);
            set(var("c")).add(intNum(), var("a"), var("b"));
            exit(var("c"));
        });
        expect(exitFunction(addInt(invokeStatic(SUPPLY_INT_1), invokeStatic(SUPPLY_INT_2))));
    }

    @Test
    public void wrongOrderOfArguments() {
        decompile(() -> {
            set(var("a")).invokeStatic(SUPPLY_INT_1);
            set(var("b")).invokeStatic(SUPPLY_INT_2);
            set(var("c")).add(intNum(), var("b"), var("a"));
            exit(var("c"));
        });
        expect(sequence(
                assign(var(0), invokeStatic(SUPPLY_INT_1)),
                exitFunction(addInt(invokeStatic(SUPPLY_INT_2), var(0)))
        ));
    }

    @Test
    public void simpleCondition() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("less"), label("greater"));

            put(label("less"));
            invokeStaticMethod(PRINT);
            jump(label("join"));

            put(label("greater"));
            invokeStaticMethod(PRINT_2);
            jump(label("join"));

            put(label("join"));
            exit();
        });

        expect(cond(
                less(constant(2), constant(0)),
                Arrays.asList(
                        statementExpr(invokeStatic(PRINT))
                ),
                Arrays.asList(
                        statementExpr(invokeStatic(PRINT_2))
                )
        ));
    }

    @Test
    public void simpleConditionWithOneBranch() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("less"), label("join"));

            put(label("less"));
            invokeStaticMethod(PRINT);
            jump(label("join"));

            put(label("join"));
            exit();
        });

        expect(cond(
                less(constant(2), constant(0)),
                Arrays.asList(
                        statementExpr(invokeStatic(PRINT))
                )
        ));
    }

    @Test
    public void simpleConditionWithEachBranchReturning() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("less"), label("greater"));

            put(label("less"));
            invokeStaticMethod(PRINT);
            exit();

            put(label("greater"));
            invokeStaticMethod(PRINT_2);
            exit();
        });

        expect(cond(
                less(constant(2), constant(0)),
                Arrays.asList(
                        statementExpr(invokeStatic(PRINT))
                ),
                Arrays.asList(
                        statementExpr(invokeStatic(PRINT_2))
                )
        ));
    }

    @Test
    public void shortCircuit() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("next"), label("false"));

            put(label("next"));
            set(var("b")).constant(3);
            ifLessThanZero(var("b"), label("true"), label("false"));

            put(label("true"));
            invokeStaticMethod(PRINT);
            jump(label("joint"));

            put(label("false"));
            invokeStaticMethod(PRINT_2);
            jump(label("joint"));

            put(label("joint"));
            invokeStaticMethod(PRINT_3);
            exit();
        });

        expect(sequence(
                cond(
                    and(
                            less(constant(2), constant(0)),
                            less(constant(3), constant(0))
                    ),
                    Arrays.asList(
                            statementExpr(invokeStatic(PRINT))
                    ),
                    Arrays.asList(
                            statementExpr(invokeStatic(PRINT_2))
                    )
                ),
                statementExpr(invokeStatic(PRINT_3))
        ));
    }

    @Test
    public void shortCircuitFailure() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("next"), label("false"));

            put(label("next"));
            invokeStaticMethod(PRINT_4);
            set(var("b")).constant(3);
            ifLessThanZero(var("b"), label("true"), label("false"));

            put(label("true"));
            invokeStaticMethod(PRINT);
            jump(label("joint"));

            put(label("false"));
            invokeStaticMethod(PRINT_2);
            jump(label("joint"));

            put(label("joint"));
            invokeStaticMethod(PRINT_3);
            exit();
        });

        expect(sequence(
                block(label -> Arrays.asList(
                    cond(
                            less(constant(2), constant(0)),
                            Arrays.asList(
                                    statementExpr(invokeStatic(PRINT_4)),
                                    cond(
                                            less(constant(3), constant(0)),
                                            Arrays.asList(
                                                    statementExpr(invokeStatic(PRINT)),
                                                    exitBlock(label)
                                            )
                                    )
                            )
                    ),
                    statementExpr(invokeStatic(PRINT_2))
                )),
                statementExpr(invokeStatic(PRINT_3))
        ));
    }

    @Test
    public void complexShortCircuit() {
        decompile(() -> {
            set(var("a")).constant(2);
            ifLessThanZero(var("a"), label("test_b"), label("test_c"));

            put(label("test_b"));
            set(var("b")).constant(3);
            ifLessThanZero(var("b"), label("true"), label("test_c"));

            put(label("test_c"));
            set(var("c")).constant(4);
            ifLessThanZero(var("c"), label("true"), label("false"));

            put(label("true"));
            invokeStaticMethod(PRINT);
            jump(label("joint"));

            put(label("false"));
            invokeStaticMethod(PRINT_2);
            jump(label("joint"));

            put(label("joint"));
            invokeStaticMethod(PRINT_3);
            exit();
        });

        expect(sequence(
                cond(
                        or(
                            and(
                                    less(constant(2), constant(0)),
                                    less(constant(3), constant(0))
                            ),
                            less(constant(4), constant(0))
                        ),
                        Arrays.asList(
                                statementExpr(invokeStatic(PRINT))
                        ),
                        Arrays.asList(
                                statementExpr(invokeStatic(PRINT_2))
                        )
                ),
                statementExpr(invokeStatic(PRINT_3))
        ));
    }

    @Test
    public void loop() {
        decompile(() -> {
            set(var("i")).constant(0);
            set(var("n")).constant(10);
            jump(label("head"));

            put(label("head"));
            set(var("cmp")).sub(intNum(), var("i"), var("n"));
            ifLessThanZero(var("cmp"), label("body"), label("exit"));

            put(label("body"));
            invokeStaticMethod(PRINT_NUM, var("i"));
            set(var("step")).constant(1);
            set(var("i")).add(intNum(), var("i"), var("step"));
            jump(label("head"));

            put(label("exit"));
            invokeStaticMethod(PRINT);
            exit();
        });

        expect(sequence(
                assign(var(0), constant(0)),
                loopWhile(less(var(0), constant(10)), loop -> Arrays.asList(
                        statementExpr(invokeStatic(PRINT_NUM, var(0))),
                        assign(var(0), addInt(var(0), constant(1)))
                )),
                statementExpr(invokeStatic(PRINT))
        ));
    }

    @Test
    public void simpleSwitch() {
        decompile(() -> {
            set(var("a")).invokeStatic(SUPPLY_INT_1);
            tableSwitch(
                    var("a"),
                    label("default"),
                    switchEntry("first", 1),
                    switchEntry("first", 2),
                    switchEntry("second", 3)
            );

            put(label("first"));
            invokeStaticMethod(PRINT);
            jump(label("end"));

            put(label("second"));
            invokeStaticMethod(PRINT_2);
            jump(label("end"));

            put(label("default"));
            invokeStaticMethod(PRINT_3);
            jump(label("end"));

            put(label("end"));
            invokeStaticMethod(PRINT_4);
            exit();
        });

        expect(sequence(
                switchStatement(
                        invokeStatic(SUPPLY_INT_1),
                        List.of(statementExpr(invokeStatic(PRINT_3))),
                        switchClause(new int[] { 1, 2 }, statementExpr(invokeStatic(PRINT))),
                        switchClause(3, statementExpr(invokeStatic(PRINT_2)))
                ),
                statementExpr(invokeStatic(PRINT_4))
        ));
    }

    @Test
    public void switchWithFallThrough() {
        decompile(() -> {
            set(var("a")).invokeStatic(SUPPLY_INT_1);
            tableSwitch(
                    var("a"),
                    label("default"),
                    switchEntry("first", 1),
                    switchEntry("second", 2)
            );

            put(label("first"));
            invokeStaticMethod(PRINT);
            jump(label("second"));

            put(label("second"));
            invokeStaticMethod(PRINT_2);
            jump(label("end"));

            put(label("default"));
            invokeStaticMethod(PRINT_3);
            jump(label("end"));

            put(label("end"));
            invokeStaticMethod(PRINT_4);
            exit();
        });

        expect(sequence(
                block(label -> Arrays.asList(
                    switchStatement(
                            invokeStatic(SUPPLY_INT_1),
                            List.of(
                                    statementExpr(invokeStatic(PRINT_3)),
                                    exitBlock(label)
                            ),
                            switchClause(1, statementExpr(invokeStatic(PRINT))),
                            switchClause(2)
                    ),
                    statementExpr(invokeStatic(PRINT_2))
                )),
                statementExpr(invokeStatic(PRINT_4))
        ));
    }

    private void decompile(Runnable r) {
        program = build(r);
        statement = decompiler.decompile(program);
    }

    private void expect(Statement statement) {
        assertEquals(
                "Wrong result for program:\n" + listingBuilder.buildListing(program, "  "),
                astPrinter.print(statement),
                astPrinter.print(this.statement)
        );
    }
}
