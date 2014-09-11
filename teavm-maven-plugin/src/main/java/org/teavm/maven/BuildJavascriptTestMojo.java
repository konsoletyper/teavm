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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FilenameUtils;
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
import org.teavm.model.ClassHolderTransformer;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.tooling.TeaVMTestTool;
import org.teavm.tooling.TeaVMToolException;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "build-test-javascript", requiresDependencyResolution = ResolutionScope.TEST,
        requiresDependencyCollection = ResolutionScope.TEST)
public class BuildJavascriptTestMojo extends AbstractMojo {
    private static Set<String> testScopes = new HashSet<>(Arrays.asList(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME,
            Artifact.SCOPE_PROVIDED));

    @Component
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/javascript-test")
    private File outputDir;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testFiles;

    @Parameter
    private String[] wildcards = { "**.*Test", "**.*UnitTest" };

    @Parameter
    private boolean minifying = true;

    @Parameter
    private boolean scanDependencies;

    @Parameter
    private int numThreads = 1;

    @Parameter
    private String adapterClass = JUnitTestAdapter.class.getName();

    @Parameter
    private String[] transformers;

    @Parameter
    private String[] additionalScripts;

    @Parameter
    private Properties properties;

    @Parameter
    private boolean incremental;

    @Parameter
    private boolean debugInformationGenerated;

    @Parameter
    private boolean sourceMapsGenerated;

    private TeaVMTestTool tool = new TeaVMTestTool();

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

    public void setAdapterClass(String adapterClass) {
        this.adapterClass = adapterClass;
    }

    public void setWildcards(String[] wildcards) {
        this.wildcards = wildcards;
    }

    public String[] getTransformers() {
        return transformers;
    }

    public void setTransformers(String[] transformers) {
        this.transformers = transformers;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public boolean isSourceMapsGenerated() {
        return sourceMapsGenerated;
    }

    public void setSourceMapsGenerated(boolean sourceMapsGenerated) {
        this.sourceMapsGenerated = sourceMapsGenerated;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (System.getProperty("maven.test.skip", "false").equals("true") ||
                System.getProperty("skipTests") != null) {
            getLog().info("Tests build skipped as specified by system property");
            return;
        }
        try {
            final ClassLoader classLoader = prepareClassLoader();
            getLog().info("Searching for tests in the directory `" + testFiles.getAbsolutePath() + "'");
            tool.setClassLoader(classLoader);
            tool.setAdapter(createAdapter(classLoader));
            findTestClasses(classLoader, testFiles, "");
            if (scanDependencies) {
                findTestsInDependencies(classLoader);
            }
            tool.getTransformers().addAll(instantiateTransformers(classLoader));
            tool.setLog(new MavenTeaVMToolLog(getLog()));
            tool.setOutputDir(outputDir);
            tool.setNumThreads(numThreads);
            tool.setMinifying(minifying);
            tool.setIncremental(incremental);
            tool.setDebugInformationGenerated(debugInformationGenerated);
            tool.setSourceMapsGenerated(sourceMapsGenerated);
            if (properties != null) {
                tool.getProperties().putAll(properties);
            }
            if (additionalScripts != null) {
                tool.getAdditionalScripts().addAll(Arrays.asList(additionalScripts));
            }
            tool.generate();
        } catch (TeaVMToolException e) {
            throw new MojoFailureException("Error occured generating JavaScript files", e);
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
            throw new MojoExecutionException("Adapter " + adapterClass + " does not implement " +
                    TestAdapter.class.getName());
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

    private ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Preparing classpath for JavaScript test generation");
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
            log.info("Using the following classpath for JavaScript test generation: " + classpath);
            return new URLClassLoader(urls.toArray(new URL[urls.size()]),
                    BuildJavascriptTestMojo.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
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


    private List<ClassHolderTransformer> instantiateTransformers(ClassLoader classLoader)
            throws MojoExecutionException {
        List<ClassHolderTransformer> transformerInstances = new ArrayList<>();
        if (transformers == null) {
            return transformerInstances;
        }
        for (String transformerName : transformers) {
            Class<?> transformerRawType;
            try {
                transformerRawType = Class.forName(transformerName, true, classLoader);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Transformer not found: " + transformerName, e);
            }
            if (!ClassHolderTransformer.class.isAssignableFrom(transformerRawType)) {
                throw new MojoExecutionException("Transformer " + transformerName + " is not subtype of " +
                        ClassHolderTransformer.class.getName());
            }
            Class<? extends ClassHolderTransformer> transformerType = transformerRawType.asSubclass(
                    ClassHolderTransformer.class);
            Constructor<? extends ClassHolderTransformer> ctor;
            try {
                ctor = transformerType.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new MojoExecutionException("Transformer " + transformerName + " has no default constructor");
            }
            try {
                ClassHolderTransformer transformer = ctor.newInstance();
                transformerInstances.add(transformer);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new MojoExecutionException("Error instantiating transformer " + transformerName, e);
            }
        }
        return transformerInstances;
    }
}
