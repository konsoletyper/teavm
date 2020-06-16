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

import java.util.List;

public class RecursiveVisitor implements ExprVisitor, StatementVisitor {
    protected void beforeVisit(Expr expr) {
    }

    protected void afterVisit(Expr expr) {
    }

    protected boolean canceled;

    protected final void cancel() {
        canceled = true;
    }

    @Override
    public void visit(BinaryExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getFirstOperand().acceptVisitor(this);
            if (!canceled) {
                expr.getSecondOperand().acceptVisitor(this);
            }
        }
        afterVisit(expr);
    }

    @Override
    public void visit(UnaryExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getOperand().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
            if (canceled) {
                return;
            }
        }
        statement.getRightValue().acceptVisitor(this);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getCondition().acceptVisitor(this);
            if (!canceled) {
                expr.getConsequent().acceptVisitor(this);
                if (!canceled) {
                    expr.getAlternative().acceptVisitor(this);
                }
            }
        }
        afterVisit(expr);
    }

    public void visit(List<Statement> statements) {
        for (Statement part : statements) {
            part.acceptVisitor(this);
            if (canceled) {
                break;
            }
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        visit(statement.getSequence());
    }

    @Override
    public void visit(ConstantExpr expr) {
        beforeVisit(expr);
        afterVisit(expr);
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        visit(statement.getConsequent());
        if (!canceled) {
            visit(statement.getAlternative());
        }
    }

    @Override
    public void visit(VariableExpr expr) {
        beforeVisit(expr);
        afterVisit(expr);
    }

    @Override
    public void visit(SubscriptExpr expr) {
        beforeVisit(expr);
        expr.getArray().acceptVisitor(this);
        if (!canceled) {
            expr.getIndex().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        if (canceled) {
            return;
        }
        for (SwitchClause clause : statement.getClauses()) {
            visit(clause.getBody());
            if (canceled) {
                return;
            }
        }
        if (!canceled) {
            visit(statement.getDefaultClause());
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getArray().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
            if (canceled) {
                return;
            }
        }
        visit(statement.getBody());
    }

    @Override
    public void visit(InvocationExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            for (Expr argument : expr.getArguments()) {
                argument.acceptVisitor(this);
                if (canceled) {
                    break;
                }
            }
        }
        afterVisit(expr);
    }

    @Override
    public void visit(BlockStatement statement) {
        visit(statement.getBody());
    }

    @Override
    public void visit(QualificationExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
            }
        }
        afterVisit(expr);
    }

    @Override
    public void visit(BreakStatement statement) {
    }

    @Override
    public void visit(NewExpr expr) {
        beforeVisit(expr);
        afterVisit(expr);
    }

    @Override
    public void visit(ContinueStatement statement) {
    }

    @Override
    public void visit(NewArrayExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getLength().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            for (Expr dimension : expr.getDimensions()) {
                dimension.acceptVisitor(this);
                if (canceled) {
                    break;
                }
            }
        }
        afterVisit(expr);
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            for (Expr element : expr.getData()) {
                element.acceptVisitor(this);
                if (canceled) {
                    break;
                }
            }
        }
        afterVisit(expr);
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            statement.getResult().acceptVisitor(this);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getExpr().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
    }

    @Override
    public void visit(CastExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getValue().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getValue().acceptVisitor(this);
        }
        afterVisit(expr);
    }

    @Override
    public void visit(TryCatchStatement statement) {
        visit(statement.getProtectedBody());
        visit(statement.getHandler());
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        statement.getObjectRef().acceptVisitor(this);
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        statement.getObjectRef().acceptVisitor(this);
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        beforeVisit(expr);
        if (!canceled) {
            expr.getIndex().acceptVisitor(this);
            if (!canceled && expr.getArray() != null) {
                expr.getArray().acceptVisitor(this);
            }
        }
        afterVisit(expr);
    }
}
