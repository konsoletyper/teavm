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
package org.teavm.ast.decompilation;

import java.util.HashSet;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.model.MethodReference;

class AsyncCallsFinder extends RecursiveVisitor {
    final Set<MethodReference> asyncCalls = new HashSet<>();
    final Set<MethodReference> allCalls = new HashSet<>();

    @Override
    public void visit(AssignmentStatement statement) {
        InvocationExpr invocation = (InvocationExpr) statement.getRightValue();
        asyncCalls.add(invocation.getMethod());
    }

    @Override
    public void visit(InvocationExpr expr) {
        super.visit(expr);
        allCalls.add(expr.getMethod());
    }
}
