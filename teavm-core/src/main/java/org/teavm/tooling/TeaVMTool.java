/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.tooling;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.teavm.common.ThreadPoolFiniteExecutor;
import org.teavm.javascript.RenderingContext;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.*;
import org.teavm.vm.spi.AbstractRendererListener;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMTool {
    private File targetDirectory = new File(".");
    private String targetFileName = "classes.js";
    private boolean minifying = true;
    private String mainClass;
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;
    private Properties properties = new Properties();
    private boolean mainPageIncluded;
    private boolean bytecodeLogging;
    private int numThreads = 1;
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private List<ClassAlias> classAliases = new ArrayList<>();
    private List<MethodAlias> methodAliases = new ArrayList<>();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private ClassLoader classLoader = TeaVMTool.class.getClassLoader();

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public RuntimeCopyOperation getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeCopyOperation runtime) {
        this.runtime = runtime;
    }

    public boolean isMainPageIncluded() {
        return mainPageIncluded;
    }

    public void setMainPageIncluded(boolean mainPageIncluded) {
        this.mainPageIncluded = mainPageIncluded;
    }

    public boolean isBytecodeLogging() {
        return bytecodeLogging;
    }

    public void setBytecodeLogging(boolean bytecodeLogging) {
        this.bytecodeLogging = bytecodeLogging;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public Properties getProperties() {
        return properties;
    }

    public List<ClassHolderTransformer> getTransformers() {
        return transformers;
    }

    public List<ClassAlias> getClassAliases() {
        return classAliases;
    }

    public List<MethodAlias> getMethodAliases() {
        return methodAliases;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void generate() throws TeaVMToolException {
        Runnable finalizer = null;
        try {
            log.info("Building JavaScript file");
            TeaVMBuilder vmBuilder = new TeaVMBuilder();
            vmBuilder.setClassLoader(classLoader).setClassSource(new ClasspathClassHolderSource(classLoader));
            if (numThreads != 1) {
                int threads = numThreads != 0 ? numThreads : Runtime.getRuntime().availableProcessors();
                final ThreadPoolFiniteExecutor executor = new ThreadPoolFiniteExecutor(threads);
                finalizer = new Runnable() {
                    @Override public void run() {
                        executor.stop();
                    }
                };
                vmBuilder.setExecutor(executor);
            }
            TeaVM vm = vmBuilder.build();
            vm.setMinifying(minifying);
            vm.setBytecodeLogging(bytecodeLogging);
            vm.setProperties(properties);
            vm.installPlugins();
            for (ClassHolderTransformer transformer : transformers) {
                vm.add(transformer);
            }
            if (mainClass != null) {
                MethodDescriptor mainMethodDesc = new MethodDescriptor("main", ValueType.arrayOf(
                        ValueType.object("java.lang.String")), ValueType.VOID);
                vm.entryPoint("main", new MethodReference(mainClass, mainMethodDesc))
                        .withValue(1, "java.lang.String");
            }
            for (ClassAlias alias : classAliases) {
                vm.exportType(alias.getAlias(), alias.getClassName());
            }
            for (MethodAlias methodAlias : methodAliases) {
                MethodReference ref = new MethodReference(methodAlias.getClassName(), methodAlias.getMethodName(),
                        MethodDescriptor.parseSignature(methodAlias.getDescriptor()));
                TeaVMEntryPoint entryPoint = vm.entryPoint(methodAlias.getAlias(), ref);
                if (methodAlias.getTypes() != null) {
                    for (int i = 0; i < methodAlias.getTypes().length; ++i) {
                        String types = methodAlias.getTypes()[i];
                        if (types != null) {
                            for (String type : types.split(" +")) {
                                type = type.trim();
                                if (!type.isEmpty()) {
                                    entryPoint.withValue(i, type);
                                }
                            }
                        }
                    }
                }
            }
            targetDirectory.mkdirs();
            try (FileWriter writer = new FileWriter(new File(targetDirectory, targetFileName))) {
                if (runtime == RuntimeCopyOperation.MERGED) {
                    vm.add(runtimeInjector);
                }
                vm.build(writer, new DirectoryBuildTarget(targetDirectory));
                vm.checkForMissingItems();
                log.info("JavaScript file successfully built");
            }
            if (runtime == RuntimeCopyOperation.SEPARATE) {
                resourceToFile("org/teavm/javascript/runtime.js", "runtime.js");
            }
            if (mainPageIncluded) {
                String text;
                try (Reader reader = new InputStreamReader(classLoader.getResourceAsStream(
                        "org/teavm/tooling/main.html"), "UTF-8")) {
                    text = IOUtils.toString(reader).replace("${classes.js}", targetFileName);
                }
                File mainPageFile = new File(targetDirectory, "main.html");
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(mainPageFile), "UTF-8")) {
                    writer.append(text);
                }
            }
        } catch (IOException e) {
            throw new TeaVMToolException("IO error occured", e);
        } finally {
            if (finalizer != null) {
                finalizer.run();
            }
        }
    }

    private AbstractRendererListener runtimeInjector = new AbstractRendererListener() {
        @Override
        public void begin(RenderingContext context, BuildTarget buildTarget) throws IOException {
            @SuppressWarnings("resource")
            StringWriter writer = new StringWriter();
            resourceToWriter("org/teavm/javascript/runtime.js", writer);
            writer.close();
            context.getWriter().append(writer.toString()).newLine();
        }
    };

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(targetDirectory, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }

    private void resourceToWriter(String resource, Writer writer) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            IOUtils.copy(input, writer, "UTF-8");
        }
    }
}
