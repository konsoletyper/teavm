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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.tooling.ClassAlias;
import org.teavm.tooling.MethodAlias;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.SourceFileProvider;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "build-javascript", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildJavascriptMojo extends AbstractMojo {
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

    @Parameter(defaultValue = "${project.build.directory}/javascript")
    private File targetDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter
    private List<String> compileScopes;

    @Parameter
    private String targetFileName = "classes.js";

    @Parameter
    private boolean minifying = true;

    @Parameter
    private String mainClass;

    @Parameter
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;

    @Parameter
    private Properties properties;

    @Parameter
    private boolean mainPageIncluded;

    @Parameter
    private boolean bytecodeLogging;

    @Parameter
    private boolean debugInformationGenerated;

    @Parameter
    private boolean sourceMapsGenerated;

    @Parameter
    private boolean sourceFilesCopied;

    @Parameter
    private boolean incremental;

    @Parameter(defaultValue = "${project.build.directory}/teavm-cache")
    private File cacheDirectory;

    @Parameter
    private String[] transformers;

    @Parameter
    private ClassAlias[] classAliases;

    @Parameter
    private MethodAlias[] methodAliases;

    @Parameter
    private boolean stopOnErrors = true;

    private TeaVMTool tool = new TeaVMTool();

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public void setClassFiles(File classFiles) {
        this.classFiles = classFiles;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public void setBytecodeLogging(boolean bytecodeLogging) {
        this.bytecodeLogging = bytecodeLogging;
    }

    public void setRuntimeCopy(RuntimeCopyOperation runtimeCopy) {
        this.runtime = runtimeCopy;
    }

    public void setMainPageIncluded(boolean mainPageIncluded) {
        this.mainPageIncluded = mainPageIncluded;
    }

    public void setCompileScopes(List<String> compileScopes) {
        this.compileScopes = compileScopes;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
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

    public void setClassAliases(ClassAlias[] classAliases) {
        this.classAliases = classAliases;
    }

    public void setMethodAliases(MethodAlias[] methodAliases) {
        this.methodAliases = methodAliases;
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

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setStopOnErrors(boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }

    public File getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        tool.setLog(new MavenTeaVMToolLog(log));
        try {
            ClassLoader classLoader = prepareClassLoader();
            tool.setClassLoader(classLoader);
            tool.setBytecodeLogging(bytecodeLogging);
            tool.setMainClass(mainClass);
            tool.setMainPageIncluded(mainPageIncluded);
            tool.setMinifying(minifying);
            tool.setRuntime(runtime);
            tool.setTargetDirectory(targetDirectory);
            tool.setTargetFileName(targetFileName);
            tool.getTransformers().addAll(instantiateTransformers(classLoader));
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
            if (classAliases != null) {
                tool.getClassAliases().addAll(Arrays.asList(classAliases));
            }
            if (methodAliases != null) {
                tool.getMethodAliases().addAll(Arrays.asList(methodAliases));
            }
            if (properties != null) {
                tool.getProperties().putAll(properties);
            }
            tool.setCacheDirectory(cacheDirectory);
            tool.setIncremental(incremental);
            tool.setDebugInformationGenerated(debugInformationGenerated);
            tool.setSourceMapsFileGenerated(sourceMapsGenerated);
            tool.setSourceFilesCopied(sourceFilesCopied);
            tool.generate();
            if (stopOnErrors && !tool.getProblemProvider().getSevereProblems().isEmpty()) {
                throw new MojoExecutionException("Build error");
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unexpected error occured", e);
        } catch (TeaVMToolException e) {
            throw new MojoExecutionException("IO error occured", e);
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

    private ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Preparing classpath for JavaScript generation");
            List<URL> urls = new ArrayList<>();
            StringBuilder classpath = new StringBuilder();
            Set<String> scopes;
            if (compileScopes == null) {
                scopes = new HashSet<>(Arrays.asList(
                    Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM));
            } else {
                scopes = new HashSet<>(compileScopes);
            }
            for (Artifact artifact : project.getArtifacts()) {
                if (!scopes.contains(artifact.getScope())) {
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
            classpath.append(classFiles.getPath());
            urls.add(classFiles.toURI().toURL());
            log.info("Using the following classpath for JavaScript generation: " + classpath);
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), BuildJavascriptMojo.class.getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
        }
    }
}
