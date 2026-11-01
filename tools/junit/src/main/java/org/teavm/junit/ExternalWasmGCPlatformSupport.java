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
package org.teavm.junit;

import static org.teavm.junit.PropertyNames.WASM_GC_DISASM;
import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Properties;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.browserrunner.BrowserRunner;
import org.teavm.junit.spi.ExternalCompilationRequest;
import org.teavm.junit.spi.ExternalTestPlatform;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.TeaVMOptimizationLevel;

class ExternalWasmGCPlatformSupport extends WebAssemblyGCPlatformSupport {
    private final ExternalTestPlatform external;

    ExternalWasmGCPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache,
            ExternalTestPlatform external) {
        super(classSource, referenceCache, Boolean.getBoolean(WASM_GC_DISASM));
        this.external = external;
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        var runner = external.getBrowserRunner();
        return runner != null
                ? new BrowserRunStrategy(outputDir, "WASM_GC", BrowserRunner.pickBrowser(runner))
                : null;
    }

    @Override
    CompileResult compile(TestEntryPointSpec entryPoint, String baseName,
            TeaVMTestConfiguration<WasmGCTarget> configuration, File path, AnnotatedElement element) {
        var result = new CompileResult();
        result.file = getOutputFile(path, baseName, configuration.getSuffix(), getExtension());
        var optimizationLevel = configuration == TeaVMTestConfiguration.WASM_GC_OPTIMIZED
                ? TeaVMOptimizationLevel.FULL
                : TeaVMOptimizationLevel.SIMPLE;
        result.file.getParentFile().mkdirs();
        try {
            var externalResult = external.compile(new RequestImpl(entryPoint, optimizationLevel, result.file));
            if (!externalResult.isSuccess()) {
                result.success = false;
                result.errorMessage = externalResult.getErrorMessage();
            }
        } catch (Exception e) {
            result.success = false;
            result.throwable = e;
        }
        return result;
    }

    @Override
    String getId() {
        return external.getName();
    }

    @Override
    String getPath() {
        return external.getName();
    }

    @Override
    boolean isEnabled() {
        return external.isEnabled();
    }

    void close() {
        external.close();
    }

    private static class RequestImpl implements ExternalCompilationRequest {
        private final TestEntryPointSpec spec;
        private final TeaVMOptimizationLevel optimizationLevel;
        private final File outputFile;

        RequestImpl(TestEntryPointSpec spec, TeaVMOptimizationLevel optimizationLevel, File outputFile) {
            this.spec = spec;
            this.optimizationLevel = optimizationLevel;
            this.outputFile = outputFile;
        }

        @Override
        public String getEntryPointClassName() {
            return TestWasmGCEntryPoint.class.getName();
        }

        @Override
        public String getTestClassName() {
            return spec.testClassName;
        }

        @Override
        public List<MethodReference> getTestMethods() {
            return spec.testMethods;
        }

        @Override
        public boolean isWholeClass() {
            return spec.wholeClass;
        }

        @Override
        public String getParameterizedPlans() {
            return spec.plans.isEmpty() ? "" : JupiterArgumentsPlanCodec.encode(spec.plans);
        }

        @Override
        public Properties getProperties() {
            return spec.properties;
        }

        @Override
        public TeaVMOptimizationLevel getOptimizationLevel() {
            return optimizationLevel;
        }

        @Override
        public File getOutputFile() {
            return outputFile;
        }

        @Override
        public ClassLoader getClassLoader() {
            return TeaVMTestInfrastructure.classLoader;
        }
    }
}
