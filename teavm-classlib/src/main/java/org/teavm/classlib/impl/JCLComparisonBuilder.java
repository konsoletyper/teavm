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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.commons.io.IOUtils;
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
    private static final String TEMPLATE_PLACEHOLDER = "${CONTENT}";
    private Set<String> packages = new HashSet<>();
    private ClassLoader classLoader = JCLComparisonBuilder.class.getClassLoader();
    private JCLComparisonVisitor visitor;
    private String outputDirectory;

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Set<String> getPackages() {
        return packages;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: package.name [package.name2 ...] [-output directory]");
            System.exit(1);
        }
        JCLComparisonBuilder builder = new JCLComparisonBuilder();

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-output")) {
                builder.setOutputDirectory(args[++i]);
            } else {
                builder.getPackages().add(args[i].replace('.', '/'));
            }
        }

        builder.buildComparisonReport();
    }

    public void buildComparisonReport() throws IOException {
        List<JCLPackage> packages = buildModel();
        processModel(packages);
        new File(outputDirectory).mkdirs();
        copyResource("html/class_obj.png");
        copyResource("html/field_protected_obj.png");
        copyResource("html/field_public_obj.png");
        copyResource("html/jcl.css");
        copyResource("html/methpro_obj.png");
        copyResource("html/methpub_obj.png");
        copyResource("html/package_obj.png");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(new File(
                outputDirectory, "jcl.html")), "UTF-8")) {
            generateHtml(out, packages);
        }
    }

    private List<JCLPackage> buildModel() throws IOException {
        Map<String, JCLPackage> packageMap = new HashMap<>();
        URL url = classLoader.getResource("java/lang/Object" + CLASS_SUFFIX);
        String path = url.toString();
        if (!path.startsWith(JAR_PREFIX) || !path.endsWith(JAR_SUFFIX)) {
            throw new RuntimeException("Can't find JCL classes");
        }
        ClasspathClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
        path = path.substring(JAR_PREFIX.length(), path.length() - JAR_SUFFIX.length());
        File outDir = new File(outputDirectory).getParentFile();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        try (JarInputStream jar = new JarInputStream(new FileInputStream(path))) {
            visitor = new JCLComparisonVisitor(classSource, packageMap);
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
        }
        return new ArrayList<>(packageMap.values());
    }

    private void processModel(List<JCLPackage> packages) {
        Collections.sort(packages, new Comparator<JCLPackage>() {
            @Override public int compare(JCLPackage o1, JCLPackage o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        for (JCLPackage pkg : packages) {
            Collections.sort(pkg.classes, new Comparator<JCLClass>() {
                @Override public int compare(JCLClass o1, JCLClass o2) {
                    return o1.name.compareTo(o2.name);
                }
            });
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
        byte[] buffer = IOUtils.toByteArray(input);
        ClassReader reader = new ClassReader(buffer);
        reader.accept(visitor, 0);
    }

    private void copyResource(String name) throws IOException {
        String simpleName = name.substring(name.lastIndexOf('/') + 1);
        try (InputStream input = classLoader.getResourceAsStream(name);
                OutputStream output = new FileOutputStream(new File(outputDirectory, simpleName))) {
            IOUtils.copy(input, output);
        }
    }

    private void generateHtml(Writer out, List<JCLPackage> packages) throws IOException {
        String template;
        try (Reader reader = new InputStreamReader(classLoader.getResourceAsStream("html/jcl.html"), "UTF-8")) {
            template = IOUtils.toString(reader);
        }
        int placeholderIndex = template.indexOf(TEMPLATE_PLACEHOLDER);
        String header = template.substring(0, placeholderIndex);
        String footer = template.substring(placeholderIndex + TEMPLATE_PLACEHOLDER.length());
        out.write(header);
        for (JCLPackage pkg : packages) {
            int totalClasses = pkg.classes.size();
            int fullClasses = 0;
            int partialClasses = 0;
            for (JCLClass cls : pkg.classes) {
                switch (cls.status) {
                    case FOUND:
                        fullClasses++;
                        partialClasses++;
                        break;
                    case PARTIAL:
                        partialClasses++;
                        break;
                    default:
                        break;
                }
            }
            writeRow(out, "package", pkg.status, pkg.name,
                    totalClasses > 0 ? fullClasses * 100 / totalClasses : null,
                    totalClasses > 0 ? partialClasses * 100 / totalClasses : null);
            for (JCLClass cls : pkg.classes) {
                int implemented = 0;
                for (JCLItem item : cls.items) {
                    if (item.status != JCLStatus.MISSING) {
                        ++implemented;
                    }
                }
                writeRow(out, "class", cls.status, cls.name,
                        !cls.items.isEmpty() ? implemented * 100 / cls.items.size() : null, null);
                for (JCLItem item : cls.items) {
                    String type;
                    switch (item.type) {
                        case FIELD:
                            type = "field";
                            break;
                        case METHOD:
                            type = "method";
                            break;
                        default:
                            type = "";
                            break;
                    }
                    writeRow(out, type, item.status, item.name, null, null);
                }
            }
        }
        out.write(footer);
    }

    private void writeRow(Writer out, String type, JCLStatus status, String name, Integer percent,
            Integer partialPercent) throws IOException {
        out.write("<tr class=\"");
        switch (status) {
            case FOUND:
                out.write("full");
                break;
            case MISSING:
                out.write("missing");
                break;
            case PARTIAL:
                out.write("partial");
                break;
        }
        out.write("\">\n");
        out.write("<td><div class=\"");
        out.write(type);
        out.write("\">");
        out.write(escape(name));
        out.write("</div></td>\n");
        out.write("<td class=\"percent\">" + (partialPercent != null ? partialPercent.toString() : "") + "</td>");
        out.write("<td class=\"percent\">" + (percent != null ? percent.toString() : "") + "</td>");
        out.write("</tr>\n");
    }

    private String escape(String string) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char ch = string.charAt(i);
            switch (ch) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
