/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.common;

import java.util.*;

public class RangeTree {
    public static class Range {
        public final int left;
        public final int right;

        public Range(int left, int right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "[" + left + "; " + right + ")";
        }
    }

    public interface Node {
        Node getParent();

        int getStart();

        int getEnd();

        Node getNext();

        Node getFirstChild();
    }

    private static class NodeImpl implements Node {
        public NodeImpl parent;
        public int start;
        public int end;
        public NodeImpl firstChild;
        public NodeImpl next;

        @Override
        public Node getParent() {
            return parent;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public Node getNext() {
            return next;
        }

        @Override
        public Node getFirstChild() {
            return firstChild;
        }
    }

    private NodeImpl root;

    public RangeTree(int sz, Iterable<Range> ranges) {
        root = new NodeImpl();
        root.start = 0;
        root.end = sz;
        List<Range> rangeList = new ArrayList<>();
        for (Range range : ranges) {
            rangeList.add(range);
        }
        Collections.sort(rangeList, (o1, o2) -> {
            if (o1.right != o2.right) {
                return o2.right - o1.right;
            }
            return o1.left - o2.left;
        });
        Deque<NodeImpl> stack = new ArrayDeque<>();
        stack.push(root);
        for (Range range : rangeList) {
            NodeImpl current = new NodeImpl();
            current.start = range.left;
            current.end = range.right;
            while (range.right <= stack.peek().start) {
                stack.pop();
            }
            NodeImpl parent = stack.peek();
            current.next = parent.firstChild;
            parent.firstChild = current;
            current.parent = parent;
            for (NodeImpl ancestor : stack) {
                if (ancestor.start <= current.start) {
                    break;
                }
                ancestor.start = current.start;
            }
            stack.push(current);
        }
    }

    public Node getRoot() {
        return root;
    }
}
