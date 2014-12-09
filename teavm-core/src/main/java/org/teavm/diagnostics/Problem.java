/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.diagnostics;

import java.util.Arrays;
import org.teavm.model.CallLocation;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class Problem {
    private ProblemSeverity severity;
    private CallLocation location;
    private String text;
    private Object[] params;

    public Problem(ProblemSeverity severity, CallLocation location, String text, Object[] params) {
        this.severity = severity;
        this.location = location;
        this.text = text;
        this.params = Arrays.copyOf(params, params.length);
    }

    public ProblemSeverity getSeverity() {
        return severity;
    }

    public CallLocation getLocation() {
        return location;
    }

    public String getText() {
        return text;
    }

    public Object[] getParams() {
        return params;
    }
}
