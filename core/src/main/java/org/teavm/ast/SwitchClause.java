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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwitchClause {
    private int[] conditions;
    private List<Statement> body = new ArrayList<>();

    public int[] getConditions() {
        return conditions != null ? Arrays.copyOf(conditions, conditions.length) : null;
    }

    public void setConditions(int[] conditions) {
        this.conditions = conditions != null ? Arrays.copyOf(conditions, conditions.length) : null;
    }

    public List<Statement> getBody() {
        return body;
    }
}
