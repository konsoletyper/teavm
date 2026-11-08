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
package org.teavm.tooling.transformation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.teavm.extension.spi.transformation.ResourceTransformer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;

public class ResourceTransformerTest {
    private static final String REPLACEMENT = "PATCHED_MARKER!";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void transformsClassFromClassLoader() throws Exception {
        var tool = createTool(true);
        tool.getProperties().setProperty(TestResourceTransformer.REPLACEMENT_PROPERTY, REPLACEMENT);
        var output = build(tool);
        assertTrue("Generated code should contain transformed constant", output.contains(REPLACEMENT));
        assertFalse("Generated code should not contain original constant",
                output.contains(ResourceTransformerTestApplication.MARKER));
    }

    @Test
    public void transformsClassFromClassPath() throws Exception {
        var tool = createTool(true);
        tool.setClassPath(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new)
                .collect(Collectors.toList()));
        tool.getProperties().setProperty(TestResourceTransformer.REPLACEMENT_PROPERTY, REPLACEMENT);
        var output = build(tool);
        assertTrue("Generated code should contain transformed constant", output.contains(REPLACEMENT));
        assertFalse("Generated code should not contain original constant",
                output.contains(ResourceTransformerTestApplication.MARKER));
    }

    @Test
    public void keepsClassIntactWithoutTransformer() throws Exception {
        var tool = createTool(false);
        tool.getProperties().setProperty(TestResourceTransformer.REPLACEMENT_PROPERTY, REPLACEMENT);
        var output = build(tool);
        assertTrue("Generated code should contain original constant",
                output.contains(ResourceTransformerTestApplication.MARKER));
        assertFalse("Generated code should not contain transformed constant", output.contains(REPLACEMENT));
    }

    private TeaVMTool createTool(boolean registerTransformer) throws IOException {
        var tool = new TeaVMTool();
        tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        tool.setObfuscated(false);
        tool.setMainClass(ResourceTransformerTestApplication.class.getName());
        tool.setTargetDirectory(temporaryFolder.newFolder("target"));
        if (registerTransformer) {
            tool.setClassLoader(createClassLoaderWithTransformer());
        }
        return tool;
    }

    private ClassLoader createClassLoaderWithTransformer() throws IOException {
        var root = temporaryFolder.newFolder("services");
        var servicesDir = new File(root, "META-INF/services");
        servicesDir.mkdirs();
        Files.writeString(new File(servicesDir, ResourceTransformer.class.getName()).toPath(),
                TestResourceTransformer.class.getName() + "\n", StandardCharsets.UTF_8);
        return new URLClassLoader(new URL[] { root.toURI().toURL() }, getClass().getClassLoader());
    }

    private String build(TeaVMTool tool) throws TeaVMToolException, IOException {
        tool.generate();
        var problems = tool.getProblemProvider().getSevereProblems();
        assertTrue("Build should not report severe problems: " + problems, problems.isEmpty());
        var outputFile = new File(tool.getTargetDirectory(), "classes.js");
        assertTrue("Expected JS file to be generated at " + outputFile, outputFile.isFile());
        return Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
    }
}
