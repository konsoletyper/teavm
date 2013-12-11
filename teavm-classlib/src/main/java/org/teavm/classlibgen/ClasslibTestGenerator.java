package org.teavm.classlibgen;

import java.io.*;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.teavm.codegen.*;
import org.teavm.dependency.DependencyChecker;
import org.teavm.javascript.Decompiler;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.model.*;
import org.teavm.model.resource.ClasspathClassHolderSource;
import org.teavm.optimization.ClassSetOptimizer;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    private static File outputDir;
    private static ClasspathClassHolderSource classSource;
    private static Decompiler decompiler;
    private static AliasProvider aliasProvider;
    private static DefaultNamingStrategy naming;
    private static SourceWriter writer;
    private static Renderer renderer;
    private static List<MethodReference> testMethods = new ArrayList<>();
    private static Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private static String[] testClasses = { "java.lang.ObjectTests", "java.lang.SystemTests",
            "java.lang.StringBuilderTests", "java.lang.ClassTests", "java.lang.StringTests",
            "java.lang.VMTests" };

    public static void main(String[] args) throws IOException {
        outputDir = new File(args[0]);
        outputDir.mkdirs();
        resourceToFile("org/teavm/javascript/runtime.js", "runtime.js");
        resourceToFile("org/teavm/classlib/junit-support.js", "junit-support.js");
        resourceToFile("org/teavm/classlib/junit.css", "junit.css");
        classSource = new ClasspathClassHolderSource();
        for (int i = 0; i < testClasses.length; ++i) {
            testClasses[i] = "org.teavm.classlib." + testClasses[i];
        }
        for (String testClass : testClasses) {
            ClassHolder classHolder = classSource.getClassHolder(testClass);
            findTests(classHolder);
        }
        writer.append("runTests = function() {").newLine().indent();
        writer.append("document.getElementById(\"start-button\").style.display = 'none';").newLine();
        for (String testClass : testClasses) {
            renderClassTest(classSource.getClassHolder(testClass));
        }
        writer.outdent().append("}").newLine();
    }

    private static void decompileClassesForTest(MethodReference methodRef, String targetName) throws IOException {
        decompiler = new Decompiler(classSource);
        aliasProvider = new MinifyingAliasProvider();
        naming = new DefaultNamingStrategy(aliasProvider, classSource);
        naming.setMinifying(true);
        SourceWriterBuilder builder = new SourceWriterBuilder(naming);
        builder.setMinified(true);
        writer = builder.build();
        renderer = new Renderer(writer, classSource);
        renderer.renderRuntime();
        DependencyChecker dependencyChecker = new DependencyChecker(classSource);
        MethodReference cons = new MethodReference(methodRef.getClassName(),
                new MethodDescriptor("<init>", ValueType.VOID));
        dependencyChecker.addEntryPoint(cons);
        dependencyChecker.addEntryPoint(methodRef);
        dependencyChecker.checkDependencies();
        ListableClassHolderSource classSet = dependencyChecker.cutUnachievableClasses();
        ClassSetOptimizer optimizer = new ClassSetOptimizer();
        optimizer.optimizeAll(classSet);
        renderer.renderRuntime();
        decompileClasses(classSet.getClassNames());
        try (Writer out = new OutputStreamWriter(new FileOutputStream(new File(outputDir, targetName)), "UTF-8")) {
            out.write(writer.toString());
        }
    }

    private static void decompileClasses(Collection<String> classNames) {
        List<ClassNode> clsNodes = decompiler.decompile(classNames);
        for (ClassNode clsNode : clsNodes) {
            renderer.render(clsNode);
        }
    }

    private static void renderClassTest(ClassHolder cls) {
        List<MethodReference> methods = groupedMethods.get(cls.getName());
        writer.append("testClass(\"" + cls.getName() + "\", function() {").newLine().indent();
        MethodReference cons = new MethodReference(cls.getName(), new MethodDescriptor("<init>", ValueType.VOID));
        for (MethodReference method : methods) {
            writer.append("runTestCase(").appendClass(cls.getName()).append(".").appendMethod(cons)
                    .append("(), \"" + method.getDescriptor().getName() + "\", \"").appendMethod(method)
                    .append("\", [");
            MethodHolder methodHolder = classSource.getClassHolder(method.getClassName()).getMethod(
                    method.getDescriptor());
            AnnotationHolder annot = methodHolder.getAnnotations().get("org.junit.Test");
            AnnotationValue expectedAnnot = annot.getValues().get("expected");
            if (expectedAnnot != null) {
                String className = ((ValueType.Object)expectedAnnot.getJavaClass()).getClassName();
                writer.appendClass(className);
            }
            writer.append("]);").newLine();
        }
        writer.outdent().append("})").newLine();
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
