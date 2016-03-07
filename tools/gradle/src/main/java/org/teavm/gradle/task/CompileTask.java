package org.teavm.gradle.task;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.teavm.gradle.TeaVMPlugin;
import org.teavm.gradle.extension.TeaVMExtension;
import org.teavm.gradle.logging.LoggerWrapper;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;

public class CompileTask extends DefaultTask {
    @TaskAction
    public void compile() throws TeaVMToolException {
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
        tool.setCacheDirectory(toFile(extension.getCacheDirectory()));
        tool.setTargetFileName(extension.getTargetFile());
        tool.setRuntime(extension.getRuntime());
        tool.setMainPageIncluded(extension.isMainPageIncluded());
        tool.setMinifying(extension.isMinify());
        tool.setDebugInformationGenerated(extension.isDebugInfo());
        tool.setSourceFilesCopied(extension.isSourceFilesCopied());
        tool.setSourceMapsFileGenerated(extension.isSourceMaps());
        tool.setMainClass(findMainClass());
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
