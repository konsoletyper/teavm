/*
 *  Copyright 2014 Alexey Andreev.
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

public class TryCatchBlock implements TryCatchBlockReader {
    BasicBlock protectedBlock;
    private BasicBlock handler;
    private String exceptionType;
    private List<TryCatchJoint> joints = new ArrayList<>();
    private List<TryCatchJointReader> immutableJoints;

    private List<TryCatchJoint> safeJoints = new AbstractList<TryCatchJoint>() {
        @Override
        public TryCatchJoint get(int index) {
            return joints.get(index);
        }

        @Override
        public int size() {
            return joints.size();
        }

        @Override
        public void add(int index, TryCatchJoint e) {
            if (e.getBlock() != null) {
                throw new IllegalArgumentException("This joint is already in some basic block");
            }
            e.block = TryCatchBlock.this;
            joints.add(index, e);
        }

        @Override
        public TryCatchJoint set(int index, TryCatchJoint element) {
            if (element.block != null) {
                throw new IllegalArgumentException("This phi is already in some basic block");
            }
            TryCatchJoint oldJoint = joints.get(index);
            oldJoint.block = null;
            element.block = TryCatchBlock.this;
            return joints.set(index, element);
        }

        @Override
        public TryCatchJoint remove(int index) {
            TryCatchJoint joint = joints.remove(index);
            joint.block = null;
            return joint;
        }

        @Override
        public void clear() {
            for (TryCatchJoint joint : joints) {
                joint.block = null;
            }
            joints.clear();
        }
    };

    @Override
    public BasicBlock getHandler() {
        return handler;
    }

    public void setHandler(BasicBlock handler) {
        this.handler = handler;
    }

    @Override
    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    @Override
    public BasicBlock getProtectedBlock() {
        return protectedBlock;
    }

    public List<TryCatchJoint> getJoints() {
        return safeJoints;
    }

    @Override
    public List<TryCatchJointReader> readJoints() {
        if (immutableJoints == null) {
            immutableJoints = Collections.unmodifiableList(safeJoints);
        }
        return immutableJoints;
    }
}
