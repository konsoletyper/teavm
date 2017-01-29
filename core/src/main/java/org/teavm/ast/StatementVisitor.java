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

public interface StatementVisitor {
    void visit(AssignmentStatement statement);

    void visit(SequentialStatement statement);

    void visit(ConditionalStatement statement);

    void visit(SwitchStatement statement);

    void visit(WhileStatement statement);

    void visit(BlockStatement statement);

    void visit(BreakStatement statement);

    void visit(ContinueStatement statement);

    void visit(ReturnStatement statement);

    void visit(ThrowStatement statement);

    void visit(InitClassStatement statement);

    void visit(TryCatchStatement statement);

    void visit(GotoPartStatement statement);

    void visit(MonitorEnterStatement statement);

    void visit(MonitorExitStatement statement);
}
