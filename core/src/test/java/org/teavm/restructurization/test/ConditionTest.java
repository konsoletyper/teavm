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
import static org.teavm.restructurization.BlockBuilder.cond;
import static org.teavm.restructurization.BlockBuilder.labeled;
import static org.teavm.restructurization.BlockBuilder.ret;
import static org.teavm.restructurization.BlockBuilder.simple;
import org.junit.Test;

public class ConditionTest extends BaseRestructurizationTest {
    @Test
    public void simpleCond() {
        restructurize(() -> {
            nop();
            ifLessThanZero(var("a"), label("less"), label("greater"));

            put(label("less"));
            nop();
            jump(label("join"));

            put(label("greater"));
            nop();
            jump(label("join"));

            put(label("join"));
            exit();
        });

        check(() -> {
            simple(block(0));
            cond(terminator(0), c -> {
                simple(block(1));
            }).otherwise(c -> {
                simple(block(2));
            });
            simple(block(3));
            ret();
        });
    }

    @Test
    public void simpleConditionWithOneBranch() {
        restructurize(() -> {
            nop();
            ifLessThanZero(var("a"), label("less"), label("join"));

            put(label("less"));
            nop();
            jump(label("join"));

            put(label("join"));
            exit();
        });

        check(() -> {
            simple(block(0));
            cond(terminator(0), c -> {
                simple(block(1));
            });
            simple(block(2));
            ret();
        });
    }

    @Test
    public void simpleConditionWithEachBranchReturning() {
        restructurize(() -> {
            nop();
            ifLessThanZero(var("a"), label("less"), label("greater"));

            put(label("less"));
            nop();
            exit();

            put(label("greater"));
            nop();
            exit();
        });

        check(() -> {
            simple(block(0));
            cond(terminator(0), c -> {
                simple(block(1));
                ret();
            }).otherwise(c -> {
                simple(block(2));
                ret();
            });
        });
    }

    @Test
    public void shortCircuit() {
        restructurize(() -> {
            nop();
            ifLessThanZero(var("a"), label("next"), label("false"));

            put(label("next"));
            nop();
            ifLessThanZero(var("b"), label("true"), label("false"));

            put(label("true"));
            nop();
            jump(label("joint"));

            put(label("false"));
            nop();
            jump(label("joint"));

            put(label("joint"));
            nop();
            exit();
        });

        check(() -> {
            simple(block(0));
            labeled(b1 -> {
                cond(terminator(0), b2 -> {
                    simple(block(1));
                    cond(terminator(1), b3 -> {
                        simple(block(2));
                        b1.br();
                    });
                });
                simple(block(3));
            });
            simple(block(4));
            ret();
        });
    }
}
