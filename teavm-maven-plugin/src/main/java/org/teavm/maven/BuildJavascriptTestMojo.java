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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FilenameUtils;
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
import org.teavm.common.FiniteExecutor;
import org.teavm.common.SimpleFiniteExecutor;
import org.teavm.common.ThreadPoolFiniteExecutor;
import org.teavm.model.*;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

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
    private Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private Map<MethodReference, String> fileNames = new HashMap<>();
    private List<MethodReference> testMethods = new ArrayList<>();
    private List<String> testClasses = new ArrayList<>();
    private TestAdapter adapter;

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

    private List<ClassHolderTransformer> transformerInstances;

    @Parameter
    private String[] additionalScripts;

    private List<String> additionalScriptLocalPaths = new ArrayList<>();

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Runnable finalizer = null;
        try {
            final ClassLoader classLoader = prepareClassLoader();
            createAdapter(classLoader);
            getLog().info("Searching for tests in the directory `" + testFiles.getAbsolutePath() + "'");
            findTestClasses(classLoader, testFiles, "");
            if (scanDependencies) {
                findTestsInDependencies(classLoader);
            }
            final Log log = getLog();
            new File(outputDir, "tests").mkdirs();
            new File(outputDir, "res").mkdirs();
            resourceToFile("org/teavm/javascript/runtime.js", "res/runtime.js");
            resourceToFile("org/teavm/maven/res/junit-support.js", "res/junit-support.js");
            resourceToFile("org/teavm/maven/res/junit.css", "res/junit.css");
            resourceToFile("org/teavm/maven/res/class_obj.png", "res/class_obj.png");
            resourceToFile("org/teavm/maven/res/control-000-small.png", "res/control-000-small.png");
            resourceToFile("org/teavm/maven/res/methpub_obj.png", "res/methpub_obj.png");
            resourceToFile("org/teavm/maven/res/package_obj.png", "res/package_obj.png");
            resourceToFile("org/teavm/maven/res/tick-small-red.png", "res/tick-small-red.png");
            resourceToFile("org/teavm/maven/res/tick-small.png", "res/tick-small.png");
            resourceToFile("org/teavm/maven/res/toggle-small-expand.png", "res/toggle-small-expand.png");
            resourceToFile("org/teavm/maven/res/toggle-small.png", "res/toggle-small.png");
            resourceToFile("org/teavm/maven/junit.html", "junit.html");
            final ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
            for (String testClass : testClasses) {
                ClassHolder classHolder = classSource.get(testClass);
                if (classHolder == null) {
                    throw new MojoFailureException("Could not find class " + testClass);
                }
                findTests(classHolder);
            }

            transformerInstances = instantiateTransformers(classLoader);
            includeAdditionalScripts(classLoader);
            File allTestsFile = new File(outputDir, "tests/all.js");
            try (Writer allTestsWriter = new OutputStreamWriter(new FileOutputStream(allTestsFile), "UTF-8")) {
                allTestsWriter.write("prepare = function() {\n");
                allTestsWriter.write("    return new JUnitServer(document.body).readTests([");
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
                        boolean firstException = true;
                        for (String exception : adapter.getExpectedExceptions(methodHolder)) {
                            if (!firstException) {
                                allTestsWriter.append(", ");
                            }
                            firstException = false;
                            allTestsWriter.append("\"" + exception + "\"");
                        }
                        allTestsWriter.append("], additionalScripts : [");
                        for (int i = 0; i < additionalScriptLocalPaths.size(); ++i) {
                            if (i > 0) {
                                allTestsWriter.append(", ");
                            }
                            escapeString(additionalScriptLocalPaths.get(i), allTestsWriter);
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

    private void createAdapter(ClassLoader classLoader) throws MojoExecutionException {
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
            adapter = cons.newInstance();
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

    private void decompileClassesForTest(ClassLoader classLoader, ClassHolderSource classSource,
            MethodReference methodRef, String targetName, FiniteExecutor executor) throws IOException {
        TeaVM vm = new TeaVMBuilder()
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .setExecutor(executor)
                .build();
        vm.setMinifying(minifying);
        vm.installPlugins();
        new TestExceptionPlugin().install(vm);
        for (ClassHolderTransformer transformer : transformerInstances) {
            vm.add(transformer);
        }
        File file = new File(outputDir, targetName);
        try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            MethodReference cons = new MethodReference(methodRef.getClassName(),
                    new MethodDescriptor("<init>", ValueType.VOID));
            MethodReference exceptionMsg = new MethodReference("java.lang.Throwable", "getMessage",
                    ValueType.object("java.lang.String"));
            vm.entryPoint("initInstance", cons);
            vm.entryPoint("runTest", methodRef).withValue(0, cons.getClassName());
            vm.entryPoint("extractException", exceptionMsg);
            vm.exportType("TestClass", cons.getClassName());
            vm.build(innerWriter, new DirectoryBuildTarget(outputDir));
            if (!vm.hasMissingItems()) {
                innerWriter.append("\n");
                innerWriter.append("\nJUnitClient.run();");
                innerWriter.close();
            } else {
                innerWriter.append("JUnitClient.reportError(\n");
                StringBuilder sb = new StringBuilder();
                vm.showMissingItems(sb);
                escapeStringLiteral(sb.toString(), innerWriter);
                innerWriter.append(");");
                getLog().warn("Error building test " + methodRef);
                getLog().warn(sb);
            }
        }
    }

    private void escapeStringLiteral(String text, Writer writer) throws IOException {
        int index = 0;
        while (true) {
            int next = text.indexOf('\n', index);
            if (next < 0) {
                break;
            }
            escapeString(text.substring(index, next + 1), writer);
            writer.append(" +\n");
            index = next + 1;
        }
        escapeString(text.substring(index), writer);
    }

    private void escapeString(String string, Writer writer) throws IOException {
        writer.append('\"');
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                    writer.append("\\\"");
                    break;
                case '\\':
                    writer.append("\\\\");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                default:
                    writer.append(c);
                    break;
            }
        }
        writer.append('\"');
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
            if (adapter.acceptClass(candidate)) {
                testClasses.add(candidate.getName());
                getLog().info("Test class detected: " + candidate.getName());
            }
        } catch (ClassNotFoundException e) {
            getLog().info("Could not load class `" + className + "' to search for tests");
        }
    }

    private void findTests(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (adapter.acceptMethod(method)) {
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
        try (InputStream input = BuildJavascriptTestMojo.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                IOUtils.copy(input, output);
            }
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

    private void includeAdditionalScripts(ClassLoader classLoader) throws MojoExecutionException {
        if (additionalScripts == null) {
            return;
        }
        for (String script : additionalScripts) {
            String simpleName = script.substring(script.lastIndexOf('/') + 1);
            additionalScriptLocalPaths.add("tests/" + simpleName);
            if (classLoader.getResource(script) == null) {
                throw new MojoExecutionException("Additional script " + script + " was not found");
            }
            File file = new File(outputDir, "tests/" + simpleName);
            try (InputStream in = classLoader.getResourceAsStream(script)) {
                if (!file.exists()) {
                    file.createNewFile();
                }
                try(OutputStream out = new FileOutputStream(file)) {
                    IOUtils.copy(in, out);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error copying additional script " + script, e);
            }
        }
    }
}

