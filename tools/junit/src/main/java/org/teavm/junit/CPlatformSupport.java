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

import static org.teavm.junit.PropertyNames.C_COMPILER;
import static org.teavm.junit.PropertyNames.C_ENABLED;
import static org.teavm.junit.PropertyNames.C_LINE_NUMBERS;
import static org.teavm.junit.PropertyNames.OPTIMIZED;
import static org.teavm.junit.TestUtil.resourceToFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.vm.TeaVM;

class CPlatformSupport extends TestPlatformSupport<CTarget> {
    CPlatformSupport(ClassHolderSource classSource, ReferenceCache referenceCache) {
        super(classSource, referenceCache);
    }

    @Override
    TestRunStrategy createRunStrategy(File outputDir) {
        String cCommand = System.getProperty(C_COMPILER);
        if (cCommand != null) {
            return new CRunStrategy(cCommand);
        }
        return null;
    }

    @Override
    TestPlatform getPlatform() {
        return TestPlatform.C;
    }

    @Override
    String getPath() {
        return "c";
    }

    @Override
    String getExtension() {
        return "";
    }

    @Override
    boolean isEnabled() {
        return Boolean.getBoolean(C_ENABLED);
    }

    @Override
    List<TeaVMTestConfiguration<CTarget>> getConfigurations() {
        List<TeaVMTestConfiguration<CTarget>> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.C_DEFAULT);
        if (Boolean.getBoolean(OPTIMIZED)) {
            configurations.add(TeaVMTestConfiguration.C_OPTIMIZED);
        }
        return configurations;
    }

    @Override
    CompileResult compile(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<CTarget> configuration, File path, AnnotatedElement element) {
        CompilePostProcessor postBuild = (vm, file) -> {
            try {
                resourceToFile("teavm-CMakeLists.txt", new File(file.getParent(), "CMakeLists.txt"),
                        Collections.emptyMap());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        var result = compile(configuration, this::createCTarget, TestNativeEntryPoint.class.getName(), path, "",
                postBuild, additionalProcessing, baseName);
        if (result.success) {
            includeAndAttachAdditionalFiles(element, result.file);
        }
        return result;
    }

    private void includeAndAttachAdditionalFiles(AnnotatedElement element, File baseFile) {
        var cls = element instanceof Method ? ((Method) element).getDeclaringClass() : (Class<?>) element;
        var attachC = element.getAnnotation(AttachC.class);
        if (attachC != null) {
            var fileNamesToAdd = new ArrayList<String>();
            for (var resourceName : attachC.value()) {
                var fileName = "custom-src/" + resourceName.substring(resourceName.lastIndexOf('/') + 1);
                fileNamesToAdd.add(fileName);
                var outputFile = new File(baseFile, fileName);
                outputFile.getParentFile().mkdirs();
                try (var input = cls.getClassLoader().getResourceAsStream(resourceName);
                        var output = new FileOutputStream(outputFile)) {
                    input.transferTo(output);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try (var output = new FileWriter(new File(baseFile, "all.c"), StandardCharsets.UTF_8, true)) {
                output.append("\n");
                for (var fileName : fileNamesToAdd) {
                    output.append("#include \"").append(fileName).append("\"\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        var includeC = element.getAnnotation(IncludeC.class);
        if (includeC != null) {
            for (var resourceName : includeC.value()) {
                var fileName = "custom-include/" + resourceName.substring(resourceName.lastIndexOf('/') + 1);
                var outputFile = new File(baseFile, fileName);
                outputFile.getParentFile().mkdirs();
                try (var input = cls.getClassLoader().getResourceAsStream(resourceName);
                        var output = new FileOutputStream(outputFile)) {
                    input.transferTo(output);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private CTarget createCTarget() {
        CTarget cTarget = new CTarget(new CNameProvider());
        cTarget.setLineNumbersGenerated(Boolean.parseBoolean(System.getProperty(C_LINE_NUMBERS, "false")));
        return cTarget;
    }

    @Override
    boolean usesFileName() {
        return false;
    }
}
