/*
 *  Copyright 2013 Alexey Andreev.
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
import java.util.Collections;
import java.util.List;

public class Phi implements PhiReader {
    private BasicBlock basicBlock;
    private Variable receiver;
    private List<Incoming> incomings = new ArrayList<>();

    @Override
    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    @Override
    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    private List<Incoming> safeIncomings = new AbstractList<Incoming>() {
        @Override
        public Incoming get(int index) {
            return incomings.get(index);
        }

        @Override
        public Incoming set(int index, Incoming element) {
            if (element.getPhi() != null) {
                throw new IllegalArgumentException("This incoming is already in some phi");
            }
            element.setPhi(Phi.this);
            Incoming old = incomings.get(index);
            old.setPhi(null);
            incomings.set(index, element);
            return old;
        }

        @Override
        public void add(int index, Incoming element) {
            if (element.getPhi() != null) {
                throw new IllegalArgumentException("This incoming is already in some phi");
            }
            element.setPhi(Phi.this);
            incomings.add(index, element);
        }

        @Override
        public Incoming remove(int index) {
            Incoming incoming = incomings.remove(index);
            incoming.setPhi(null);
            return incoming;
        }

        @Override
        public void clear() {
            for (Incoming incoming : incomings) {
                incoming.setPhi(null);
            }
            incomings.clear();
        }

        @Override
        public int size() {
            return incomings.size();
        }
    };

    public List<Incoming> getIncomings() {
        return safeIncomings;
    }

    private List<? extends IncomingReader> immutableIncomings = Collections.unmodifiableList(incomings);

    @Override
    public List<? extends IncomingReader> readIncomings() {
        return immutableIncomings;
    }
}
