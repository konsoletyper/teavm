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
package org.teavm.common;

public class MutableGraphEdge {
    MutableGraphEdge back;
    MutableGraphNode first;
    MutableGraphNode second;

    public MutableGraphNode getFirst() {
        return first;
    }

    public void setFirst(MutableGraphNode first) {
        back.setSecond(first);
    }

    public MutableGraphNode getSecond() {
        return second;
    }

    public void setSecond(MutableGraphNode second) {
        if (this.second == second) {
            return;
        }
        this.second.edges.remove(first);
        first.edges.remove(this.second);
        if (!second.edges.containsKey(first)) {
            this.second = second;
            back.first = second;
            second.edges.put(first, back);
            first.edges.put(second, this);
        } else {
            this.first = null;
            this.second = null;
            back.first = null;
            back.second = null;
        }
    }

    public MutableGraphEdge getBack() {
        return back;
    }

    @Override
    public String toString() {
        return String.valueOf(second.getTag());
    }
}
