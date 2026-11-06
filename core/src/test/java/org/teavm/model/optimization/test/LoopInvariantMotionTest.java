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
package org.teavm.model.optimization.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.optimization.MethodOptimizationContext;
import org.teavm.model.optimization.LoopInvariantMotion;

public class LoopInvariantMotionTest {
    @Test
    public void doesNotHoistAcrossExceptionalLoopEntry() {
        LoopFixture fixture = new LoopFixture(true, false);

        new LoopInvariantMotion().optimize(fixture.context(), fixture.program);

        assertSame(fixture.loopHeader, fixture.cast.getBasicBlock());
        assertEquals(5, fixture.program.basicBlockCount());
    }

    @Test
    public void hoistsInvariantFromOrdinaryLoop() {
        LoopFixture fixture = new LoopFixture(false, false);

        assertTrue(new LoopInvariantMotion().optimize(fixture.context(), fixture.program));

        assertSame(fixture.entry, fixture.cast.getBasicBlock());
        assertEquals(4, fixture.program.basicBlockCount());
    }

    @Test
    public void hoistsAcrossInternalExceptionalBackedge() {
        LoopFixture fixture = new LoopFixture(false, true);

        assertTrue(new LoopInvariantMotion().optimize(fixture.context(), fixture.program));

        assertSame(fixture.entry, fixture.cast.getBasicBlock());
        assertEquals(4, fixture.program.basicBlockCount());
    }

    private static class LoopFixture {
        final Program program = new Program();
        final org.teavm.model.BasicBlock entry = program.createBasicBlock();
        final org.teavm.model.BasicBlock alternateEntry;
        final org.teavm.model.BasicBlock loopHeader;
        final org.teavm.model.BasicBlock loopBody;
        final org.teavm.model.BasicBlock exit;
        final org.teavm.model.Variable input = program.createVariable();
        final org.teavm.model.Variable condition = program.createVariable();
        final org.teavm.model.Variable castResult = program.createVariable();
        final org.teavm.model.Variable callResult = program.createVariable();
        final CastInstruction cast = new CastInstruction();
        final MethodHolder method = new MethodHolder("test", ValueType.object("java.lang.Object"), ValueType.VOID);

        LoopFixture(boolean exceptionalEntry, boolean exceptionalBackedge) {
            if (exceptionalEntry) {
                alternateEntry = program.createBasicBlock();
            } else {
                alternateEntry = null;
            }
            loopHeader = program.createBasicBlock();
            loopBody = program.createBasicBlock();
            exit = program.createBasicBlock();

            if (alternateEntry == null) {
                JumpInstruction jump = new JumpInstruction();
                jump.setTarget(loopHeader);
                entry.add(jump);
            } else {
                BranchingInstruction branch = new BranchingInstruction(BranchingCondition.NULL);
                branch.setOperand(condition);
                branch.setConsequent(alternateEntry);
                branch.setAlternative(exit);
                entry.add(branch);

                InvokeInstruction mayThrow = new InvokeInstruction();
                mayThrow.setType(InvocationType.SPECIAL);
                mayThrow.setMethod(new org.teavm.model.MethodReference(
                        "Test", "mayThrow", ValueType.VOID));
                alternateEntry.add(mayThrow);
                JumpInstruction jump = new JumpInstruction();
                jump.setTarget(exit);
                alternateEntry.add(jump);

                TryCatchBlock tryCatchBlock = new TryCatchBlock();
                tryCatchBlock.setHandler(loopHeader);
                tryCatchBlock.setExceptionType("java.lang.RuntimeException");
                alternateEntry.getTryCatchBlocks().add(tryCatchBlock);
            }
            if (exceptionalEntry || exceptionalBackedge) {
                loopHeader.setExceptionVariable(program.createVariable());
            }

            cast.setValue(input);
            cast.setReceiver(castResult);
            cast.setTargetType(ValueType.object("java.lang.Object"));
            cast.setWeak(true);
            loopHeader.add(cast);

            InvokeInstruction call = new InvokeInstruction();
            call.setType(InvocationType.VIRTUAL);
            call.setInstance(castResult);
            call.setMethod(new org.teavm.model.MethodReference(Object.class, "toString", String.class));
            call.setReceiver(callResult);
            loopHeader.add(call);

            BranchingInstruction branch = new BranchingInstruction(BranchingCondition.NULL);
            branch.setOperand(condition);
            branch.setConsequent(loopBody);
            branch.setAlternative(exit);
            loopHeader.add(branch);

            JumpInstruction backedge = new JumpInstruction();
            backedge.setTarget(loopHeader);
            loopBody.add(backedge);
            if (exceptionalBackedge) {
                TryCatchBlock tryCatchBlock = new TryCatchBlock();
                tryCatchBlock.setHandler(loopHeader);
                tryCatchBlock.setExceptionType("java.lang.RuntimeException");
                loopBody.getTryCatchBlocks().add(tryCatchBlock);
            }
            exit.add(new ExitInstruction());
        }

        MethodOptimizationContext context() {
            return new MethodOptimizationContext() {
                @Override
                public MethodReader getMethod() {
                    return method;
                }

                @Override
                public DependencyInfo getDependencyInfo() {
                    return null;
                }

                @Override
                public ClassReaderSource getClassSource() {
                    return null;
                }

                @Override
                public ClassHierarchy getHierarchy() {
                    return null;
                }
            };
        }
    }
}
