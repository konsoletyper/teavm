/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.emit;

import org.teavm.model.BasicBlock;

public abstract class ForkEmitter {
    private ProgramEmitter pe;

    public ForkEmitter(ProgramEmitter pe) {
        this.pe = pe;
    }

    public abstract ForkEmitter setThen(BasicBlock block);

    public abstract ForkEmitter setElse(BasicBlock block);

    public ForkEmitter and(BasicBlock block, final ForkEmitter other) {
        setThen(block);
        return new ForkEmitter(pe) {
            @Override public ForkEmitter setThen(BasicBlock block) {
                other.setThen(block);
                return this;
            }
            @Override public ForkEmitter setElse(BasicBlock block) {
                ForkEmitter.this.setElse(block);
                other.setElse(block);
                return this;
            }
        };
    }

    public ForkEmitter or(BasicBlock block, final ForkEmitter other) {
        setElse(block);
        return new ForkEmitter(pe) {
            @Override public ForkEmitter setThen(BasicBlock block) {
                ForkEmitter.this.setThen(block);
                other.setThen(block);
                return this;
            }
            @Override public ForkEmitter setElse(BasicBlock block) {
                other.setElse(block);
                return this;
            }
        };
    }

    public ForkEmitter not() {
        return new ForkEmitter(pe) {
            @Override public ForkEmitter setThen(BasicBlock block) {
                ForkEmitter.this.setElse(block);
                return this;
            }
            @Override public ForkEmitter setElse(BasicBlock block) {
                ForkEmitter.this.setThen(block);
                return this;
            }
        };
    }
}
