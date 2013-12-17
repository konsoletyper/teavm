package org.teavm.javascript;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.codegen.*;
import org.teavm.dependency.DependencyChecker;
import org.teavm.javascript.ast.ClassNode;
import org.teavm.model.*;
import org.teavm.model.resource.ClasspathClassHolderSource;
import org.teavm.optimization.ClassSetOptimizer;

/**
 *
 * @author Alexey Andreev
 */
public class JavascriptBuilder {
    private ClassHolderSource classSource;
    private DependencyChecker dependencyChecker;
    private ClassLoader classLoader;
    private boolean minifying = true;
    private Map<String, JavascriptEntryPoint> entryPoints = new HashMap<>();
    private Map<String, String> exportedClasses = new HashMap<>();

    public JavascriptBuilder(ClassHolderSource classSource, ClassLoader classLoader) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        dependencyChecker = new DependencyChecker(classSource, classLoader);
    }

    public JavascriptBuilder(ClassLoader classLoader) {
        this(new ClasspathClassHolderSource(classLoader), classLoader);
    }

    public JavascriptBuilder() {
        this(JavascriptBuilder.class.getClassLoader());
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public JavascriptEntryPoint entryPoint(String name, MethodReference ref) {
        if (entryPoints.containsKey(name)) {
            throw new IllegalArgumentException("Entry point with public name `" + name + "' already defined " +
                    "for method " + ref);
        }
        JavascriptEntryPoint entryPoint = new JavascriptEntryPoint(name, ref,
                dependencyChecker.attachMethodGraph(ref));
        entryPoints.put(name, entryPoint);
        return entryPoint;
    }

    public void exportType(String name, String className) {
        if (exportedClasses.containsKey(name)) {
            throw new IllegalArgumentException("Class with public name `" + name + "' already defined for class " +
                    className);
        }
        exportedClasses.put(name, className);
    }

    public ClassHolderSource getClassSource() {
        return classSource;
    }

    public void build(Appendable writer) throws RenderingException {
        Decompiler decompiler = new Decompiler(classSource, classLoader);
        AliasProvider aliasProvider = minifying ? new MinifyingAliasProvider() : new DefaultAliasProvider();
        DefaultNamingStrategy naming = new DefaultNamingStrategy(aliasProvider, classSource);
        naming.setMinifying(minifying);
        SourceWriterBuilder builder = new SourceWriterBuilder(naming);
        builder.setMinified(minifying);
        SourceWriter sourceWriter = builder.build(writer);
        Renderer renderer = new Renderer(sourceWriter, classSource);
        renderer.renderRuntime();
        dependencyChecker.attachMethodGraph(new MethodReference("java.lang.Class", new MethodDescriptor("createNew",
                ValueType.object("java.lang.Class"))));
        dependencyChecker.attachMethodGraph(new MethodReference("java.lang.String", new MethodDescriptor("<init>",
                ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID)));
        dependencyChecker.checkDependencies();
        ListableClassHolderSource classSet = dependencyChecker.cutUnachievableClasses();
        ClassSetOptimizer optimizer = new ClassSetOptimizer();
        optimizer.optimizeAll(classSet);
        renderer.renderRuntime();
        List<ClassNode> clsNodes = decompiler.decompile(classSet.getClassNames());
        for (ClassNode clsNode : clsNodes) {
            renderer.render(clsNode);
        }
        try {
            for (Map.Entry<String, JavascriptEntryPoint> entry : entryPoints.entrySet()) {
                sourceWriter.append(entry.getKey()).ws().append("=").ws().appendMethodBody(entry.getValue().reference)
                        .append(";").softNewLine();
            }
            for (Map.Entry<String, String> entry : exportedClasses.entrySet()) {
                sourceWriter.append(entry.getKey()).ws().append("=").ws().appendClass(entry.getValue()).append(";")
                        .softNewLine();
            }
        } catch (IOException e) {
            throw new RenderingException("IO Error occured", e);
        }
    }

    public void build(File file) throws RenderingException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            build(writer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Platform does not support UTF-8", e);
        } catch (IOException e) {
            throw new RenderingException();
        }
    }
}
