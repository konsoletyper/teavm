/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization.test;

import static org.teavm.model.builder.ProgramBuilder.exit;
import static org.teavm.model.builder.ProgramBuilder.ifLessThanZero;
import static org.teavm.model.builder.ProgramBuilder.jump;
import static org.teavm.model.builder.ProgramBuilder.label;
import static org.teavm.model.builder.ProgramBuilder.nop;
import static org.teavm.model.builder.ProgramBuilder.put;
import static org.teavm.model.builder.ProgramBuilder.var;
import static org.teavm.restructurization.BlockBuilder.invCond;
import static org.teavm.restructurization.BlockBuilder.loop;
import static org.teavm.restructurization.BlockBuilder.ret;
import static org.teavm.restructurization.BlockBuilder.simple;
import org.junit.Test;

public class LoopTest extends BaseRestructurizationTest {
    @Test
    public void simpleLoop() {
        restructurize(() -> {
            nop();
            jump(label("head"));

            put(label("head"));
            nop();
            ifLessThanZero(var("cmp"), label("body"), label("exit"));

            put(label("body"));
            nop();
            jump(label("head"));

            put(label("exit"));
            nop();
            exit();
        });

        check(() -> {
            simple(block(0));
            loop(b1 -> {
                simple(block(1));
                invCond(terminator(1), b2 -> {
                   b1.br();
                });
                simple(block(2));
            });
            simple(block(3));
            ret();
        });
    }
}
