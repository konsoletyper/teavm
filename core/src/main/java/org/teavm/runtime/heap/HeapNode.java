/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.runtime.heap;

import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
class HeapNode extends HeapRecord {
    static final int FLAG_LIST = 1;

    HeapNode left;
    HeapNode right;
    HeapNode list;
    short height;
    short flags;

    static HeapNode balance(HeapNode node) {
        int factor = factor(node);
        if (factor == 2) {
            if (factor(node.right) < 0) {
                node.right = rotateRight(node.right);
            }
            return rotateLeft(node);
        } else if (factor == -2) {
            if (factor(node.left) > 0) {
                node.left = rotateLeft(node.left);
            }
            return rotateRight(node);
        } else {
            return node;
        }
    }

    private static int factor(HeapNode node) {
        return (node.right != null ? node.right.height : 0) - (node.left != null ? node.left.height : 0);
    }

    private static HeapNode rotateRight(HeapNode node) {
        var left = node.left;
        node.left = left.right;
        left.right = node;
        fix(node);
        fix(left);
        return left;
    }

    private static HeapNode rotateLeft(HeapNode node) {
        var right = node.right;
        node.right = right.left;
        right.left = node;
        fix(node);
        fix(right);
        return right;
    }

    static void fix(HeapNode node) {
        var leftHeight = node.right != null ? node.right.height : 0;
        var rightHeight = node.left != null ? node.left.height : 0;
        node.height = (short) (Math.max(leftHeight, rightHeight) + 1);
    }
}
