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
import static org.teavm.model.builder.ProgramBuilder.jump;
import static org.teavm.model.builder.ProgramBuilder.label;
import static org.teavm.model.builder.ProgramBuilder.nop;
import static org.teavm.model.builder.ProgramBuilder.put;
import static org.teavm.model.builder.ProgramBuilder.switchEntry;
import static org.teavm.model.builder.ProgramBuilder.tableSwitch;
import static org.teavm.model.builder.ProgramBuilder.var;
import static org.teavm.restructurization.BlockBuilder.labeled;
import static org.teavm.restructurization.BlockBuilder.ret;
import static org.teavm.restructurization.BlockBuilder.simple;
import static org.teavm.restructurization.BlockBuilder.sw;
import org.junit.Test;

public class SwitchTest extends BaseRestructurizationTest {
    @Test
    public void simpleSwitch() {
        restructurize(() -> {
            nop();
            tableSwitch(
                    var("a"),
                    label("default"),
                    switchEntry("first", 1),
                    switchEntry("first", 2),
                    switchEntry("second", 3)
            );

            put(label("first"));
            nop();
            jump(label("end"));

            put(label("second"));
            nop();
            jump(label("end"));

            put(label("default"));
            nop();
            jump(label("end"));

            put(label("end"));
            nop();
            exit();
        });
        check(() -> {
            simple(block(0));
            sw(variable(0), b1 -> {
                simple(block(3));
            }).entry(new int[] { 1, 2 }, b1 -> {
                simple(block(1));
            }).entry(3, b1 -> {
                simple(block(2));
            });
            simple(block(4));
            ret();
        });
    }

    @Test
    public void fallThrough() {
        restructurize(() -> {
            nop();
            tableSwitch(
                    var("a"),
                    label("default"),
                    switchEntry("first", 1),
                    switchEntry("second", 2)
            );

            put(label("first"));
            nop();
            jump(label("second"));

            put(label("second"));
            nop();
            jump(label("end"));

            put(label("default"));
            nop();
            jump(label("end"));

            put(label("end"));
            nop();
            exit();
        });
        check(() -> {
            simple(block(0));
            labeled(b1 -> {
                sw(variable(0), b2 -> {
                    simple(block(3));
                    b1.br();
                }).entry(1, b2 -> {
                    simple(block(1));
                }).entry(2, b2 -> {
                });
                simple(block(2));
            });
            simple(block(4));
            ret();
        });
    }

    @Test
    public void sharedBody() {
        restructurize(() -> {
            nop();
            tableSwitch(
                    var("a"),
                    label("default"),
                    switchEntry("first", 1),
                    switchEntry("second", 2)
            );

            put(label("first"));
            nop();
            jump(label("shared"));

            put(label("second"));
            nop();
            jump(label("shared"));

            put(label("shared"));
            nop();
            jump(label("end"));

            put(label("default"));
            nop();
            jump(label("end"));

            put(label("end"));
            nop();
            exit();
        });
        check(() -> {
            simple(block(0));
            labeled(b1 -> {
                sw(variable(0), b2 -> {
                    simple(block(4));
                    b1.br();
                }).entry(1, b2 -> {
                    simple(block(1));
                }).entry(2, b2 -> {
                    simple(block(2));
                });
                simple(block(3));
            });
            simple(block(5));
            ret();
        });
    }
}
