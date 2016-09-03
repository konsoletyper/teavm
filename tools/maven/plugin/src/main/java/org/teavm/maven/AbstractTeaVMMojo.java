/*
 *  Copyright 2015 Alexey Andreev.
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.tooling.BaseTeaVMTool;
import org.teavm.tooling.sources.SourceFileProvider;

public abstract class AbstractTeaVMMojo extends AbstractMojo {
    @Component
    protected MavenProject project;

    @Component
    protected RepositorySystem repositorySystem;

    @Parameter(required = true, readonly = true, defaultValue = "${localRepository}")
    protected MavenArtifactRepository localRepository;

    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    protected List<MavenArtifactRepository> remoteRepositories;

    @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
    protected List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classFiles;

    @Parameter
    protected List<String> compileScopes;

    @Parameter
    protected boolean minifying = true;

    @Parameter
    protected String mainClass;

    @Parameter
    protected Properties properties;

    @Parameter
    protected boolean debugInformationGenerated;

    @Parameter
    protected boolean sourceMapsGenerated;

    @Parameter
    protected boolean sourceFilesCopied;

    @Parameter
    protected boolean incremental;

    @Parameter
    protected String[] transformers;

    protected ClassLoader classLoader;

    protected abstract File getTargetDirectory();

    protected final List<ClassHolderTransformer> instantiateTransformers(ClassLoader classLoader)
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
                throw new MojoExecutionException("Transformer " + transformerName + " is not subtype of "
                        + ClassHolderTransformer.class.getName());
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

    protected void setupTool(BaseTeaVMTool tool) throws MojoExecutionException {
        tool.setLog(new MavenTeaVMToolLog(getLog()));
        try {
            ClassLoader classLoader = prepareClassLoader();
            tool.setClassLoader(classLoader);
            tool.setMinifying(minifying);
            tool.setTargetDirectory(getTargetDirectory());
            tool.getTransformers().addAll(instantiateTransformers(classLoader));
            if (sourceFilesCopied) {
                for (SourceFileProvider provider : getSourceFileProviders()) {
                    tool.addSourceFileProvider(provider);
                }
            }
            if (properties != null) {
                tool.getProperties().putAll(properties);
            }
            tool.setIncremental(incremental);
            tool.setDebugInformationGenerated(debugInformationGenerated);
            tool.setSourceMapsFileGenerated(sourceMapsGenerated);
            tool.setSourceFilesCopied(sourceFilesCopied);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unexpected error occured", e);
        }
    }

    protected final ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Preparing classpath for JavaScript generation");
            List<URL> urls = new ArrayList<>();
            StringBuilder classpath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                if (!filterByScope(artifact)) {
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
            for (File additionalEntry : getAdditionalClassPath()) {
                classpath.append(':').append(additionalEntry.getPath());
                urls.add(additionalEntry.toURI().toURL());
            }
            log.info("Using the following classpath for JavaScript generation: " + classpath);
            classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                    AbstractTeaVMMojo.class.getClassLoader());
            return classLoader;
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
        }
    }

    protected List<File> getAdditionalClassPath() {
        return Collections.emptyList();
    }

    protected boolean filterByScope(Artifact artifact) {
        return compileScopes == null ? isSupportedScope(artifact.getScope())
                : compileScopes.contains(artifact.getScope());
    }

    protected boolean isSupportedScope(String scope) {
        switch (scope) {
            case Artifact.SCOPE_COMPILE:
            case Artifact.SCOPE_PROVIDED:
            case Artifact.SCOPE_SYSTEM:
                return true;
            default:
                return false;
        }
    }

    protected final List<SourceFileProvider> getSourceFileProviders() {
        MavenSourceFileProviderLookup lookup = new MavenSourceFileProviderLookup();
        lookup.setMavenProject(project);
        lookup.setRepositorySystem(repositorySystem);
        lookup.setLocalRepository(localRepository);
        lookup.setRemoteRepositories(remoteRepositories);
        lookup.setPluginDependencies(pluginArtifacts);
        List<SourceFileProvider> providers = lookup.resolve();
        addSourceProviders(providers);
        return providers;
    }

    protected void addSourceProviders(@SuppressWarnings("unused") List<SourceFileProvider> providers) {
    }
}
