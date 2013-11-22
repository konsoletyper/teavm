package org.teavm.classlibgen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.teavm.codegen.DefaultAliasProvider;
import org.teavm.codegen.DefaultNamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.javascript.Decompiler;
import org.teavm.javascript.Renderer;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.model.*;
import org.teavm.model.resource.ClasspathClassHolderSource;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClasslibTestGenerator {
    private static PrintStream out;
    private static ClasspathClassHolderSource classSource;
    private static Decompiler decompiler;
    private static DefaultAliasProvider aliasProvider;
    private static DefaultNamingStrategy naming;
    private static SourceWriter writer;
    private static Renderer renderer;
    private static List<MethodReference> testMethods = new ArrayList<>();
    private static Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private static String[] testClasses = { "java.lang.ObjectTests", "java.lang.SystemTests" };

    public static void main(String[] args) throws IOException {
        out = System.out;
        if (args.length > 0) {
            out = new PrintStream(new FileOutputStream(args[0]));
        }
        classSource = new ClasspathClassHolderSource();
        decompiler = new Decompiler(classSource);
        aliasProvider = new DefaultAliasProvider();
        naming = new DefaultNamingStrategy(aliasProvider, classSource);
        writer = new SourceWriter(naming);
        renderer = new Renderer(writer, classSource);
        DependencyChecker dependencyChecker = new DependencyChecker(classSource);
        for (String testClass : testClasses) {
            ClassHolder classHolder = classSource.getClassHolder(testClass);
            findTests(classHolder);
            MethodReference cons = new MethodReference(testClass, new MethodDescriptor("<init>", ValueType.VOID));
            dependencyChecker.addEntryPoint(cons);
        }
        for (MethodReference methodRef : testMethods) {
            dependencyChecker.addEntryPoint(methodRef);
        }
        dependencyChecker.checkDependencies();
        dependencyChecker.cutUnachievableClasses();
        decompileClasses(dependencyChecker.getAchievableClasses());
        renderHead();
        ClassLoader classLoader = ClasslibTestGenerator.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream("org/teavm/classlib/junit-support.js")) {
            out.println(IOUtils.toString(input));
        }
        try (InputStream input = classLoader.getResourceAsStream("org/teavm/javascript/runtime.js")) {
            out.println(IOUtils.toString(input));
        }
        renderer.renderRuntime();
        for (String testClass : testClasses) {
            renderClassTest(classSource.getClassHolder(testClass));
        }
        out.println(writer);
        renderFoot();
    }

    private static void decompileClasses(Collection<String> classNames) {
        List<ClassNode> clsNodes = decompiler.decompile(classNames);
        for (ClassNode clsNode : clsNodes) {
            renderer.render(clsNode);
        }
    }

    private static void renderHead() {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("  <head>");
        out.println("    <title>TeaVM JUnit tests</title>");
        out.println("    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\"/>");
        out.println("    <title>TeaVM JUnit tests</title>");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <script type=\"text/javascript\">");
    }

    private static void renderFoot() {
        out.println("    </script>");
        out.println("  </body>");
        out.println("</html>");
    }

    private static void renderClassTest(ClassHolder cls) {
        List<MethodReference> methods = groupedMethods.get(cls.getName());
        writer.append("testClass(\"" + cls.getName() + "\", function() {").newLine().indent();
        MethodReference cons = new MethodReference(cls.getName(), new MethodDescriptor("<init>", ValueType.VOID));
        for (MethodReference method : methods) {
            writer.append("runTestCase(").appendClass(cls.getName()).append(".").appendMethod(cons)
                    .append("(), \"" + method.getDescriptor().getName() + "\", \"").appendMethod(method)
                    .append("\");").newLine();
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
}
