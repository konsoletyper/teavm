/*
 *  Copyright 2015 Alexey Andreev.
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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class AsyncMethodPart {
    private Statement statement;
    private Integer inputVariable;
    private List<AsyncMethodCatch> catches = new ArrayList<>();

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public Integer getInputVariable() {
        return inputVariable;
    }

    public void setInputVariable(Integer inputVariable) {
        this.inputVariable = inputVariable;
    }

    public List<AsyncMethodCatch> getCatches() {
        return catches;
    }
}
