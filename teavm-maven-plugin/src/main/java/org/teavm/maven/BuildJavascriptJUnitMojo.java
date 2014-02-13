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

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.teavm.common.FiniteExecutor;
import org.teavm.common.SimpleFiniteExecutor;
import org.teavm.common.ThreadPoolFiniteExecutor;
import org.teavm.javascript.DirectoryBuildTarget;
import org.teavm.javascript.JavascriptBuilder;
import org.teavm.javascript.JavascriptBuilderFactory;
import org.teavm.model.*;
import org.teavm.parsing.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "build-junit", requiresDependencyResolution = ResolutionScope.TEST,
        requiresDependencyCollection = ResolutionScope.TEST)
public class BuildJavascriptJUnitMojo extends AbstractMojo {
    private static Set<String> testScopes = new HashSet<>(Arrays.asList(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_PROVIDED));
    private Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private Map<MethodReference, String> fileNames = new HashMap<>();
    private List<MethodReference> testMethods = new ArrayList<>();
    private List<String> testClasses = new ArrayList<>();

    @Component
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/javascript-junit")
    private File outputDir;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testFiles;

    @Parameter
    private boolean minifying = true;

    @Parameter
    private int numThreads = 1;

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public void setClassFiles(File classFiles) {
        this.classFiles = classFiles;
    }

    public void setTestFiles(File testFiles) {
        this.testFiles = testFiles;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Runnable finalizer = null;
        try {
            final ClassLoader classLoader = prepareClassLoader();
            getLog().info("Searching for tests in the directory `" + testFiles.getAbsolutePath() + "'");
            findTestClasses(classLoader, testFiles, "");
            final Log log = getLog();
            new File(outputDir, "tests").mkdirs();
            resourceToFile("org/teavm/javascript/runtime.js", "runtime.js");
            resourceToFile("org/teavm/maven/junit-support.js", "junit-support.js");
            resourceToFile("org/teavm/maven/junit.css", "junit.css");
            resourceToFile("org/teavm/maven/junit.html", "junit.html");
            final ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
            for (String testClass : testClasses) {
                ClassHolder classHolder = classSource.get(testClass);
                if (classHolder == null) {
                    throw new MojoFailureException("Could not find class " + testClass);
                }
                findTests(classHolder);
            }

            File allTestsFile = new File(outputDir, "tests/all.js");
            try (Writer allTestsWriter = new OutputStreamWriter(new FileOutputStream(allTestsFile), "UTF-8")) {
                allTestsWriter.write("doRunTests = function() {\n");
                allTestsWriter.write("    new JUnitServer(document.body).runAllTests([");
                boolean first = true;
                for (String testClass : testClasses) {
                    if (!first) {
                        allTestsWriter.append(",");
                    }
                    first = false;
                    allTestsWriter.append("\n        { name : \"").append(testClass).append("\", methods : [");
                    boolean firstMethod = true;
                    for (MethodReference methodRef : groupedMethods.get(testClass)) {
                        String scriptName = "tests/" + fileNames.size() + ".js";
                        fileNames.put(methodRef, scriptName);
                        if (!firstMethod) {
                            allTestsWriter.append(",");
                        }
                        firstMethod = false;
                        allTestsWriter.append("\n            { name : \"" + methodRef.getName() + "\", script : \"" +
                                scriptName + "\", expected : [");
                        MethodHolder methodHolder = classSource.get(testClass).getMethod(
                                methodRef.getDescriptor());
                        AnnotationHolder annot = methodHolder.getAnnotations().get("org.junit.Test");
                        AnnotationValue expectedAnnot = annot.getValues().get("expected");
                        if (expectedAnnot != null) {
                            String className = ((ValueType.Object)expectedAnnot.getJavaClass()).getClassName();
                            allTestsWriter.append("\"" + className + "\"");
                        }
                        allTestsWriter.append("] }");
                    }
                    allTestsWriter.append("] }");
                }
                allTestsWriter.write("], function() {}); }");
            }
            int methodsGenerated = 0;
            log.info("Generating test files");
            FiniteExecutor executor = new SimpleFiniteExecutor();
            if (numThreads != 1) {
                int threads = numThreads != 0 ? numThreads : Runtime.getRuntime().availableProcessors();
                final ThreadPoolFiniteExecutor threadedExecutor = new ThreadPoolFiniteExecutor(threads);
                finalizer = new Runnable() {
                    @Override public void run() {
                        threadedExecutor.stop();
                    }
                };
                executor = threadedExecutor;
            }
            for (final MethodReference method : testMethods) {
                executor.execute(new Runnable() {
                    @Override public void run() {
                        log.debug("Building test for " + method);
                        try {
                            decompileClassesForTest(classLoader, new CopyClassHolderSource(classSource), method,
                                    fileNames.get(method), new SimpleFiniteExecutor());
                        } catch (IOException e) {
                            log.error("Error generating JavaScript", e);
                        }
                    }
                });
                ++methodsGenerated;
            }
            executor.complete();
            log.info("Test files successfully generated for " + methodsGenerated + " method(s).");
        } catch (IOException e) {
            throw new MojoFailureException("IO error occured generating JavaScript files", e);
        } finally {
            if (finalizer != null) {
                finalizer.run();
            }
        }
    }

