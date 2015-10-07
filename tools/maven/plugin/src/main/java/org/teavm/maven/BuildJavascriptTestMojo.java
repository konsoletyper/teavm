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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.tooling.SourceFileProvider;
import org.teavm.tooling.TeaVMTestCase;
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

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(required = true, readonly = true, defaultValue = "${localRepository}")
    private MavenArtifactRepository localRepository;

    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    private List<MavenArtifactRepository> remoteRepositories;

    @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
    private List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "${project.build.directory}/javascript-test")
    private File outputDir;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter(defaultValue = "${project.build.testOutputDirectory}")
    private File testFiles;

    @Parameter
    private String[] wildcards = { "**.*Test", "**.*UnitTest" };

    @Parameter
    private String[] excludeWildcards = new String[0];

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

    @Parameter
    private boolean sourceFilesCopied;

    @Parameter
    private URL seleniumURL;
    private WebDriver webDriver;

    private TeaVMTestTool tool = new TeaVMTestTool();
    private BlockingQueue<Runnable> seleniumTaskQueue = new LinkedBlockingQueue<>();
    private volatile boolean seleniumStopped = false;

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

    public void setExcludeWildcards(String[] excludeWildcards) {
        this.excludeWildcards = excludeWildcards;
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

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public void setSeleniumURL(URL seleniumURL) {
        this.seleniumURL = seleniumURL;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (System.getProperty("maven.test.skip", "false").equals("true") ||
                System.getProperty("skipTests") != null) {
            getLog().info("Tests build skipped as specified by system property");
            return;
        }

        detectSelenium();
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
            tool.setSourceFilesCopied(sourceFilesCopied);
            if (sourceFilesCopied) {
                MavenSourceFileProviderLookup lookup = new MavenSourceFileProviderLookup();
                lookup.setMavenProject(project);
                lookup.setRepositorySystem(repositorySystem);
                lookup.setLocalRepository(localRepository);
                lookup.setRemoteRepositories(remoteRepositories);
                lookup.setPluginDependencies(pluginArtifacts);
                for (SourceFileProvider provider : lookup.resolve()) {
                    tool.addSourceFileProvider(provider);
                }
            }
            if (properties != null) {
                tool.getProperties().putAll(properties);
            }
            if (additionalScripts != null) {
                tool.getAdditionalScripts().addAll(Arrays.asList(additionalScripts));
            }
            tool.addListener(testCase -> runSelenium(testCase));
            tool.generate();
        } catch (TeaVMToolException e) {
            throw new MojoFailureException("Error occured generating JavaScript files", e);
        } finally {
            webDriver.close();
            stopSelenium();
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

    private void detectSelenium() {
        if (seleniumURL == null) {
            return;
        }
        ChromeDriver driver = new ChromeDriver();
        webDriver = driver;
        new Thread(() -> {
            while (!seleniumStopped) {
                Runnable task;
                try {
                    task = seleniumTaskQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    break;
                }
                task.run();
            }
        }).start();
    }

    private void addSeleniumTask(Runnable runnable) {
        if (seleniumURL != null) {
            seleniumTaskQueue.add(runnable);
        }
    }

    private void stopSelenium() {
        addSeleniumTask(() -> seleniumStopped = true);
    }

    private void runSelenium(TeaVMTestCase testCase) {
        if (webDriver == null) {
            return;
        }
        try {
            JavascriptExecutor js = (JavascriptExecutor) webDriver;
            js.executeAsyncScript(
                    readResource("teavm-selenium.js"),
                    readFile(testCase.getRuntimeScript()),
                    readFile(testCase.getTestScript()),
                    readResource("teavm-selenium-adapter.js"));
        } catch (IOException e) {
            getLog().error(e);
        }
    }

    private String readFile(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return IOUtils.toString(input, "UTF-8");
        }
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream input = BuildJavascriptTestMojo.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                return "";
            }
            return IOUtils.toString(input, "UTF-8");
        }
    }
}
