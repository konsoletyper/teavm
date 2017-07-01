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
package org.teavm.jso.impl;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.CallLocation;

public class TeaVMErrorReporter implements ErrorReporter {
    private Diagnostics diagnostics;
    private CallLocation location;
    private boolean hasErrors;

    public TeaVMErrorReporter(Diagnostics diagnostics, CallLocation location) {
        this.diagnostics = diagnostics;
        this.location = location;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    @Override
    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        diagnostics.warning(location, "Warning in @JSBody script line " + line + ", char " + lineOffset
                + ": " + message);
    }

    @Override
    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        diagnostics.warning(location, "Error in @JSBody script line " + line + ", char " + lineOffset
                + ": " + message);
        hasErrors = true;
    }

    @Override
    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource,
            int lineOffset) {
        return null;
    }
}
