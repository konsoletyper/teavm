package org.teavm.classlibgen;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.teavm.javascript.JavascriptBuilder;
import org.teavm.model.*;
import org.teavm.model.resource.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    private static File outputDir;
    private static ClasspathClassHolderSource classSource;
    private static List<MethodReference> testMethods = new ArrayList<>();
    private static Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private static Map<MethodReference, String> fileNames = new HashMap<>();
    private static String[] testClasses = { "java.lang.ObjectTests", "java.lang.SystemTests",
            "java.lang.StringBuilderTests", "java.lang.ClassTests", "java.lang.StringTests",
            "java.lang.VMTests" };

    public static void main(String[] args) throws IOException {
        outputDir = new File(args[0]);
        outputDir.mkdirs();
        new File(outputDir, "tests").mkdirs();
        resourceToFile("org/teavm/javascript/runtime.js", "runtime.js");
        resourceToFile("org/teavm/classlib/junit-support.js", "junit-support.js");
        resourceToFile("org/teavm/classlib/junit.css", "junit.css");
        resourceToFile("org/teavm/classlib/junit.html", "junit.html");
        classSource = new ClasspathClassHolderSource();
        for (int i = 0; i < testClasses.length; ++i) {
            testClasses[i] = "org.teavm.classlib." + testClasses[i];
        }
        for (String testClass : testClasses) {
            ClassHolder classHolder = classSource.getClassHolder(testClass);
            findTests(classHolder);
        }

        File allTestsFile = new File(outputDir, "tests/all.js");
        try (Writer allTestsWriter = new OutputStreamWriter(new FileOutputStream(allTestsFile), "UTF-8")) {
            allTestsWriter.write("doRunTests = function() {\n");
            allTestsWriter.write("    new JUnitServer(document.body).runAllTests([");
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
                    MethodHolder methodHolder = classSource.getClassHolder(testClass).getMethod(
                            methodRef.getDescriptor());
                    AnnotationHolder annot = methodHolder.getAnnotations().get("org.junit.Test");
                    AnnotationValue expectedAnnot = annot.getValues().get("expected");
                    if (expectedAnnot != null) {
                        String className = ((ValueType.Object)expectedAnnot.getJavaClass()).getClassName();
                        allTestsWriter.append("\"" + className + "\"");
                    }
                    allTestsWriter.append("] }");
                }
                allTestsWriter.append("] }");
            }
            allTestsWriter.write("], function() {}); }");
        }
        for (MethodReference method : testMethods) {
            System.out.println("Building test for " + method);
            decompileClassesForTest(method, fileNames.get(method));
        }
    }

    private static void decompileClassesForTest(MethodReference methodRef, String targetName) throws IOException {
        JavascriptBuilder builder = new JavascriptBuilder();
        builder.setMinifying(true);
        @SuppressWarnings("resource")
        Writer innerWriter = new OutputStreamWriter(new FileOutputStream(new File(outputDir, targetName)), "UTF-8");
        MethodReference cons = new MethodReference(methodRef.getClassName(),
                new MethodDescriptor("<init>", ValueType.VOID));
        builder.entryPoint("initInstance", cons);
        builder.entryPoint("runTest", methodRef).withValue(0, cons.getClassName());
        builder.exportType("TestClass", cons.getClassName());
        builder.build(innerWriter);
        innerWriter.append("\n");
        innerWriter.append("\nJUnitClient.run();");
        innerWriter.close();
    }

    private static void findTests(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.getAnnotations().get("org.junit.Test") != null) {
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

    private static void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = ClasslibTestGenerator.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }
}
