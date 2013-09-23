package org.teavm.model;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class Phi {
    private BasicBlock basicBlock;
    private Variable receiver;
    private List<Incoming> incomings = new ArrayList<>();

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

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
}
