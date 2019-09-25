/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import java.io.IOException;
import java.io.Reader;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;

public class JSParser extends Parser {
    public JSParser(Object compilerEnv, Object errorReporter) {
        this((CompilerEnvirons) compilerEnv, (ErrorReporter) errorReporter);
    }

    public JSParser(CompilerEnvirons compilerEnv, ErrorReporter errorReporter) {
        super(compilerEnv, errorReporter);
    }

    public JSParser(Object compilerEnv) {
        this((CompilerEnvirons) compilerEnv);
    }

    public JSParser(CompilerEnvirons compilerEnv) {
        super(compilerEnv);
    }

    public void enterFunction() {
        ++nestingOfFunction;
    }

    public void exitFunction() {
        --nestingOfFunction;
    }

    public Object parseAsObject(Reader sourceReader, String sourceURI, int lineno) throws IOException {
        return parse(sourceReader, sourceURI, lineno);
    }
}
