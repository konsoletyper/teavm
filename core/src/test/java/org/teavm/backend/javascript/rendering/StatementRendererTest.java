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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriterBuilder;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.analysis.ClassInitializerInfo;

public class StatementRendererTest {
    @Test
    public void verifyVariableNamesNotReserved() throws IOException {
        RenderingContext context = new RenderingContext(
                null, null, null, null, null, null, null, null, null, null, true
        );
        context.setMinifying(true);

        StatementRenderer renderer = new StatementRenderer(context, null);
        for (int i = 0; i < 100000; i++) {
            String name = renderer.variableName(i);
            assertFalse("Verifying '" + name + "' is not a reserved keyword.", RenderingUtil.KEYWORDS.contains(name));
        }
    }
}
