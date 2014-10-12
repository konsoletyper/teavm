/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.javascript.ast;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Alexey Andreev
 */
public class AssignmentStatement extends Statement {
    private Expr leftValue;
    private Expr rightValue;
    private NodeLocation location;
    private Set<String> debugNames = new HashSet<>();

    public Expr getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(Expr leftValue) {
        this.leftValue = leftValue;
    }

    public Expr getRightValue() {
        return rightValue;
    }

    public void setRightValue(Expr rightValue) {
        this.rightValue = rightValue;
    }

    public NodeLocation getLocation() {
        return location;
    }

    public void setLocation(NodeLocation location) {
        this.location = location;
    }

    public Set<String> getDebugNames() {
        return debugNames;
    }

    @Override
    public void acceptVisitor(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
