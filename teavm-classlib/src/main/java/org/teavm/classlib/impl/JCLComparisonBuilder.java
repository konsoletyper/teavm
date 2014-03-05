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
package org.teavm.classlib.impl;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.objectweb.asm.ClassReader;
import org.teavm.parsing.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev
 */
public class JCLComparisonBuilder {
    private static final String JAR_PREFIX = "jar:file:";
    private static final String JAR_SUFFIX = "!/java/lang/Object.class";
    private static final String CLASS_SUFFIX = ".class";
    private Set<String> packages = new HashSet<>();
    private ClassLoader classLoader = JCLComparisonBuilder.class.getClassLoader();
    private JCLComparisonVisitor visitor;
    private String outputFile;

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Set<String> getPackages() {
        return packages;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: package.name [package.name2 ...]");
            System.exit(1);
        }
        JCLComparisonBuilder builder = new JCLComparisonBuilder();

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-output")) {
                builder.setOutputFile(args[++i]);
            } else {
                builder.getPackages().add(args[i].replace('.', '/'));
            }
        }

        builder.buildComparisonReport();
    }

    public void buildComparisonReport() throws IOException {
        URL url = classLoader.getResource("java/lang/Object" + CLASS_SUFFIX);
        String path = url.toString();
        if (!path.startsWith(JAR_PREFIX) || !path.endsWith(JAR_SUFFIX)) {
            throw new RuntimeException("Can't find JCL classes");
        }
        ClasspathClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
        path = path.substring(JAR_PREFIX.length(), path.length() - JAR_SUFFIX.length());
        File outDir = new File(outputFile).getParentFile();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        try (JarInputStream jar = new JarInputStream(new FileInputStream(path));
             PrintStream out = new PrintStream(new FileOutputStream(outputFile))) {
            out.println("{");
            visitor = new JCLComparisonVisitor(classSource, out);
            while (true) {
                JarEntry entry = jar.getNextJarEntry();
                if (entry == null) {
                    break;
                }
                if (validateName(entry.getName())) {
                    compareClass(jar);
                }
                jar.closeEntry();
            }
            out.println();
            out.println("}");
        }
    }

    private boolean validateName(String name) {
        if (!name.endsWith(CLASS_SUFFIX)) {
            return false;
        }
        int slashIndex = name.lastIndexOf('/');
        return packages.contains(name.substring(0, slashIndex));
    }

    private void compareClass(InputStream input) throws IOException {
        ClassReader reader = new ClassReader(input);
        reader.accept(visitor, 0);
    }
}
