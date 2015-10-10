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
package org.teavm.maven;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class TestResult {
    private MethodReference method;
    private TestStatus status;
    private String exception;
    private String stack;

    @JsonCreator
    TestResult(
            @JsonProperty("method") MethodReference method,
            @JsonProperty("status") TestStatus status,
            @JsonInclude(Include.NON_NULL) @JsonProperty("exception") String exception,
            @JsonInclude(Include.NON_NULL) @JsonProperty("stack") String stack) {
        this.method = method;
        this.status = status;
        this.exception = exception;
        this.stack = stack;
    }

    public static TestResult passed(MethodReference method) {
        return new TestResult(method, TestStatus.PASSED, null, null);
    }

    public static TestResult exceptionNotThrown(MethodReference method) {
        return new TestResult(method, TestStatus.EXCEPTION_NOT_THROWN, null, null);
    }

    public static TestResult error(MethodReference method, String exception, String stack) {
        return new TestResult(method, TestStatus.ERROR, exception, stack);
    }

    @JsonGetter
    public MethodReference getMethod() {
        return method;
    }

    @JsonGetter
    public TestStatus getStatus() {
        return status;
    }

    @JsonGetter
    public String getException() {
        return exception;
    }

    @JsonGetter
    public String getStack() {
        return stack;
    }
}
