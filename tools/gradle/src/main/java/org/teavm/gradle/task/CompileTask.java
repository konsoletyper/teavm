/*
 *  Copyright 2016 MJ.
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

package org.teavm.gradle.task;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.teavm.gradle.TeaVMPlugin;
import org.teavm.gradle.extension.MethodAliasUtils;
import org.teavm.gradle.extension.TeaVMExtension;
import org.teavm.gradle.logging.LoggerWrapper;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.tooling.ClassAlias;
import org.teavm.tooling.MethodAlias;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;

/** Compiles TeaVM application. Expects the project to has the {@code mainClassName} property. Customizable with
 * {@link TeaVMExtension}.
 *
 * @author MJ */
public class CompileTask extends DefaultTask {
    @TaskAction
    public void compile() {
        try {
            compileTeaVM();
        } catch (final Exception exception) {
            getProject().getLogger().error("Unable to compile TeaVM application.", exception);
            throw new GradleException("Unable to compile TeaVM application.", exception);
        }
    }

    private void compileTeaVM() throws TeaVMToolException {
        final TeaVMTool tool = new TeaVMTool();
        tool.setLog(new LoggerWrapper(getProject().getLogger()));
        copyProperties(tool);
        gatherSources(tool);
        final URLClassLoader loader = prepareClassLoader();
        tool.setClassLoader(loader);
        try {
            tool.generate();
        } finally {
            try {
                loader.close();
            } catch (final IOException exception) {
                getProject().getLogger().error("Unable to close class loader.", exception);
            }
        }
    }

    private void copyProperties(final TeaVMTool tool) throws TeaVMToolException {
        final TeaVMExtension extension = getProject().getExtensions().getByType(TeaVMExtension.class);
        tool.setTargetDirectory(toFile(extension.getTargetDirectory()));
        tool.setTargetFileName(extension.getTargetFileName());
        tool.setMinifying(extension.isMinifying());
        tool.setDebugInformationGenerated(extension.isDebugInformationGenerated());
        tool.setSourceMapsFileGenerated(extension.isSourceMapsGenerated());
        tool.setSourceFilesCopied(extension.isSourceFilesCopied());
        tool.setIncremental(extension.isIncremental());
        tool.setCacheDirectory(toFile(extension.getCacheDirectory()));
        tool.setRuntime(extension.getRuntime());
        tool.setMainPageIncluded(extension.isMainPageIncluded());
        tool.setMainClass(findMainClass());
        fillProperties(tool, extension);
        fillClassAliases(tool, extension);
        fillMethodAliases(tool, extension);
        fillTransformers(tool, extension);
    }

    private File toFile(final String fileName) throws TeaVMToolException {
        final File file = new File(getProject().getBuildDir(), fileName);
        if (!file.exists() && !file.mkdirs()) {
            throw new TeaVMToolException("Cannot create directory: " + fileName);
        }
        return file;
    }

    private String findMainClass() throws TeaVMToolException {
        if (getProject().hasProperty("mainClassName")) {
            final Object mainClass = getProject().property("mainClassName");
            if (mainClass != null) {
                return mainClass.toString();
            }
        }
        throw new TeaVMToolException("Unable to determine main class.");
    }

    private static void fillProperties(final TeaVMTool tool, final TeaVMExtension extension) {
        if (extension.getProperties() != null) {
            tool.getProperties().putAll(extension.getProperties());
        }
    }

    private static void fillClassAliases(final TeaVMTool tool, final TeaVMExtension extension) {
        if (extension.getClassAliases() != null) {
            final List<ClassAlias> aliases = tool.getClassAliases();
            for (final Entry<String, String> aliasData : extension.getClassAliases().entrySet()) {
                final ClassAlias alias = new ClassAlias();
                alias.setClassName(aliasData.getKey());
                alias.setAlias(aliasData.getValue());
                aliases.add(alias);
            }
        }
    }

    private static void fillMethodAliases(final TeaVMTool tool, final TeaVMExtension extension)
            throws TeaVMToolException {
        final List<MethodAlias> aliases = tool.getMethodAliases();
        if (extension.getMethodAliases() != null) {
            for (final Map<String, Object> aliasData : extension.getMethodAliases()) {
                try {
                    aliases.add(MethodAliasUtils.convert(aliasData));
                } catch (final Exception exception) {
                    throw new TeaVMToolException("Unable to construct method alias from: " + aliasData, exception);
                }
            }
        }
    }

    private static void fillTransformers(final TeaVMTool tool, final TeaVMExtension extension)
            throws TeaVMToolException {
        if (extension.getTransformers() != null) {
            final List<ClassHolderTransformer> transformers = tool.getTransformers();
            for (final String transformerClass : extension.getTransformers()) {
                try {
                    transformers.add((ClassHolderTransformer) Class.forName(transformerClass).newInstance());
                } catch (final Exception exception) {
                    throw new TeaVMToolException("Unable to obtain transformer class: " + transformerClass, exception);
                }
            }
        }
    }

    private void gatherSources(final TeaVMTool tool) {
        final JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        final Set<File> sources = new HashSet<File>();
        for (final File file : javaPlugin.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllSource()) {
            sources.add(file);
        }
        for (final File file : getProject().getConfigurations().getByName(TeaVMPlugin.TEAVM_CONFIGURATION)) {
            sources.add(file);
        }
        for (final File file : sources) {
            addSourceProvider(tool, file);
        }
    }

    private static void addSourceProvider(final TeaVMTool tool, final File file) {
        if (file.isFile() && file.getName().endsWith(".jar")) {
            tool.addSourceFileProvider(new JarSourceFileProvider(file));
        } else {
            tool.addSourceFileProvider(new DirectorySourceFileProvider(file));
        }
    }

    private URLClassLoader prepareClassLoader() throws TeaVMToolException {
        final Set<URL> urls = new HashSet<URL>();
        // Gathering resources:
        final JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
        final SourceSet mainSourceSet = javaPlugin.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        for (final File file : mainSourceSet.getResources()) {
            urls.add(toUrl(file));
        }
        // Gathering binaries:
        for (final File file : mainSourceSet.getOutput().getFiles()) {
            getProject().getLogger().error("Source dir:" + file);
            urls.add(toUrl(file));
        }
        // Gathering runtime dependencies:
        for (final File file : getProject().getConfigurations().getByName("runtime")) {
            urls.add(toUrl(file));
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
    }

    private static URL toUrl(final File file) throws TeaVMToolException {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException exception) {
            throw new TeaVMToolException("Invalid URL in classpath.", exception);
        }
    }
}
