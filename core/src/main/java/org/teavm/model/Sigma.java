/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.model;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class Sigma {
    private BasicBlock basicBlock;
    private Variable value;
    private List<Outgoing> outgoings = new ArrayList<>();

    public Sigma(BasicBlock basicBlock, Variable value) {
        this.basicBlock = basicBlock;
        this.value = value;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    public List<Outgoing> getOutgoings() {
        return safeOutgoings;
    }

    private List<Outgoing> safeOutgoings = new AbstractList<Outgoing>() {
        @Override
        public Outgoing get(int index) {
            return outgoings.get(index);
        }

        @Override
        public Outgoing set(int index, Outgoing element) {
            if (element.getSigma() != null) {
                throw new IllegalArgumentException("This outgoing is already in some sigma");
            }
            element.setSigma(Sigma.this);
            Outgoing old = outgoings.get(index);
            old.setSigma(null);
            outgoings.set(index, element);
            return old;
        }

        @Override
        public void add(int index, Outgoing element) {
            if (element.getSigma() != null) {
                throw new IllegalArgumentException("This outgoing is already in some sigma");
            }
            element.setSigma(Sigma.this);
            outgoings.add(index, element);
        }

        @Override
        public Outgoing remove(int index) {
            Outgoing outgoing = outgoings.remove(index);
            outgoing.setSigma(null);
            return outgoing;
        }

        @Override
        public void clear() {
            for (Outgoing outgoing : outgoings) {
                outgoing.setSigma(null);
            }
            outgoings.clear();
        }

        @Override
        public int size() {
            return outgoings.size();
        }
    };
}
