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

public final class ArithmeticInstructions {
    private ArithmeticInstructions() {
    }

    public static Instruction iadd(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] + context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("iadd").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction isub(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] - context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("isub").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction imul(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] * context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("imul").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction idiv(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] / context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("idiv").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction irem(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] % context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("irem").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ladd(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] + context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("ladd").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lsub(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] - context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("lsub").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lmul(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] * context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("lmul").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction ldiv(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] / context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("ldiv").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lrem(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] % context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("lrem").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lcmp(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = Long.compare(context.lv[a], context.lv[b]);
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("lcmp").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction fadd(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[r] = context.fv[a] + context.fv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.floatVar(r).opcode("fadd").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction fsub(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[r] = context.fv[a] - context.fv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.floatVar(r).opcode("fsub").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction fmul(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[r] = context.fv[a] * context.fv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.floatVar(r).opcode("fmul").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction fdiv(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[r] = context.fv[a] / context.fv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.floatVar(r).opcode("fdiv").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction frem(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.fv[r] = context.fv[a] % context.fv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.floatVar(r).opcode("frem").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction fcmp(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = Float.compare(context.fv[a], context.fv[b]);
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("fcmp").floatVar(a).arg().floatVar(b);
            }
        };
    }

    public static Instruction dadd(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[r] = context.dv[a] + context.dv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.doubleVar(r).opcode("dadd").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction dsub(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[r] = context.dv[a] - context.dv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.doubleVar(r).opcode("dsub").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction dmul(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[r] = context.dv[a] * context.dv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.doubleVar(r).opcode("dmul").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction ddiv(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[r] = context.dv[a] / context.dv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.doubleVar(r).opcode("dmul").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction drem(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.dv[r] = context.dv[a] % context.dv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.doubleVar(r).opcode("drem").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction dcmp(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = Double.compare(context.dv[a], context.dv[b]);
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("dcmp").doubleVar(a).arg().doubleVar(b);
            }
        };
    }

    public static Instruction iand(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] & context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("iand").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ior(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] | context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ior").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ixor(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] ^ context.iv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ixor").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction land(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] & context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("land").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lor(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] | context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("lor").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction lxor(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.lv[r] = context.lv[a] ^ context.lv[b];
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.longVar(r).opcode("lxor").longVar(a).arg().longVar(b);
            }
        };
    }

    public static Instruction ieq(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] == context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ieq").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ine(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] != context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ine").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ilt(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] < context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ilt").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ile(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] <= context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ile").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction igt(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] > context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("igt").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction ige(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.iv[a] >= context.iv[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("ige").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction oeq(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.ov[a] == context.ov[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("oeq").intVar(a).arg().intVar(b);
            }
        };
    }

    public static Instruction one(int r, int a, int b) {
        return new Instruction() {
            @Override
            public void exec(InterpreterContext context) {
                context.iv[r] = context.ov[a] != context.ov[b] ? 1 : 0;
                context.ptr++;
            }

            @Override
            public void print(InstructionPrinter p) {
                p.intVar(r).opcode("one").intVar(a).arg().intVar(b);
            }
        };
    }
}
