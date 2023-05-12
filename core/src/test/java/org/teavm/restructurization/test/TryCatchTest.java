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

import static org.teavm.model.builder.ProgramBuilder.doCatch;
import static org.teavm.model.builder.ProgramBuilder.doTry;
import static org.teavm.model.builder.ProgramBuilder.exit;
import static org.teavm.model.builder.ProgramBuilder.jump;
import static org.teavm.model.builder.ProgramBuilder.label;
import static org.teavm.model.builder.ProgramBuilder.nop;
import static org.teavm.model.builder.ProgramBuilder.put;
import static org.teavm.model.builder.ProgramBuilder.var;
import static org.teavm.restructurization.BlockBuilder.ret;
import static org.teavm.restructurization.BlockBuilder.simple;
import static org.teavm.restructurization.BlockBuilder.tryBlock;
import org.junit.Test;

public class TryCatchTest extends BaseRestructurizationTest {
    @Test
    public void simpleTryCatch() {
        restructurize(() -> {
            nop();
            jump(label("first"));

            put(label("first"));
            nop();
            doTry("java.lang.RuntimeException", label("handler"));
            jump(label("second"));

            put(label("second"));
            nop();
            doTry("java.lang.RuntimeException", label("handler"));
            jump(label("third"));

            put(label("handler"));
            doCatch(var("e"));
            nop();
            jump(label("third"));

            put(label("third"));
            nop();
            exit();
        });

        check(() -> {
            simple(block(0));
            tryBlock(() -> {
                simple(block(1));
                simple(block(2));
            }).catchException("java.lang.RuntimeException", variable(0), () -> {
                simple(block(3));
            });
            simple(block(4));
            ret();
        });
    }
}
