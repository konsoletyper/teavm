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

/**
 *
 * @author Alexey Andreev
 */
public abstract class ForkEmitter {
    public abstract void setThen(BasicBlock block);

    public abstract void setElse(BasicBlock block);

    public ForkEmitter and(BasicBlock block, final ForkEmitter other) {
        setThen(block);
        return new ForkEmitter() {
            @Override public void setThen(BasicBlock block) {
                other.setThen(block);
            }
            @Override public void setElse(BasicBlock block) {
                ForkEmitter.this.setElse(block);
                other.setElse(block);
            }
        };
    }

    public ForkEmitter or(BasicBlock block, final ForkEmitter other) {
        setElse(block);
        return new ForkEmitter() {
            @Override public void setThen(BasicBlock block) {
                ForkEmitter.this.setThen(block);
                other.setThen(block);
            }
            @Override public void setElse(BasicBlock block) {
                other.setElse(block);
            }
        };
    }

    public ForkEmitter not() {
        return new ForkEmitter() {
            @Override public void setThen(BasicBlock block) {
                ForkEmitter.this.setElse(block);
            }
            @Override public void setElse(BasicBlock block) {
                ForkEmitter.this.setThen(block);
            }
        };
    }
}
