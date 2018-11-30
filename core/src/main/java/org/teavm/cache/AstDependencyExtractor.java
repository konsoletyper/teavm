/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.cache;

import java.util.HashSet;
import java.util.Set;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.RegularMethodNode;

public class AstDependencyExtractor extends RecursiveVisitor {
    private final ExtractingVisitor visitor = new ExtractingVisitor();

    public String[] extract(RegularMethodNode node) {
        node.getBody().acceptVisitor(visitor);
        String[] result = visitor.dependencies.toArray(new String[0]);
        visitor.dependencies.clear();
        return result;
    }

    public String[] extract(AsyncMethodNode node) {
        for (AsyncMethodPart part : node.getBody()) {
            part.getStatement().acceptVisitor(visitor);
        }
        String[] result = visitor.dependencies.toArray(new String[0]);
        visitor.dependencies.clear();
        return result;
    }

    static final class ExtractingVisitor extends RecursiveVisitor {
        final Set<String> dependencies = new HashSet<>();

        @Override
        public void visit(InvocationExpr expr) {
            super.visit(expr);
            dependencies.add(expr.getMethod().getClassName());
        }

        @Override
        public void visit(QualificationExpr expr) {
            super.visit(expr);
            dependencies.add(expr.getField().getClassName());
        }
    }
}
