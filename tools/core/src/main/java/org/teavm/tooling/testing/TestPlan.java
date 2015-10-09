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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class TestPlan {
    private String runtimeScript;
    private List<TestGroup> groups = new ArrayList<>();

    @JsonCreator
    public TestPlan(
            @JsonProperty("runtimeScript") String runtimeScript,
            @JsonProperty("groupList") List<TestGroup> groups) {
        this.runtimeScript = runtimeScript;
        this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
    }

    public String getRuntimeScript() {
        return runtimeScript;
    }

    public List<TestGroup> getGroups() {
        return groups;
    }
}
