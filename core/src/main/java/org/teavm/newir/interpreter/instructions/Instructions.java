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
package org.teavm.newir.interpreter.instructions;

import org.teavm.newir.interpreter.Instruction;
import org.teavm.newir.interpreter.InstructionPrinter;
import org.teavm.newir.interpreter.InterpreterContext;

public final class Instructions {
    private Instructions() {
    }

    public static Jump directJump(int label) {
        return new Goto(label);
    }

    public static Jump directJump() {
        return new Goto();
    }

    public static Jump jumpIfTrue(int label, int condition) {
        return new JumpIfTrue(condition, label);
    }

    public static Jump jumpIfFalse(int label, int condition) {
        return new JumpIfFalse(condition, label);
    }

    public static Jump jumpIfTrue(int condition) {
        return new JumpIfTrue(condition);
    }

    public static Jump jumpIfFalse(int condition) {
        return new JumpIfFalse(condition);
    }

    public static Instruction stop() {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.stopped = true;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.opcode("stop");
            }
        };
    }

    public static Instruction icst(int slot, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[slot] = value;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(slot).opcode("icst").arg().text(String.valueOf(value));
            }
        };
    }

    public static Instruction lcst(int slot, long value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[slot] = value;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(slot).opcode("lcst").arg().text(String.valueOf(value));
            }
        };
    }

    public static Instruction fcst(int slot, float value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[slot] = value;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(slot).opcode("fcst").arg().text(String.valueOf(value));
            }
        };
    }

    public static Instruction dcst(int slot, double value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[slot] = value;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(slot).opcode("dcst").arg().text(String.valueOf(value));
            }
        };
    }

    public static Instruction imov(int result, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[result] = context.iv[value];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(result).opcode("imov").arg().intVar(value);
            }
        };
    }

    public static Instruction lmov(int result, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[result] = context.lv[value];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(result).opcode("lmov").arg().longVar(value);
            }
        };
    }

    public static Instruction fmov(int result, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[result] = context.fv[value];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(result).opcode("fmov").arg().longVar(value);
            }
        };
    }

    public static Instruction dmov(int result, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[result] = context.dv[value];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(result).opcode("dmov").arg().longVar(value);
            }
        };
    }

    public static Instruction omov(int result, int value) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.ov[result] = context.ov[value];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.objVar(result).opcode("omov").arg().objVar(value);
            }
        };
    }
}
