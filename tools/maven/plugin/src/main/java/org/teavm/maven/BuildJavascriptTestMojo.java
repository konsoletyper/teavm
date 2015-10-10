/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.testing.TeaVMTestTool;
import org.teavm.tooling.testing.TestPlan;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "testCompile", requiresDependencyResolution = ResolutionScope.TEST,
        requiresDependencyCollection = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class BuildJavascriptTestMojo extends AbstractJavascriptMojo {
    private static Set<String> testScopes = new HashSet<>(Arrays.asList(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_PROVIDED));
    @Parameter(defaultValue = "${project.build.directory}/javascript-test")
    private File targetDirectory;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testFiles;

    @Parameter
    private String[] wildcards = { "**.*Test", "**.*UnitTest" };

    @Parameter
    private String[] excludeWildcards = new String[0];

    @Parameter
    private boolean scanDependencies;

    @Parameter
    private int numThreads = 1;

    @Parameter
    private String adapterClass = JUnitTestAdapter.class.getName();

    @Parameter
    private String[] additionalScripts;

    private TeaVMTestTool tool = new TeaVMTestTool();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (System.getProperty("maven.test.skip", "false").equals("true")
                || System.getProperty("skipTests") != null) {
            getLog().info("Tests build skipped as specified by system property");
            return;
        }

        setupTool(tool);
        try {
            getLog().info("Searching for tests in the directory `" + testFiles.getAbsolutePath() + "'");
            tool.setAdapter(createAdapter(classLoader));
            findTestClasses(classLoader, testFiles, "");
            if (scanDependencies) {
                findTestsInDependencies(classLoader);
            }
            tool.setNumThreads(numThreads);
            if (additionalScripts != null) {
                tool.getAdditionalScripts().addAll(Arrays.asList(additionalScripts));
            }
            writePlan(tool.generate());
        } catch (TeaVMToolException e) {
            throw new MojoFailureException("Error occured generating JavaScript files", e);
        }
    }

    private void writePlan(TestPlan plan) throws MojoExecutionException {
        File file = new File(targetDirectory, "plan.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, plan);
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing test plan", e);
        }
    }

    private TestAdapter createAdapter(ClassLoader classLoader) throws MojoExecutionException {
        Class<?> adapterClsRaw;
        try {
            adapterClsRaw = Class.forName(adapterClass, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Adapter not found: " + adapterClass, e);
        }
        if (!TestAdapter.class.isAssignableFrom(adapterClsRaw)) {
            throw new MojoExecutionException("Adapter " + adapterClass + " does not implement "
                    + TestAdapter.class.getName());
        }
        Class<? extends TestAdapter> adapterCls = adapterClsRaw.asSubclass(TestAdapter.class);
        Constructor<? extends TestAdapter> cons;
        try {
            cons = adapterCls.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("No default constructor found for test adapter " + adapterClass, e);
        }
        try {
            return cons.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new MojoExecutionException("Error creating test adapter", e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Error creating test adapter", e.getTargetException());
        }
    }

    private void findTestsInDependencies(ClassLoader classLoader) throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Scanning dependencies for tests");
            for (Artifact artifact : project.getArtifacts()) {
                if (!testScopes.contains(artifact.getScope())) {
                    continue;
                }
                File file = artifact.getFile();
                if (file.isDirectory()) {
                    findTestClasses(classLoader, file, "");
                } else if (file.getName().endsWith(".jar")) {
                    findTestClassesInJar(classLoader, new JarFile(file));
                }
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning dependencies for tests", e);
        }
    }

    private void findTestClasses(ClassLoader classLoader, File folder, String prefix) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                String newPrefix = prefix.isEmpty() ? file.getName() : prefix + "." + file.getName();
                findTestClasses(classLoader, file, newPrefix);
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - ".class".length());
                if (!prefix.isEmpty()) {
                    className = prefix + "." + className;
                }
                addCandidate(classLoader, className);
            }
        }
    }

    private void findTestClassesInJar(ClassLoader classLoader, JarFile jarFile) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            String className = entry.getName().substring(0, entry.getName().length() - ".class".length())
                    .replace('/', '.');
            addCandidate(classLoader, className);
        }
    }

    private void addCandidate(ClassLoader classLoader, String className) {
        boolean matches = false;
        String simpleName = className.replace('.', '/');
        for (String wildcard : wildcards) {
            if (FilenameUtils.wildcardMatch(simpleName, wildcard.replace('.', '/'))) {
                matches = true;
                break;
            }
        }
        if (!matches) {
            return;
        }

        for (String wildcard : excludeWildcards) {
            if (FilenameUtils.wildcardMatch(simpleName, wildcard.replace('.', '/'))) {
                return;
            }
        }

        try {
            Class<?> candidate = Class.forName(className, true, classLoader);
            if (tool.getAdapter().acceptClass(candidate)) {
                tool.getTestClasses().add(candidate.getName());
                getLog().info("Test class detected: " + candidate.getName());
            }
        } catch (ClassNotFoundException e) {
            getLog().info("Could not load class `" + className + "' to search for tests");
        }
    }

    @Override
    protected File getTargetDirectory() {
        return targetDirectory;
    }

    @Override
    protected List<File> getAdditionalClassPath() {
        return Arrays.asList(testFiles);
    }

    @Override
    protected boolean isSupportedScope(String scope) {
        return testScopes.contains(scope);
    }
}
