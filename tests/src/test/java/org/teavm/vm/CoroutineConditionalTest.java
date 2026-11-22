/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.teavm.junit.AsyncEnabled;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@AsyncEnabled
@EachTestCompiledSeparately
public class CoroutineConditionalTest {
    @Test
    public void earlyExitFromSuspendingThenArm() throws InterruptedException {
        assertEquals(1, choose(true, true));
        assertEquals(2, choose(true, false));
        assertEquals(3, choose(false, true));
        assertEquals(3, choose(false, false));
    }

    private int choose(boolean first, boolean early) throws InterruptedException {
        int result;
        done: {
            if (first) {
                Thread.sleep(1);
                result = 1;
                if (early) {
                    break done;
                }
                result = 2;
            } else {
                result = 3;
            }
        }
        return result;
    }

    @Test
    public void earlyExitFromNestedSuspendingThenArm() throws InterruptedException {
        assertEquals(1, chooseNested(true, true, true));
        assertEquals(2, chooseNested(true, true, false));
        assertEquals(3, chooseNested(true, false, true));
        assertEquals(3, chooseNested(true, false, false));
        assertEquals(4, chooseNested(false, true, true));
        assertEquals(4, chooseNested(false, true, false));
        assertEquals(4, chooseNested(false, false, true));
        assertEquals(4, chooseNested(false, false, false));
    }

    private int chooseNested(boolean outer, boolean inner, boolean early) throws InterruptedException {
        int result;
        done: {
            if (outer) {
                if (inner) {
                    Thread.sleep(1);
                    result = 1;
                    if (early) {
                        break done;
                    }
                    result = 2;
                } else {
                    result = pause(3);
                }
            } else {
                result = pause(4);
            }
        }
        return result;
    }

    @Test
    public void valueProducingNestedSuspendingConditional() throws InterruptedException {
        assertEquals(11, chooseValue(true, true));
        assertEquals(12, chooseValue(true, false));
        assertEquals(13, chooseValue(false, true));
        assertEquals(13, chooseValue(false, false));
    }

    private int chooseValue(boolean outer, boolean inner) throws InterruptedException {
        // The left operand remains on the stack while the nested conditional suspends.
        return pause(10) + (outer ? (inner ? pause(1) : pause(2)) : pause(3));
    }

    private int pause(int value) throws InterruptedException {
        Thread.sleep(1);
        return value;
    }
}
