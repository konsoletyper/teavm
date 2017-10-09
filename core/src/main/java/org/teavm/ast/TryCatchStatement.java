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
import java.util.List;

public class TryCatchStatement extends Statement {
    private List<Statement> protectedBody = new ArrayList<>();
    private List<Statement> handler = new ArrayList<>();
    private String exceptionType;
    private Integer exceptionVariable;
    private boolean async;

    public List<Statement> getProtectedBody() {
        return protectedBody;
    }

    public List<Statement> getHandler() {
        return handler;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public Integer getExceptionVariable() {
        return exceptionVariable;
    }

    public void setExceptionVariable(Integer exceptionVariable) {
        this.exceptionVariable = exceptionVariable;
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
