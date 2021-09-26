/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.interpreter;

public interface InterpreterInstruction {
    void accept(ExprInterpreterContext context);

    class JumpIfTrue implements Jump {
        public final int slot;
        public int target;

        public JumpIfTrue(int slot) {
            this.slot = slot;
        }

        public JumpIfTrue(int slot, int target) {
            this.slot = slot;
            this.target = target;
        }

        @Override
        public void setTarget(int target) {
            this.target = target;
        }

        @Override
        public void accept(ExprInterpreterContext context) {
            if (context.iv[slot] != 0) {
                context.ptr = slot;
            } else {
                context.ptr++;
            }
        }
    }

    class JumpIfFalse implements Jump {
        public final int slot;
        public int target;

        public JumpIfFalse(int slot) {
            this.slot = slot;
        }

        public JumpIfFalse(int slot, int target) {
            this.slot = slot;
            this.target = target;
        }

        @Override
        public void setTarget(int target) {
            this.target = target;
        }

        @Override
        public void accept(ExprInterpreterContext context) {
            if (context.iv[slot] == 0) {
                context.ptr = slot;
            } else {
                context.ptr++;
            }
        }
    }

    class Goto implements Jump {
        public int target;

        public Goto() {
        }

        public Goto(int target) {
            this.target = target;
        }

        @Override
        public void setTarget(int target) {
            this.target = target;
        }

        @Override
        public void accept(ExprInterpreterContext context) {
            context.ptr = target;
        }
    }

    interface Jump extends InterpreterInstruction {
        void setTarget(int target);
    }
}
