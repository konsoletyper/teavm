/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.teavm.junit.PropertyNames.JS_DECODE_STACK;
import static org.teavm.junit.PropertyNames.JS_ENABLED;
import static org.teavm.junit.PropertyNames.JS_RUNNER;
import static org.teavm.junit.PropertyNames.MINIFIED;
import static org.teavm.junit.PropertyNames.OPTIMIZED;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.TeaVM;

class JSPlatformSupport extends TestPlatformSupport<JavaScriptTarget> {
    JSPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        String runStrategyName = System.getProperty(JS_RUNNER);
        if (runStrategyName != null) {
            switch (runStrategyName) {
                case "browser":
                    return new BrowserRunStrategy(outputDir, "JAVASCRIPT", BrowserRunStrategy::customBrowser);
                case "browser-chrome":
                    return new BrowserRunStrategy(outputDir, "JAVASCRIPT", BrowserRunStrategy::chromeBrowser);
                case "browser-firefox":
                    return new BrowserRunStrategy(outputDir, "JAVASCRIPT", BrowserRunStrategy::firefoxBrowser);
                case "none":
                    return null;
                default:
                    throw new RuntimeException("Unknown run strategy: " + runStrategyName);
            }
        }
        return null;
    }

    @Override
    TestPlatform getPlatform() {
        return TestPlatform.JAVASCRIPT;
    }

    @Override
    String getPath() {
        return "js";
    }

    @Override
    String getExtension() {
        return ".js";
    }

    @Override
    boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(JS_ENABLED, "true"));
    }

    @Override
    List<TeaVMTestConfiguration<JavaScriptTarget>> getConfigurations() {
        List<TeaVMTestConfiguration<JavaScriptTarget>> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.JS_DEFAULT);
        if (Boolean.getBoolean(MINIFIED)) {
            configurations.add(TeaVMTestConfiguration.JS_MINIFIED);
        }
        if (Boolean.getBoolean(OPTIMIZED)) {
            configurations.add(TeaVMTestConfiguration.JS_OPTIMIZED);
        }
        return configurations;
    }

    @Override
    CompileResult compile(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<JavaScriptTarget> configuration, File path) {
        boolean decodeStack = Boolean.parseBoolean(System.getProperty(JS_DECODE_STACK, "true"));
        var debugEmitter = new DebugInformationBuilder(new ReferenceCache());
        Supplier<JavaScriptTarget> targetSupplier = () -> {
            JavaScriptTarget target = new JavaScriptTarget();
            target.setStrict(true);
            if (decodeStack) {
                target.setDebugEmitter(debugEmitter);
                target.setStackTraceIncluded(true);
            }
            return target;
        };
        CompilePostProcessor postBuild = null;
        if (decodeStack) {
            postBuild = (vm, file) -> {
                DebugInformation debugInfo = debugEmitter.getDebugInformation();
                File sourceMapsFile = new File(file.getPath() + ".map");
                File debugFile = new File(file.getPath() + ".teavmdbg");
                try {
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, true), UTF_8)) {
                        writer.write("\n//# sourceMappingURL=");
                        writer.write(sourceMapsFile.getName());
                    }

                    try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(sourceMapsFile), UTF_8)) {
                        debugInfo.writeAsSourceMaps(sourceMapsOut, "", file.getPath());
                    }

                    try (OutputStream out = new FileOutputStream(debugFile)) {
                        debugInfo.write(out);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }
        return compile(configuration, targetSupplier, TestJsEntryPoint.class.getName(), path, ".js",
                postBuild, additionalProcessing, baseName);
    }

    @Override
    void additionalOutput(File outputPath, File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlOutput(outputPath, outputPathForMethod, configuration, reference, "teavm-run-test.html");
    }

    @Override
    void additionalSingleTestOutput(File outputPathForMethod, TeaVMTestConfiguration<?> configuration,
            MethodReference reference) {
        htmlSingleTestOutput(outputPathForMethod, configuration, "teavm-run-test.html");
    }

    @Override
    boolean usesFileName() {
        return true;
    }
}
