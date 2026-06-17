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
package org.teavm.flow;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.util.TransitionExtractor;

public class FlowReconstructionTest {
    @Test
    public void simple() {
        var program = buildProgram(new int[][] {
                { 1 },
                null
        });
        assertFlow(
                program,
                """
                0: br 1
                1: terminate
                """.stripIndent()
        );
    }

    @Test
    public void simpleIfThenElse() {
        var program = buildProgram(new int[][] {
                { 1, 2 },
                { 3 },
                { 3 },
                null
        });
        assertFlow(
                program,
                """
                block_3: block
                  block_2: block
                    0: br 1 block_2
                    1: br block_3
                  end
                  2: br block_3
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void simpleIfThen() {
        var program = buildProgram(new int[][] {
                { 1, 2 },
                { 2 },
                null
        });
        assertFlow(
                program,
                """
                block_2: block
                  0: br 1 block_2
                  1: br block_2
                end
                2: terminate
                """.stripIndent()
        );
    }

    @Test
    public void ifThenWithMultipleBreaks() {
        var program = buildProgram(new int[][] {
                { 1, 4 },
                { 2, 4 },
                { 3, 4 },
                { 4 },
                null
        });
        assertFlow(
                program,
                """
                block_4: block
                  0: br 1 block_4
                  1: br 2 block_4
                  2: br 3 block_4
                  3: br block_4
                end
                4: terminate
                """.stripIndent()
        );
    }

    @Test
    public void whileLoop() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3 },
                { 1 },
                null
        });
        assertFlow(
                program,
                """
                0: br 1
                block_3: block
                  loop_1: loop
                    1: br 2 block_3
                    2: br loop_1
                  end
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void doWhileLoop() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                { 1, 3 },
                null,
        });
        assertFlow(
                program,
                """
                0: br 1
                loop_1: loop
                  1: br 2
                  2: br loop_1 3
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void doWhileLoopSingleNode() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 1, 2 },
                null,
        });
        assertFlow(
                program,
                """
                0: br 1
                loop_1: loop
                  1: br loop_1 2
                end
                2: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopWithSeveralExits() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3 },
                { 5 },
                { 4, 6 },
                { 5 },
                null,
                { 1 },
        });
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  block_2: block
                    block_4: block
                      loop_1: loop
                        1: br block_2 3
                        3: br block_4 6
                        6: br loop_1
                      end
                    end
                    4: br block_5
                  end
                  2: br block_5
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopWithContinue() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                { 3, 1 },
                { 4 },
                { 5, 1 },
                null
        });
        assertFlow(
                program,
                """
                0: br 1
                loop_1: loop
                  1: br 2
                  2: br 3 loop_1
                  3: br 4
                  4: br 5 loop_1
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopWithNonTrivialContinue() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                { 3, 4 },
                { 1 },
                { 5 },
                { 6, 1 },
                null
        });
        assertFlow(
                program,
                """
                0: br 1
                loop_1: loop
                  1: br 2
                  block_4: block
                    2: br 3 block_4
                    3: br loop_1
                  end
                  4: br 5
                  5: br 6 loop_1
                end
                6: terminate
                """.stripIndent()
        );
    }

    @Test
    public void simpleTryCatch() {
        var program = buildProgram(new int[][] {
                null,
                null
        });
        addExceptionHandler(program, 0, 1, "java.lang.RuntimeException", 1);
        assertFlow(
                program,
                """
                try
                  0: terminate
                catch java.lang.RuntimeException 1
                1: terminate
                """.stripIndent()
        );
    }

    @Test
    public void longerTryCatch() {
        var program = buildProgram(new int[][] {
                { 1 },
                null,
                null
        });
        addExceptionHandler(program, 0, 2, "java.lang.RuntimeException", 2);
        assertFlow(
                program,
                """
                block_2: block
                  try
                    0: br 1
                    1: terminate
                  catch java.lang.RuntimeException block_2
                end
                2: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryCatchWithHandlers() {
        var program = buildProgram(new int[][] {
                { 1 },
                null,
                null,
                null
        });
        addExceptionHandler(program, 0, 2, "A", 2);
        addExceptionHandler(program, 0, 2, "B", 3);
        assertFlow(
                program,
                """
                block_3: block
                  try
                    block_2: block
                      try
                        0: br 1
                        1: terminate
                      catch A block_2
                    end
                  catch B block_3
                  2: terminate
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void mutiCatch() {
        var program = buildProgram(new int[][] {
                { 1 },
                null,
                null
        });
        addExceptionHandler(program, 0, 2, "A", 2);
        addExceptionHandler(program, 0, 2, "B", 2);
        assertFlow(
                program,
                """
                block_2: block
                  try
                    try
                      0: br 1
                      1: terminate
                    catch A block_2
                  catch B block_2
                end
                2: terminate
                """.stripIndent()
        );
    }

    @Test
    public void catchWithRecovery() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 3 },
                { 3 },
                null
        });
        addExceptionHandler(program, 0, 2, "A", 2);
        assertFlow(
                program,
                """
                block_3: block
                  block_2: block
                    try
                      0: br 1
                      1: br block_3
                    catch A block_2
                  end
                  2: br block_3
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void ifThenElseInTryCatch() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3 },
                { 4 },
                { 4 },
                null,
                null
        });
        addExceptionHandler(program, 1, 5, "java.lang.RuntimeException", 5);
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  try
                    block_4: block
                      block_3: block
                        1: br 2 block_3
                        2: br block_4
                      end
                      3: br block_4
                    end
                    4: terminate
                  catch java.lang.RuntimeException block_5
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryCatchInThenBranch() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 4 },
                { 5 },
                null,
                { 5 },
                null
        });
        addExceptionHandler(program, 2, 3, "java.lang.RuntimeException", 3);
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  block_4: block
                    1: br 2 block_4
                    try
                      2: br block_5
                    catch java.lang.RuntimeException 3
                    3: terminate
                  end
                  4: br block_5
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopInTryCatch() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3 },
                { 1 },
                null,
                null
        });
        addExceptionHandler(program, 1, 3, "A", 4);
        assertFlow(
                program,
                """
                0: br 1
                block_4: block
                  try
                    block_3: block
                      loop_1: loop
                        1: br 2 block_3
                        2: br loop_1
                      end
                    end
                  catch A block_4
                  3: terminate
                end
                4: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopExitPartiallyInTryCatch() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3 },
                { 1 },
                { 4 },
                null,
                null
        });
        addExceptionHandler(program, 1, 4, "A", 5);
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  try
                    block_3: block
                      loop_1: loop
                        1: br 2 block_3
                        2: br loop_1
                      end
                    end
                    3: br 4
                  catch A block_5
                  4: terminate
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopExitsWithAndWithoutTry() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 6 },
                { 3, 4 },
                { 1 },
                { 5 },
                null,
                { 7 },
                null,
                null
        });
        addExceptionHandler(program, 1, 6, "A", 8);
        assertFlow(
                program,
                """
                0: br 1
                block_8: block
                  try
                    block_6: block
                      block_4: block
                        loop_1: loop
                          1: br 2 block_6
                          2: br 3 block_4
                          3: br loop_1
                        end
                      end
                      4: br 5
                      5: terminate
                    end
                  catch A block_8
                  6: br 7
                  7: terminate
                end
                8: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryBeforeLoopAndLoopStart() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                { 3, 6 },
                { 4 },
                { 5 },
                { 2 },
                null,
                null
        });
        addExceptionHandler(program, 1, 4, "A", 7);
        assertFlow(
                program,
                """
                0: br 1
                block_7: block
                  try
                    1: br 2
                  catch A block_7
                  block_6: block
                    loop_2: loop
                      try
                        2: br 3 block_6
                        3: br 4
                      catch A block_7
                      4: br 5
                      5: br loop_2
                    end
                  end
                  6: terminate
                end
                7: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryLoopEndAndAfterLoop() {
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                { 3, 6 },
                { 4 },
                { 5 },
                { 2 },
                { 7 },
                { 8 },
                null,
                null
        });
        addExceptionHandler(program, 4, 8, "A", 9);
        assertFlow(
                program,
                """
                0: br 1
                1: br 2
                block_9: block
                  block_6: block
                    loop_2: loop
                      2: br 3 block_6
                      3: br 4
                      try
                        4: br 5
                        5: br loop_2
                      catch A block_9
                    end
                  end
                  try
                    6: br 7
                    7: br 8
                  catch A block_9
                  8: terminate
                end
                9: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryPartiallyThenElse() {
        var program = buildProgram(new int[][] {
                { 1, 4 },
                { 2 },
                { 3 },
                { 7 },
                { 5 },
                { 6 },
                { 7 },
                null,
                null
        });
        addExceptionHandler(program, 2, 5, "A", 8);
        assertFlow(
                program,
                """
                block_8: block
                  block_7: block
                    block_4: block
                      0: br 1 block_4
                      1: br 2
                      try
                        2: br 3
                        3: br block_7
                      catch A block_8
                    end
                    try
                      4: br 5
                    catch A block_8
                    5: br 6
                    6: br block_7
                  end
                  7: terminate
                end
                8: terminate
                """.stripIndent()
        );
    }

    @Test
    public void consecutiveTryCatch() {
        // Two sequential blocks each inside DIFFERENT try-catch regions.
        // Block 0 in try-catch A, block 1 in try-catch B (same depth, different handler).
        // The handler for B (block 4) must be wrapped in block_4 so that the catch label resolves.
        var program = buildProgram(new int[][] {
                { 1 },
                { 2 },
                null,
                null,
                null
        });
        addExceptionHandler(program, 0, 1, "A", 3);
        addExceptionHandler(program, 1, 2, "B", 4);
        assertFlow(
                program,
                """
                block_3: block
                  try
                    0: br 1
                  catch A block_3
                  block_4: block
                    try
                      1: br 2
                    catch B block_4
                    2: terminate
                  end
                  4: terminate
                end
                3: terminate
                """.stripIndent()
        );
    }

    @Test
    public void switchInLoop() {
        // A switch inside a loop: default target continues the loop, two case targets exit.
        var program = buildProgram(new int[][] {
                { 1 },
                { 2, 3, 4 },
                { 1 },
                { 5 },
                { 5 },
                null
        });
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  block_4: block
                    block_3: block
                      loop_1: loop
                        1: br block_3 block_4 2
                        2: br loop_1
                      end
                    end
                    3: br block_5
                  end
                  4: br block_5
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void switchWithTryCatchThreeWay() {
        // Switch in a try-catch loop: 3 successors with 3 distinct priorities
        // (loop-continue-in-TC > loop-exit-in-TC > loop-exit-no-TC).
        // block 4 is outside the try-catch region and must not be placed inside the try.
        var program = buildProgram(new int[][] {
                { 1 },        // 0 → 1
                { 3, 4, 2 },  // 1: switch default=3, case1=4, case2=2
                { 1 },        // 2 → 1 (loop back)
                { 6 },        // 3 → 6
                { 6 },        // 4 → 6
                null,         // 5 (exception handler A)
                null          // 6
        });
        addExceptionHandler(program, 1, 4, "A", 5);
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  block_6: block
                    try
                      block_4: block
                        block_3: block
                          loop_1: loop
                            1: br block_4 2 block_3
                            2: br loop_1
                          end
                        end
                        3: br block_6
                      end
                    catch A block_5
                    4: br block_6
                  end
                  6: terminate
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void nestedLoops() {
        var program = buildProgram(new int[][] {
                { 1 },     // 0 → outer loop head
                { 2 },     // 1: outer loop head → inner loop head
                { 3, 4 },  // 2: inner loop body
                { 2 },     // 3: inner loop back
                { 1, 5 },  // 4: after inner loop, continue outer or exit
                null       // 5
        });
        assertFlow(
                program,
                """
                0: br 1
                loop_1: loop
                  1: br 2
                  block_4: block
                    loop_2: loop
                      2: br 3 block_4
                      3: br loop_2
                    end
                  end
                  4: br loop_1 5
                end
                5: terminate
                """.stripIndent()
        );
    }

    @Test
    public void tryCatchAroundNestedLoops() {
        var program = buildProgram(new int[][] {
                { 1 },        // 0 → outer loop head
                { 2 },        // 1: outer loop head
                { 3, 4 },     // 2: inner loop head, branch
                { 2 },        // 3: inner loop back
                { 1, 5 },     // 4: after inner loop, continue outer or exit
                null,         // 5
                null          // 6 (exception handler)
        });
        addExceptionHandler(program, 1, 5, "A", 6);
        assertFlow(
                program,
                """
                0: br 1
                block_6: block
                  try
                    loop_1: loop
                      1: br 2
                      block_4: block
                        loop_2: loop
                          2: br 3 block_4
                          3: br loop_2
                        end
                      end
                      4: br loop_1 5
                    end
                  catch A block_6
                  5: terminate
                end
                6: terminate
                """.stripIndent()
        );
    }

    @Test
    public void loopBreakToOuter() {
        // Inner loop (loop_2) can break out to block_5 via the outer loop (loop_1).
        var program = buildProgram(new int[][] {
                { 1 },        // 0 → outer loop head
                { 2, 5 },     // 1: outer loop body or exit
                { 3, 4 },     // 2: inner loop head, continue or exit inner
                { 2 },        // 3: inner loop back
                { 1 },        // 4: after inner loop, continue outer
                null          // 5
        });
        assertFlow(
                program,
                """
                0: br 1
                block_5: block
                  loop_1: loop
                    1: br 2 block_5
                    block_4: block
                      loop_2: loop
                        2: br 3 block_4
                        3: br loop_2
                      end
                    end
                    4: br loop_1
                  end
                end
                5: terminate
                """.stripIndent()
        );
    }

    private void assertFlow(Program program, String expected) {
        var flow = new FlowReconstruction().reconstruct(program);
        assertEquals(expected, formatFlow(flow));
    }

    private Program buildProgram(int[][] transitions) {
        var program = new Program();
        var variable = program.createVariable();
        for (var i = 0; i < transitions.length; ++i) {
            program.createBasicBlock();
        }
        for (var i = 0; i < transitions.length; ++i) {
            var block = program.basicBlockAt(i);
            var blockTransitions = transitions[i];
            if (blockTransitions == null) {
                block.add(new ExitInstruction());
            } else if (blockTransitions.length == 1) {
                var jump = new JumpInstruction();
                jump.setTarget(program.basicBlockAt(blockTransitions[0]));
                block.add(jump);
            } else if (blockTransitions.length == 2) {
                var branch = new BranchingInstruction(BranchingCondition.EQUAL);
                branch.setOperand(variable);
                branch.setConsequent(program.basicBlockAt(blockTransitions[0]));
                branch.setAlternative(program.basicBlockAt(blockTransitions[1]));
                block.add(branch);
            } else {
                var switchInstruction = new SwitchInstruction();
                switchInstruction.setCondition(variable);
                for (var j = 1; j < blockTransitions.length; ++j) {
                    var entry = new SwitchTableEntry();
                    entry.setCondition(j);
                    entry.setTarget(program.basicBlockAt(blockTransitions[j]));
                    switchInstruction.getEntries().add(entry);
                }
                switchInstruction.setDefaultTarget(program.basicBlockAt(blockTransitions[0]));
                block.add(switchInstruction);
            }
        }
        return program;
    }

    private void addExceptionHandler(Program program, int start, int end, String exceptionType, int handler) {
        for (var i = start; i < end; ++i) {
            var tryCatchBlock = new TryCatchBlock();
            tryCatchBlock.setExceptionType(exceptionType);
            tryCatchBlock.setHandler(program.basicBlockAt(handler));
            program.basicBlockAt(i).getTryCatchBlocks().add(tryCatchBlock);
        }
    }

    private String formatFlow(List<FlowTreeNode> nodes) {
        var sb = new StringBuilder();
        var labels = new HashMap<Integer, String>();
        for (var part : nodes) {
            formatFlow(part, sb, 0, labels);
        }
        return sb.toString();
    }

    private void formatFlow(FlowTreeNode node, StringBuilder sb, int depth, Map<Integer, String> labels) {
        node.acceptVisitor(new FlowTreeNodeVisitor() {
            @Override
            public void visit(FlowTreeNode.Region node) {
                var transitionExtractor = new TransitionExtractor();
                for (var block : node.blocks) {
                    sb.append("  ".repeat(depth)).append(block.getIndex()).append(": ");
                    block.getLastInstruction().acceptVisitor(transitionExtractor);
                    var transitions = transitionExtractor.getTargets();
                    if (transitions == null || transitions.length == 0) {
                        sb.append("terminate");
                    } else {
                        sb.append("br");
                        for (var target : transitions) {
                            sb.append(" ");
                            var label = labels.get(target.getIndex());
                            sb.append(label != null ? label : target.getIndex());
                        }
                    }
                    sb.append("\n");
                }
            }

            @Override
            public void visit(FlowTreeNode.TryCatch node) {
                sb.append("  ".repeat(depth)).append("try").append("\n");
                for (var part : node.tryBody) {
                    formatFlow(part, sb, depth + 1, labels);
                }
                var label = labels.get(node.catchBlock.getIndex());
                sb.append("  ".repeat(depth)).append("catch ").append(node.exceptionType).append(" ")
                        .append(label != null ? label : node.catchBlock.getIndex()).append("\n");
            }

            @Override
            public void visit(FlowTreeNode.Loop node) {
                var label = "loop_" + node.head.getIndex();
                labels.put(node.head.getIndex(), label);
                sb.append("  ".repeat(depth)).append(label).append(": loop\n");
                for (var part : node.body) {
                    formatFlow(part, sb, depth + 1, labels);
                }
                labels.remove(node.head.getIndex());
                sb.append("  ".repeat(depth)).append("end\n");
            }

            @Override
            public void visit(FlowTreeNode.Block node) {
                var label = "block_" + node.jumpTarget.getIndex();
                labels.put(node.jumpTarget.getIndex(), label);
                sb.append("  ".repeat(depth)).append(label).append(": block\n");
                for (var part : node.body) {
                    formatFlow(part, sb, depth + 1, labels);
                }
                labels.remove(node.jumpTarget.getIndex());
                sb.append("  ".repeat(depth)).append("end\n");
            }
        });
    }
}
