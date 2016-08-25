/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.ast;

import org.teavm.model.TextLocation;

public class AssignmentStatement extends Statement {
    private Expr leftValue;
    private Expr rightValue;
    private TextLocation location;
    private boolean async;

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

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public void acceptVisitor(StatementVisitor visitor) {
        visitor.visit(this);
    }
}
