/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.flow;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.BasicBlock;

public abstract sealed class FlowTreeNode {
    public abstract void acceptVisitor(FlowTreeNodeVisitor visitor);

    public static final class Region extends FlowTreeNode {
        public final List<BasicBlock> blocks = new ArrayList<>();

        @Override
        public void acceptVisitor(FlowTreeNodeVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class TryCatch extends FlowTreeNode {
        public final List<FlowTreeNode> tryBody = new ArrayList<>();
        public final BasicBlock catchBlock;
        public final String exceptionType;

        public TryCatch(BasicBlock catchBlock, String exceptionType) {
            this.catchBlock = catchBlock;
            this.exceptionType = exceptionType;
        }

        @Override
        public void acceptVisitor(FlowTreeNodeVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class Loop extends FlowTreeNode {
        public final BasicBlock head;
        public final List<FlowTreeNode> body = new ArrayList<>();

        public Loop(BasicBlock head) {
            this.head = head;
        }

        @Override
        public void acceptVisitor(FlowTreeNodeVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static final class Block extends FlowTreeNode {
        public final BasicBlock jumpTarget;
        public final List<FlowTreeNode> body = new ArrayList<>();

        public Block(BasicBlock jumpTarget) {
            this.jumpTarget = jumpTarget;
        }

        @Override
        public void acceptVisitor(FlowTreeNodeVisitor visitor) {
            visitor.visit(this);
        }
    }
}
