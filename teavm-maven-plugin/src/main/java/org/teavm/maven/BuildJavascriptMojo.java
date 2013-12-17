package org.teavm.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.teavm.javascript.JavascriptBuilder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
@Mojo(name = "build-javascript")
public class BuildJavascriptMojo extends AbstractMojo {
    private static Set<String> compileScopes = new HashSet<>(Arrays.asList(
            Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM));

    @Component
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/javascript/classes.js")
    private File targetFile;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classFiles;

    @Parameter
    private boolean minifiying = true;

    @Parameter
    private String mainClass;

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    public void setClassFiles(File classFiles) {
        this.classFiles = classFiles;
    }

    public void setMinifiying(boolean minifiying) {
        this.minifiying = minifiying;
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        try {
            ClassLoader classLoader = prepareClassLoader();
            log.info("Building JavaScript file");
            JavascriptBuilder builder = new JavascriptBuilder(classLoader);
            builder.setMinifying(minifiying);
            MethodDescriptor mainMethodDesc = new MethodDescriptor("main", ValueType.arrayOf(
                    ValueType.object("java.lang.String")), ValueType.VOID);
            builder.entryPoint("main", new MethodReference(mainClass, mainMethodDesc))
                    .withValue(1, "java.lang.String");
            builder.build(targetFile);
            log.info("JavaScript file successfully built");
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Unexpected error occured", e);
        }
    }

    private ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Preparing classpath for JavaScript generation");
            List<URL> urls = new ArrayList<>();
            StringBuilder classpath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                if (!compileScopes.contains(artifact.getScope())) {
                    continue;
                }
                File file = artifact.getFile();
                if (classpath.length() > 0) {
                    classpath.append(':');
                }
                classpath.append(file.getPath());
                urls.add(file.toURI().toURL());
            }
            log.info("Using the following classpath for JavaScript generation: " + classpath);
            urls.add(classFiles.toURI().toURL());
            return new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error gathering classpath information", e);
        }
    }
}
