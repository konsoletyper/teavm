/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.devserver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.cache.MethodNodeCache;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.MemoryBuildTarget;

public class CodeServletJavaScriptBackend implements CodeServletBackend<JavaScriptTarget> {
    private DebugInformationBuilder debugInformationBuilder;

    @Override
    public JavaScriptTarget createTarget() {
        return new JavaScriptTarget();
    }

    @Override
    public void setup(JavaScriptTarget target, MethodNodeCache astCache, ReferenceCache referenceCache,
            CodeServletSettings settings) {
        debugInformationBuilder = new DebugInformationBuilder(referenceCache);
        target.setStackTraceIncluded(true);
        target.setObfuscated(false);
        target.setAstCache(astCache);
        target.setDebugEmitter(debugInformationBuilder);
        if (settings.jsModuleType() != null) {
            target.setModuleType(settings.jsModuleType());
        }
        target.setStrict(true);
    }

    @Override
    public void afterBuild(MemoryBuildTarget buildTarget, CodeServletSettings settings) {
        try {
            DebugInformation debugInformation = debugInformationBuilder.getDebugInformation();
            String sourceMapName = settings.fileName() + ".map";

            try (Writer writer = new OutputStreamWriter(buildTarget.appendToResource(settings.fileName()),
                    StandardCharsets.UTF_8)) {
                writer.append("\n//# sourceMappingURL=" + sourceMapName);
            }

            try (Writer writer = new OutputStreamWriter(buildTarget.createResource(sourceMapName),
                    StandardCharsets.UTF_8)) {
                debugInformation.writeAsSourceMaps(writer, "src", settings.fileName());
            }
            debugInformation.write(buildTarget.createResource(settings.fileName() + ".teavmdbg"));
        } catch (IOException e) {
            throw new RuntimeException("IO error occurred writing debug information", e);
        } finally {
            debugInformationBuilder = null;
        }
    }

    @Override
    public boolean isIndicatorSupported() {
        return true;
    }
}
