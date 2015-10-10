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
package org.teavm.tooling.testing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class TestCase {
    private String testMethod;
    private String testScript;
    private String debugTable;
    private List<String> expectedExceptions = new ArrayList<>();

    @JsonCreator
    public TestCase(
            @JsonProperty("testMethod") String testMethod,
            @JsonProperty("script") String testScript,
            @JsonProperty("debugTable") String debugTable,
            @JsonProperty("expectedExceptions") List<String> expectedExceptions) {
        this.testMethod = testMethod;
        this.testScript = testScript;
        this.debugTable = debugTable;
        this.expectedExceptions = Collections.unmodifiableList(new ArrayList<>(expectedExceptions));
    }

    @JsonGetter
    public String getTestMethod() {
        return testMethod;
    }

    @JsonGetter("script")
    public String getTestScript() {
        return testScript;
    }

    @JsonGetter
    public String getDebugTable() {
        return debugTable;
    }

    @JsonGetter
    public List<String> getExpectedExceptions() {
        return expectedExceptions;
    }
}