    private ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Preparing classpath for JavaScript JUnit generation");
            List<URL> urls = new ArrayList<>();
            StringBuilder classpath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                if (!testScopes.contains(artifact.getScope())) {
                    continue;
                }
                File file = artifact.getFile();
                if (classpath.length() > 0) {
                    classpath.append(':');
                }
                classpath.append(file.getPath());
                urls.add(file.toURI().toURL());
            }
            if (classpath.length() > 0) {
                classpath.append(':');
            }
            classpath.append(testFiles.getPath());
            urls.add(testFiles.toURI().toURL());
            classpath.append(':').append(classFiles.getPath());
            urls.add(classFiles.toURI().toURL());
            log.info("Using the following classpath for JavaScript JUnit generation: " + classpath);
            return new URLClassLoader(urls.toArray(new URL[urls.size()]),
                    BuildJavascriptJUnitMojo.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
        }
    }

    private void decompileClassesForTest(ClassLoader classLoader, ClassHolderSource classSource,
            MethodReference methodRef, String targetName, FiniteExecutor executor) throws IOException {
        JavascriptBuilderFactory builderFactory = new JavascriptBuilderFactory();
        builderFactory.setClassLoader(classLoader);
        builderFactory.setClassSource(classSource);
        builderFactory.setExecutor(executor);
        JavascriptBuilder builder = builderFactory.create();
        builder.setMinifying(minifying);
        builder.installPlugins();
        builder.prepare();
        File file = new File(outputDir, targetName);
        try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            MethodReference cons = new MethodReference(methodRef.getClassName(),
                    new MethodDescriptor("<init>", ValueType.VOID));
            builder.entryPoint("initInstance", cons);
            builder.entryPoint("runTest", methodRef).withValue(0, cons.getClassName());
            builder.exportType("TestClass", cons.getClassName());
            builder.build(innerWriter, new DirectoryBuildTarget(outputDir));
            innerWriter.append("\n");
            innerWriter.append("\nJUnitClient.run();");
            innerWriter.close();
        }
    }

    private void findTestClasses(ClassLoader classLoader, File folder, String prefix) {
        Class<? extends Annotation> testAnnot;
        try {
            testAnnot = Class.forName(Test.class.getName(), true, classLoader).asSubclass(Annotation.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load `" + Test.class.getName() + "` annotation");
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                String newPrefix = prefix.isEmpty() ? file.getName() : prefix + "." + file.getName();
                findTestClasses(classLoader, file, newPrefix);
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - ".class".length());
                if (!prefix.isEmpty()) {
                    className = prefix + "." + className;
                }
                try {
                    Class<?> candidate = Class.forName(className, true, classLoader);
                    boolean hasTests = false;
                    for (Method method : candidate.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(testAnnot)) {
                            hasTests = true;
                            break;
                        }
                    }
                    if (hasTests) {
                        testClasses.add(candidate.getName());
                        getLog().info("Test class detected: " + candidate.getName());
                    }
                } catch (ClassNotFoundException e) {
                    getLog().info("Could not load class `" + className + "' to search for tests");
                }
            }
        }
    }

    private void findTests(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getAnnotations().get("org.junit.Test") != null) {
                MethodReference ref = new MethodReference(cls.getName(), method.getDescriptor());
                testMethods.add(ref);
                List<MethodReference> group = groupedMethods.get(cls.getName());
                if (group == null) {
                    group = new ArrayList<>();
                    groupedMethods.put(cls.getName(), group);
                }
                group.add(ref);
            }
        }
    }

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = BuildJavascriptJUnitMojo.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }
}
