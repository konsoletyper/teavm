/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

public interface BlockVisitor {
    void visit(SimpleBlock block);

    void visit(BreakBlock block);

    void visit(ContinueBlock block);

    void visit(ReturnBlock block);

    void visit(ThrowBlock block);

    void visit(SimpleLabeledBlock block);

    void visit(SwitchBlock block);

    void visit(IfBlock block);

    void visit(LoopBlock block);

    void visit(TryBlock block);
}
