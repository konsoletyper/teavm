/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntFunction;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.MethodNode;
import org.teavm.ast.MethodNodeVisitor;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.VariableNode;
import org.teavm.backend.javascript.codegen.NamingOrderer;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.ScopedName;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.decompile.PreparedClass;
import org.teavm.backend.javascript.decompile.PreparedMethod;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.vm.RenderingException;
import org.teavm.vm.TeaVMProgressFeedback;

public class Renderer implements RenderingManager {
    private final NamingStrategy naming;
    private final SourceWriter writer;
    private final ListableClassReaderSource classSource;
    private final ClassLoader classLoader;
    private boolean minifying;
    private final Properties properties = new Properties();
    private final ServiceRepository services;
    private DebugInformationEmitter debugEmitter = new DummyDebugInformationEmitter();
    private final Set<MethodReference> asyncMethods;
    private final Set<MethodReference> asyncFamilyMethods;
    private final Diagnostics diagnostics;
    private RenderingContext context;
    private List<PostponedFieldInitializer> postponedFieldInitializers = new ArrayList<>();
    private IntFunction<TeaVMProgressFeedback> progressConsumer = p -> TeaVMProgressFeedback.CONTINUE;
    public static final MethodDescriptor CLINIT_METHOD = new MethodDescriptor("<clinit>", ValueType.VOID);

    private ObjectIntMap<String> sizeByClass = new ObjectIntHashMap<>();
    private int stringPoolSize;
    private int metadataSize;

    private boolean longLibraryUsed;
    private boolean threadLibraryUsed;

    public Renderer(SourceWriter writer, Set<MethodReference> asyncMethods, Set<MethodReference> asyncFamilyMethods,
            Diagnostics diagnostics, RenderingContext context) {
        this.naming = context.getNaming();
        this.writer = writer;
        this.classSource = context.getClassSource();
        this.classLoader = context.getClassLoader();
        this.services = context.getServices();
        this.asyncMethods = new HashSet<>(asyncMethods);
        this.asyncFamilyMethods = new HashSet<>(asyncFamilyMethods);
        this.diagnostics = diagnostics;
        this.context = context;
    }

    public boolean isLongLibraryUsed() {
        return longLibraryUsed;
    }

    public boolean isThreadLibraryUsed() {
        return threadLibraryUsed;
    }

    public int getStringPoolSize() {
        return stringPoolSize;
    }

    public int getMetadataSize() {
        return metadataSize;
    }

    public String[] getClassesInStats() {
        return sizeByClass.keys().toArray(String.class);
    }

    public int getClassSize(String className) {
        return sizeByClass.getOrDefault(className, 0);
    }

    @Override
    public SourceWriter getWriter() {
        return writer;
    }

    @Override
    public NamingStrategy getNaming() {
        return naming;
    }

    @Override
    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    @Override
    public ListableClassReaderSource getClassSource() {
        return classSource;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.putAll(this.properties);
        return properties;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void setDebugEmitter(DebugInformationEmitter debugEmitter) {
        this.debugEmitter = debugEmitter;
    }

    public void setProgressConsumer(IntFunction<TeaVMProgressFeedback> progressConsumer) {
        this.progressConsumer = progressConsumer;
    }

    public void setProperties(Properties properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    public void renderStringPool() throws RenderingException {
        if (context.getStringPool().isEmpty()) {
            return;
        }
        try {
            int start = writer.getOffset();
            writer.append("$rt_stringPool([");
            for (int i = 0; i < context.getStringPool().size(); ++i) {
                if (i > 0) {
                    writer.append(',').ws();
                }
                RenderingUtil.writeString(writer, context.getStringPool().get(i));
            }
            writer.append("]);").newLine();
            stringPoolSize = writer.getOffset() - start;
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderStringConstants() throws RenderingException {
        try {
            for (PostponedFieldInitializer initializer : postponedFieldInitializers) {
                int start = writer.getOffset();
                writer.appendStaticField(initializer.field).ws().append("=").ws();
                context.constantToString(writer, initializer.value);
                writer.append(";").softNewLine();
                int sz = writer.getOffset() - start;
                appendClassSize(initializer.field.getClassName(), sz);
            }
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    public void renderCompatibilityStubs() throws RenderingException {
        try {
            renderJavaStringToString();
            renderJavaObjectToString();
            renderTeaVMClass();
        } catch (IOException e) {
            throw new RenderingException("IO error", e);
        }
    }

    private void renderJavaStringToString() throws IOException {
        writer.appendClass("java.lang.String").append(".prototype.toString").ws().append("=").ws()
                .append("function()").ws().append("{").indent().softNewLine();
        writer.append("return $rt_ustr(this);").softNewLine();
        writer.outdent().append("};").newLine();
        writer.appendClass("java.lang.String").append(".prototype.valueOf").ws().append("=").ws()
            .appendClass("java.lang.String").append(".prototype.toString;").softNewLine();
    }

    private void renderJavaObjectToString() throws IOException {
        writer.appendClass("java.lang.Object").append(".prototype.toString").ws().append("=").ws()
                .append("function()").ws().append("{").indent().softNewLine();
        writer.append("return $rt_ustr(").appendMethodBody(Object.class, "toString", String.class).append("(this));")
                .softNewLine();
        writer.outdent().append("};").newLine();
    }

    private void renderTeaVMClass() throws IOException {
        writer.appendClass("java.lang.Object").append(".prototype.__teavm_class__").ws().append("=").ws()
                .append("function()").ws().append("{").indent().softNewLine();
        writer.append("return $dbg_class(this);").softNewLine();
        writer.outdent().append("};").newLine();
    }

    private void appendClassSize(String className, int sz) {
        sizeByClass.put(className, sizeByClass.getOrDefault(className, 0) + sz);
    }

    private void renderCommonRuntimeAliases() throws IOException {
        renderRuntimeAliases("$rt_throw", "$rt_compare", "$rt_nullCheck", "$rt_cls", "$rt_createArray",
                "$rt_isInstance", "$rt_nativeThread", "$rt_suspending", "$rt_resuming", "$rt_invalidPointer",
                "$rt_s", "$rt_eraseClinit", "$rt_imul", "$rt_wrapException", "$rt_checkBounds",
                "$rt_checkUpperBound", "$rt_checkLowerBound", "$rt_wrapFunction0", "$rt_wrapFunction1",
                "$rt_wrapFunction2", "$rt_wrapFunction3", "$rt_wrapFunction4",
                "$rt_classWithoutFields", "$rt_createArrayFromData", "$rt_createCharArrayFromData",
                "$rt_createByteArrayFromData", "$rt_createShortArrayFromData", "$rt_createIntArrayFromData",
                "$rt_createBooleanArrayFromData", "$rt_createFloatArrayFromData", "$rt_createDoubleArrayFromData",
                "$rt_createLongArrayFromData", "$rt_createBooleanArray", "$rt_createByteArray",
                "$rt_createShortArray", "$rt_createCharArray", "$rt_createIntArray", "$rt_createLongArray",
                "$rt_createFloatArray", "$rt_createDoubleArray", "$rt_compare",
                "$rt_castToClass", "$rt_castToInterface",
                "Long_toNumber", "Long_fromInt", "Long_fromNumber", "Long_create", "Long_ZERO",
                "Long_hi", "Long_lo");
    }

    public void renderLongRuntimeAliases() throws IOException {
        renderRuntimeAliases("Long_add", "Long_sub", "Long_mul", "Long_div", "Long_rem", "Long_or", "Long_and",
                "Long_xor", "Long_shl", "Long_shr", "Long_shru", "Long_compare", "Long_eq", "Long_ne",
                "Long_lt", "Long_le", "Long_gt", "Long_ge", "Long_not", "Long_neg");
    }

    private void renderRuntimeAliases(String... names) throws IOException {
        boolean first = true;
        for (String name : names) {
            if (!first) {
                writer.softNewLine();
            }
            first = false;
            writer.append("var ").appendFunction(name).ws().append('=').ws().append(name).append(";").softNewLine();
        }
        writer.newLine();
    }

    public void prepare(List<PreparedClass> classes) {
        if (minifying) {
            NamingOrderer orderer = new NamingOrderer();
            NameFrequencyEstimator estimator = new NameFrequencyEstimator(orderer, classSource, asyncMethods,
                    asyncFamilyMethods, context.isStrict());
            for (PreparedClass cls : classes) {
                estimator.estimate(cls);
            }
            naming.getScopeName();
            orderer.apply(naming);
        }
    }

    public boolean render(List<PreparedClass> classes) throws RenderingException {
        if (minifying) {
            try {
                renderCommonRuntimeAliases();
            } catch (IOException e) {
                throw new RenderingException(e);
            }
        }
        int index = 0;
        for (PreparedClass cls : classes) {
            int start = writer.getOffset();
            renderDeclaration(cls);
            renderMethodBodies(cls);
            appendClassSize(cls.getName(), writer.getOffset() - start);
            if (progressConsumer.apply(1000 * ++index / classes.size()) == TeaVMProgressFeedback.CANCEL) {
                return false;
            }
        }
        renderClassMetadata(classes);
        return true;
    }

    private void renderDeclaration(PreparedClass cls) throws RenderingException {
        ScopedName jsName = naming.getNameFor(cls.getName());
        debugEmitter.addClass(jsName.value, cls.getName(), cls.getParentName());
        try {
            List<FieldHolder> nonStaticFields = new ArrayList<>();
            List<FieldHolder> staticFields = new ArrayList<>();
            for (FieldHolder field : cls.getClassHolder().getFields()) {
                if (field.getModifiers().contains(ElementModifier.STATIC)) {
                    staticFields.add(field);
                } else {
                    nonStaticFields.add(field);
                }
            }

            if (nonStaticFields.isEmpty() && !cls.getClassHolder().getName().equals("java.lang.Object")) {
                renderShortClassFunctionDeclaration(cls, jsName);
            } else {
                renderFullClassFunctionDeclaration(cls, jsName, nonStaticFields);
            }

            for (FieldHolder field : staticFields) {
                Object value = field.getInitialValue();
                if (value == null) {
                    value = getDefaultValue(field.getType());
                }
                FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
                if (value instanceof String) {
                    context.lookupString((String) value);
                    postponedFieldInitializers.add(new PostponedFieldInitializer(fieldRef, (String) value));
                    value = null;
                }

                ScopedName fieldName = naming.getFullNameFor(fieldRef);
                if (fieldName.scoped) {
                    writer.append(naming.getScopeName()).append(".");
                } else {
                    writer.append("var ");
                }
                writer.append(fieldName.value).ws().append("=").ws();
                context.constantToString(writer, value);
                writer.append(";").softNewLine();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private void renderFullClassFunctionDeclaration(PreparedClass cls, ScopedName jsName,
            List<FieldHolder> nonStaticFields) throws IOException {
        boolean thisAliased = false;
        renderFunctionDeclaration(jsName);
        writer.append("()").ws().append("{").indent().softNewLine();
        if (nonStaticFields.size() > 1) {
            thisAliased = true;
            writer.append("var a").ws().append("=").ws().append("this;").ws();
        }
        if (!cls.getClassHolder().getModifiers().contains(ElementModifier.INTERFACE)
                && cls.getParentName() != null) {
            writer.appendClass(cls.getParentName()).append(".call(").append(thisAliased ? "a" : "this")
                    .append(");").softNewLine();
        }
        for (FieldHolder field : nonStaticFields) {
            Object value = field.getInitialValue();
            if (value == null) {
                value = getDefaultValue(field.getType());
            }
            FieldReference fieldRef = new FieldReference(cls.getName(), field.getName());
            writer.append(thisAliased ? "a" : "this").append(".").appendField(fieldRef).ws()
                    .append("=").ws();
            context.constantToString(writer, value);
            writer.append(";").softNewLine();
            debugEmitter.addField(field.getName(), naming.getNameFor(fieldRef));
        }

        if (cls.getName().equals("java.lang.Object")) {
            writer.append("this.$id$").ws().append('=').ws().append("0;").softNewLine();
        }

        writer.outdent().append("}");
        if (jsName.scoped) {
            writer.append(";");
        }
        writer.newLine();
    }

    private void renderShortClassFunctionDeclaration(PreparedClass cls, ScopedName jsName) throws IOException {
        if (jsName.scoped) {
            writer.append(naming.getScopeName()).append(".");
        } else {
            writer.append("var ");
        }
        writer.append(jsName.value).ws().append("=").ws().appendFunction("$rt_classWithoutFields").append("(");
        if (cls.getClassHolder().hasModifier(ElementModifier.INTERFACE)) {
            writer.append("0");
        } else if (!cls.getParentName().equals("java.lang.Object")) {
            writer.appendClass(cls.getParentName());
        }
        writer.append(");").newLine();
    }

    private void renderMethodBodies(PreparedClass cls) throws RenderingException {
        debugEmitter.emitClass(cls.getName());
        try {
            MethodReader clinit = classSource.get(cls.getName()).getMethod(CLINIT_METHOD);

            if (clinit != null && context.isDynamicInitializer(cls.getName())) {
                renderCallClinit(clinit, cls);
            }
            if (!cls.getClassHolder().hasModifier(ElementModifier.INTERFACE)
                    && !cls.getClassHolder().hasModifier(ElementModifier.ABSTRACT)) {
                for (PreparedMethod method : cls.getMethods()) {
                    if (!method.methodHolder.getModifiers().contains(ElementModifier.STATIC)) {
                        if (method.reference.getName().equals("<init>")) {
                            renderInitializer(method);
                        }
                    }
                }
            }

            for (PreparedMethod method : cls.getMethods()) {
                renderBody(method);
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
        debugEmitter.emitClass(null);
    }

    private void renderCallClinit(MethodReader clinit, PreparedClass cls)
            throws IOException {
        boolean isAsync = asyncMethods.contains(clinit.getReference());

        ScopedName className = naming.getNameFor(cls.getName());
        String clinitCalled = (className.scoped ? naming.getScopeName() + "_" : "") + className.value
                + "_$clinitCalled";
        if (isAsync) {
            writer.append("var ").append(clinitCalled).ws().append("=").ws().append("false;").softNewLine();
        }

        ScopedName name = naming.getNameForClassInit(cls.getName());
        renderFunctionDeclaration(name);
        writer.append("()").ws().append("{").softNewLine().indent();

        if (isAsync) {
            writer.append("var ").append(context.pointerName()).ws().append("=").ws()
                    .append("0").append(";").softNewLine();
            writer.append("if").ws().append("(").appendFunction("$rt_resuming").append("())").ws().append("{")
                    .indent().softNewLine();
            writer.append(context.pointerName()).ws().append("=").ws().appendFunction("$rt_nativeThread")
                    .append("().pop();").softNewLine();
            writer.outdent().append("}").ws();
            writer.append("else if").ws().append("(").append(clinitCalled).append(")").ws()
                    .append("{").indent().softNewLine();
            writer.append("return;").softNewLine();
            writer.outdent().append("}").softNewLine();

            renderAsyncPrologue();

            writer.append("case 0:").indent().softNewLine();
            writer.append(clinitCalled).ws().append('=').ws().append("true;").softNewLine();
        } else {
            renderEraseClinit(cls);
        }

        if (isAsync) {
            writer.append(context.pointerName()).ws().append("=").ws().append("1;").softNewLine();
            writer.outdent().append("case 1:").indent().softNewLine();
        }

        writer.appendMethodBody(new MethodReference(cls.getName(), clinit.getDescriptor()))
                .append("();").softNewLine();

        if (isAsync) {
            writer.append("if").ws().append("(").appendFunction("$rt_suspending").append("())").ws().append("{")
                    .indent().softNewLine();
            writer.append("break " + context.mainLoopName() + ";").softNewLine();
            writer.outdent().append("}").softNewLine();
            renderEraseClinit(cls);
            writer.append("return;").softNewLine().outdent();

            renderAsyncEpilogue();
            writer.appendFunction("$rt_nativeThread").append("().push(" + context.pointerName() + ");").softNewLine();
        }

        writer.outdent().append("}");
        if (name.scoped) {
            writer.append(";");
        }
        writer.newLine();
    }

    private void renderEraseClinit(PreparedClass cls) throws IOException {
        writer.appendClassInit(cls.getName()).ws().append("=").ws()
                .appendFunction("$rt_eraseClinit").append("(")
                .appendClass(cls.getName()).append(");").softNewLine();
    }

    private void renderClassMetadata(List<PreparedClass> classes) {
        if (classes.isEmpty()) {
            return;
        }

        ClassMetadataRequirements metadataRequirements = new ClassMetadataRequirements(context.getDependencyInfo());

        int start = writer.getOffset();
        try {
            writer.append("$rt_packages([");
            ObjectIntMap<String> packageIndexes = generatePackageMetadata(classes, metadataRequirements);
            writer.append("]);").newLine();

            for (int i = 0; i < classes.size(); i += 50) {
                int j = Math.min(i + 50, classes.size());
                renderClassMetadataPortion(classes.subList(i, j), packageIndexes, metadataRequirements);
            }

        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }

        metadataSize = writer.getOffset() - start;
    }

    private void renderClassMetadataPortion(List<PreparedClass> classes, ObjectIntMap<String> packageIndexes,
            ClassMetadataRequirements metadataRequirements) throws IOException {
        writer.append("$rt_metadata([");
        boolean first = true;
        for (PreparedClass cls : classes) {
            if (!first) {
                writer.append(',').softNewLine();
            }
            first = false;
            debugEmitter.emitClass(cls.getName());
            writer.appendClass(cls.getName()).append(",").ws();

            ClassMetadataRequirements.Info requiredMetadata = metadataRequirements.getInfo(cls.getName());
            if (requiredMetadata.name()) {
                String className = cls.getName();
                int dotIndex = className.lastIndexOf('.') + 1;
                String packageName = className.substring(0, dotIndex);
                className = className.substring(dotIndex);
                writer.append("\"").append(RenderingUtil.escapeString(className)).append("\"").append(",").ws();
                writer.append(String.valueOf(packageIndexes.getOrDefault(packageName, -1)));
            } else {
                writer.append("0");
            }
            writer.append(",").ws();

            if (cls.getParentName() != null) {
                writer.appendClass(cls.getParentName());
            } else {
                writer.append("0");
            }
            writer.append(',').ws();
            writer.append("[");
            List<String> interfaces = new ArrayList<>(cls.getClassHolder().getInterfaces());
            for (int i = 0; i < interfaces.size(); ++i) {
                String iface = interfaces.get(i);
                if (i > 0) {
                    writer.append(",").ws();
                }
                writer.appendClass(iface);
            }
            writer.append("],").ws();

            writer.append(ElementModifier.pack(cls.getClassHolder().getModifiers())).append(',').ws();
            writer.append(cls.getClassHolder().getLevel().ordinal()).append(',').ws();

            if (!requiredMetadata.enclosingClass() && !requiredMetadata.declaringClass()
                    && !requiredMetadata.simpleName()) {
                writer.append("0");
            } else {
                writer.append('[');
                if (requiredMetadata.enclosingClass() && cls.getClassHolder().getOwnerName() != null) {
                    writer.appendClass(cls.getClassHolder().getOwnerName());
                } else {
                    writer.append('0');
                }
                writer.append(',');
                if (requiredMetadata.declaringClass() && cls.getClassHolder().getDeclaringClassName() != null) {
                    writer.appendClass(cls.getClassHolder().getDeclaringClassName());
                } else {
                    writer.append('0');
                }
                writer.append(',');
                if (requiredMetadata.simpleName() && cls.getClassHolder().getSimpleName() != null) {
                    writer.append("\"").append(RenderingUtil.escapeString(cls.getClassHolder().getSimpleName()))
                            .append("\"");
                } else {
                    writer.append('0');
                }
                writer.append(']');
            }
            writer.append(",").ws();

            MethodReader clinit = classSource.get(cls.getName()).getMethod(CLINIT_METHOD);
            if (clinit != null && context.isDynamicInitializer(cls.getName())) {
                writer.appendClassInit(cls.getName());
            } else {
                writer.append('0');
            }
            writer.append(',').ws();

            Map<MethodDescriptor, MethodReference> virtualMethods = new LinkedHashMap<>();
            collectMethodsToCopyFromInterfaces(classSource.get(cls.getName()), virtualMethods);
            for (PreparedMethod method : cls.getMethods()) {
                if (!method.methodHolder.getModifiers().contains(ElementModifier.STATIC)
                        && method.methodHolder.getLevel() != AccessLevel.PRIVATE) {
                    virtualMethods.put(method.reference.getDescriptor(), method.reference);
                }
            }

            renderVirtualDeclarations(virtualMethods.values());
            debugEmitter.emitClass(null);
        }
        writer.append("]);").newLine();
    }

    private ObjectIntMap<String> generatePackageMetadata(List<PreparedClass> classes,
            ClassMetadataRequirements metadataRequirements) throws IOException {
        PackageNode root = new PackageNode(null);

        for (PreparedClass classNode : classes) {
            String className = classNode.getName();
            ClassMetadataRequirements.Info requiredMetadata = metadataRequirements.getInfo(className);
            if (!requiredMetadata.name()) {
                continue;
            }

            int dotIndex = className.lastIndexOf('.');
            if (dotIndex < 0) {
                continue;
            }

            addPackageName(root, className.substring(0, dotIndex));
        }

        ObjectIntMap<String> indexes = new ObjectIntHashMap<>();
        writePackageStructure(root, -1, "", indexes);
        writer.softNewLine();
        return indexes;
    }

    private int writePackageStructure(PackageNode node, int startIndex, String prefix, ObjectIntMap<String> indexes)
            throws IOException {
        int index = startIndex;
        for (PackageNode child : node.children.values()) {
            if (index >= 0) {
                writer.append(",").ws();
            }
            writer.append(String.valueOf(startIndex)).append(",").ws()
                    .append("\"").append(RenderingUtil.escapeString(child.name)).append("\"");
            String fullName = prefix + child.name + ".";
            ++index;
            indexes.put(fullName, index);
            index = writePackageStructure(child, index, fullName, indexes);
        }
        return index;
    }

    static class PackageNode {
        String name;
        Map<String, PackageNode> children = new HashMap<>();

        PackageNode(String name) {
            this.name = name;
        }

        int count() {
            int result = 0;
            for (PackageNode child : children.values()) {
                result += 1 + child.count();
            }
            return result;
        }
    }

    private void addPackageName(PackageNode node, String name) {
        String[] parts = name.split("\\.");
        for (String part : parts) {
            node = node.children.computeIfAbsent(part, PackageNode::new);
        }
    }

    private void collectMethodsToCopyFromInterfaces(ClassReader cls, Map<MethodDescriptor, MethodReference> target) {
        Set<MethodDescriptor> implementedMethods = new HashSet<>();
        ClassReader superclass = cls;
        while (superclass != null) {
            for (MethodReader method : superclass.getMethods()) {
                if (method.getLevel() != AccessLevel.PRIVATE && !method.hasModifier(ElementModifier.STATIC)
                        && !method.hasModifier(ElementModifier.ABSTRACT)
                        && !method.getName().equals("<init>")) {
                    implementedMethods.add(method.getDescriptor());
                }
            }
            superclass = superclass.getParent() != null ? classSource.get(superclass.getParent()) : null;
        }

        Set<String> visitedClasses = new HashSet<>();
        superclass = cls;
        while (superclass != null) {
            for (String ifaceName : superclass.getInterfaces()) {
                ClassReader iface = classSource.get(ifaceName);
                if (iface != null) {
                    collectMethodsToCopyFromInterfacesImpl(iface, target, implementedMethods, visitedClasses);
                }
            }
            superclass = superclass.getParent() != null ? classSource.get(superclass.getParent()) : null;
        }
    }

    private void collectMethodsToCopyFromInterfacesImpl(ClassReader cls, Map<MethodDescriptor, MethodReference> target,
            Set<MethodDescriptor> implementedMethods, Set<String> visitedClasses) {
        if (!visitedClasses.add(cls.getName())) {
            return;
        }

        for (String ifaceName : cls.getInterfaces()) {
            ClassReader iface = classSource.get(ifaceName);
            if (iface != null) {
                collectMethodsToCopyFromInterfacesImpl(iface, target, implementedMethods, visitedClasses);
            }
        }

        for (MethodReader method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC)
                    && !method.hasModifier(ElementModifier.ABSTRACT)) {
                MethodDescriptor descriptor = method.getDescriptor();
                if (!implementedMethods.contains(descriptor)) {
                    target.put(descriptor, method.getReference());
                }
            }
        }
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive) type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte) 0;
                case SHORT:
                    return (short) 0;
                case INTEGER:
                    return 0;
                case CHARACTER:
                    return '\0';
                case LONG:
                    return 0L;
                case FLOAT:
                    return 0F;
                case DOUBLE:
                    return 0.0;
            }
        }
        return null;
    }

    private void renderInitializer(PreparedMethod method) throws IOException {
        MethodReference ref = method.reference;
        debugEmitter.emitMethod(ref.getDescriptor());
        ScopedName name = naming.getNameForInit(ref);
        renderFunctionDeclaration(name);
        writer.append("(");
        for (int i = 0; i < ref.parameterCount(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(variableNameForInitializer(i));
        }
        writer.append(")").ws().append("{").softNewLine().indent();

        String instanceName = variableNameForInitializer(ref.parameterCount());
        writer.append("var " + instanceName).ws().append("=").ws().append("new ").appendClass(
                ref.getClassName()).append("();").softNewLine();
        writer.appendMethodBody(ref).append("(" + instanceName);
        for (int i = 0; i < ref.parameterCount(); ++i) {
            writer.append(",").ws();
            writer.append(variableNameForInitializer(i));
        }
        writer.append(");").softNewLine();
        writer.append("return " + instanceName + ";").softNewLine();
        writer.outdent().append("}");
        if (name.scoped) {
            writer.append(";");
        }
        writer.newLine();
        debugEmitter.emitMethod(null);
    }

    private String variableNameForInitializer(int index) {
        return minifying ? RenderingUtil.indexToId(index) : "var_" + index;
    }

    private void renderVirtualDeclarations(Collection<MethodReference> methods) throws IOException {
        if (methods.stream().noneMatch(this::isVirtual)) {
            writer.append('0');
            return;
        }

        writer.append("[");
        boolean first = true;
        for (MethodReference method : methods) {
            if (!isVirtual(method)) {
                continue;
            }
            debugEmitter.emitMethod(method.getDescriptor());
            if (!first) {
                writer.append(",").ws();
            }
            first = false;
            emitVirtualDeclaration(method);
            debugEmitter.emitMethod(null);
        }
        writer.append("]");
    }

    private void emitVirtualDeclaration(MethodReference ref) throws IOException {
        String methodName = naming.getNameFor(ref.getDescriptor());
        writer.append("\"").append(methodName).append("\"");
        writer.append(",").ws();
        emitVirtualFunctionWrapper(ref);
    }

    private void emitVirtualFunctionWrapper(MethodReference method) throws IOException {
        if (method.parameterCount() <= 4) {
            writer.appendFunction("$rt_wrapFunction" + method.parameterCount());
            writer.append("(").appendMethodBody(method).append(")");
            return;
        }

        writer.append("function(");
        List<String> args = new ArrayList<>();
        for (int i = 1; i <= method.parameterCount(); ++i) {
            args.add(variableNameForInitializer(i));
        }
        for (int i = 0; i < args.size(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(args.get(i));
        }
        writer.append(")").ws().append("{").ws();
        if (method.getDescriptor().getResultType() != ValueType.VOID) {
            writer.append("return ");
        }
        writer.appendMethodBody(method).append("(");
        writer.append("this");
        for (String arg : args) {
            writer.append(",").ws().append(arg);
        }
        writer.append(");").ws().append("}");
    }

    private void renderBody(PreparedMethod method) throws IOException {
        StatementRenderer statementRenderer = new StatementRenderer(context, writer);
        statementRenderer.setCurrentMethod(method.node);

        MethodReference ref = method.reference;
        debugEmitter.emitMethod(ref.getDescriptor());
        ScopedName name = naming.getFullNameFor(ref);

        renderFunctionDeclaration(name);
        writer.append("(");
        int startParam = 0;
        if (method.methodHolder.getModifiers().contains(ElementModifier.STATIC)) {
            startParam = 1;
        }
        for (int i = startParam; i <= ref.parameterCount(); ++i) {
            if (i > startParam) {
                writer.append(",").ws();
            }
            writer.append(statementRenderer.variableName(i));
        }
        writer.append(")").ws().append("{").indent();

        MethodBodyRenderer renderer = new MethodBodyRenderer(statementRenderer);
        if (method.node != null) {
            if (!isTrivialBody(method.node)) {
                writer.softNewLine();
                method.node.acceptVisitor(renderer);
            }
        } else {
            writer.softNewLine();
            renderer.renderNative(method);
        }

        writer.outdent().append("}");
        if (name.scoped) {
            writer.append(";");
        }

        writer.newLine();
        debugEmitter.emitMethod(null);

        longLibraryUsed |= statementRenderer.isLongLibraryUsed();
    }

    private static boolean isTrivialBody(MethodNode node) {
        if (!(node instanceof RegularMethodNode)) {
            return false;
        }
        Statement body = ((RegularMethodNode) node).getBody();
        return body instanceof ReturnStatement && ((ReturnStatement) body).getResult() == null;
    }

    private void renderFunctionDeclaration(ScopedName name) throws IOException {
        if (name.scoped) {
            writer.append(naming.getScopeName()).append(".").append(name.value).ws().append("=").ws();
        }
        writer.append("function");
        if (!name.scoped) {
            writer.append(" ").append(name.value);
        }
    }

    private void renderAsyncPrologue() throws IOException {
        writer.append(context.mainLoopName()).append(":").ws().append("while").ws().append("(true)")
                .ws().append("{").ws();
        writer.append("switch").ws().append("(").append(context.pointerName()).append(")").ws()
                .append('{').softNewLine();
    }

    private void renderAsyncEpilogue() throws IOException {
        writer.append("default:").ws().appendFunction("$rt_invalidPointer").append("();").softNewLine();
        writer.append("}}").softNewLine();
    }

    private class MethodBodyRenderer implements MethodNodeVisitor, GeneratorContext {
        private boolean async;
        private StatementRenderer statementRenderer;

        MethodBodyRenderer(StatementRenderer statementRenderer) {
            this.statementRenderer = statementRenderer;
        }

        @Override
        public DependencyInfo getDependency() {
            return context.getDependencyInfo();
        }

        public void renderNative(PreparedMethod method) {
            try {
                this.async = method.async;
                statementRenderer.setAsync(method.async);
                method.generator.generate(this, writer, method.reference);
            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public void visit(RegularMethodNode method) {
            try {
                statementRenderer.setAsync(false);
                this.async = false;
                MethodReference ref = method.getReference();
                for (int i = 0; i < method.getVariables().size(); ++i) {
                    debugEmitter.emitVariable(new String[] { method.getVariables().get(i).getName() },
                            statementRenderer.variableName(i));
                }

                int variableCount = 0;
                for (VariableNode var : method.getVariables()) {
                    variableCount = Math.max(variableCount, var.getIndex() + 1);
                }
                TryCatchFinder tryCatchFinder = new TryCatchFinder();
                method.getBody().acceptVisitor(tryCatchFinder);
                boolean hasTryCatch = tryCatchFinder.tryCatchFound;
                List<String> variableNames = new ArrayList<>();
                for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                    variableNames.add(statementRenderer.variableName(i));
                }
                if (hasTryCatch) {
                    variableNames.add("$$je");
                }
                if (!variableNames.isEmpty()) {
                    writer.append("var ");
                    for (int i = 0; i < variableNames.size(); ++i) {
                        if (i > 0) {
                            writer.append(",").ws();
                        }
                        writer.append(variableNames.get(i));
                    }
                    writer.append(";").softNewLine();
                }

                statementRenderer.setEnd(true);
                statementRenderer.setCurrentPart(0);

                if (method.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_SYNC_METHOD);
                    writer.append("(");
                    appendMonitor(statementRenderer, method);
                    writer.append(");").softNewLine();

                    writer.append("try").ws().append("{").softNewLine().indent();
                }

                method.getBody().acceptVisitor(statementRenderer);

                if (method.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.outdent().append("}").ws().append("finally").ws().append("{").indent().softNewLine();

                    writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_SYNC_METHOD);
                    writer.append("(");
                    appendMonitor(statementRenderer, method);
                    writer.append(");").softNewLine();

                    writer.outdent().append("}").softNewLine();
                }

            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public void visit(AsyncMethodNode methodNode) {
            threadLibraryUsed = true;
            try {
                statementRenderer.setAsync(true);
                this.async = true;
                MethodReference ref = methodNode.getReference();
                for (int i = 0; i < methodNode.getVariables().size(); ++i) {
                    debugEmitter.emitVariable(new String[] { methodNode.getVariables().get(i).getName() },
                            statementRenderer.variableName(i));
                }
                int variableCount = 0;
                for (VariableNode var : methodNode.getVariables()) {
                    variableCount = Math.max(variableCount, var.getIndex() + 1);
                }
                List<String> variableNames = new ArrayList<>();
                for (int i = ref.parameterCount() + 1; i < variableCount; ++i) {
                    variableNames.add(statementRenderer.variableName(i));
                }
                TryCatchFinder tryCatchFinder = new TryCatchFinder();
                for (AsyncMethodPart part : methodNode.getBody()) {
                    if (!tryCatchFinder.tryCatchFound) {
                        part.getStatement().acceptVisitor(tryCatchFinder);
                    }
                }
                boolean hasTryCatch = tryCatchFinder.tryCatchFound;
                if (hasTryCatch) {
                    variableNames.add("$$je");
                }
                variableNames.add(context.pointerName());
                variableNames.add(context.tempVarName());
                writer.append("var ");
                for (int i = 0; i < variableNames.size(); ++i) {
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    writer.append(variableNames.get(i));
                }
                writer.append(";").softNewLine();

                int firstToSave = 0;
                if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
                    firstToSave = 1;
                }

                String popName = minifying ? "l" : "pop";
                String pushName = minifying ? "s" : "push";
                writer.append(context.pointerName()).ws().append('=').ws().append("0;").softNewLine();
                writer.append("if").ws().append("(").appendFunction("$rt_resuming").append("())").ws()
                        .append("{").indent().softNewLine();
                writer.append("var ").append(context.threadName()).ws().append('=').ws()
                        .appendFunction("$rt_nativeThread").append("();").softNewLine();
                writer.append(context.pointerName()).ws().append('=').ws().append(context.threadName()).append(".")
                        .append(popName).append("();");
                for (int i = variableCount - 1; i >= firstToSave; --i) {
                    writer.append(statementRenderer.variableName(i)).ws().append('=').ws().append(context.threadName())
                            .append(".").append(popName).append("();");
                }
                writer.softNewLine();
                writer.outdent().append("}").softNewLine();

                if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.append("try").ws().append('{').indent().softNewLine();
                }

                renderAsyncPrologue();
                for (int i = 0; i < methodNode.getBody().size(); ++i) {
                    writer.append("case ").append(i).append(":").indent().softNewLine();
                    if (i == 0 && methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                        writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_METHOD);
                        writer.append("(");
                        appendMonitor(statementRenderer, methodNode);
                        writer.append(");").softNewLine();
                        statementRenderer.emitSuspendChecker();
                    }
                    AsyncMethodPart part = methodNode.getBody().get(i);
                    statementRenderer.setEnd(true);
                    statementRenderer.setCurrentPart(i);
                    part.getStatement().acceptVisitor(statementRenderer);
                    writer.outdent();
                }
                renderAsyncEpilogue();

                if (methodNode.getModifiers().contains(ElementModifier.SYNCHRONIZED)) {
                    writer.outdent().append("}").ws().append("finally").ws().append('{').indent().softNewLine();
                    writer.append("if").ws().append("(!").appendFunction("$rt_suspending").append("())")
                            .ws().append("{").indent().softNewLine();
                    writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_METHOD);
                    writer.append("(");
                    appendMonitor(statementRenderer, methodNode);
                    writer.append(");").softNewLine();
                    writer.outdent().append('}').softNewLine();
                    writer.outdent().append('}').softNewLine();
                }

                writer.appendFunction("$rt_nativeThread").append("().").append(pushName).append("(");
                for (int i = firstToSave; i < variableCount; ++i) {
                    writer.append(statementRenderer.variableName(i)).append(',').ws();
                }
                writer.append(context.pointerName()).append(");");
                writer.softNewLine();
            } catch (IOException e) {
                throw new RenderingException("IO error occurred", e);
            }
        }

        @Override
        public String getParameterName(int index) {
            return statementRenderer.variableName(index);
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return classSource;
        }

        @Override
        public ClassReaderSource getInitialClassSource() {
            return context.getInitialClassSource();
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Properties getProperties() {
            return new Properties(properties);
        }

        @Override
        public <T> T getService(Class<T> type) {
            return services.getService(type);
        }

        @Override
        public boolean isAsync() {
            return async;
        }

        @Override
        public boolean isAsync(MethodReference method) {
            return asyncMethods.contains(method);
        }

        @Override
        public boolean isAsyncFamily(MethodReference method) {
            return asyncFamilyMethods.contains(method);
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public void typeToClassString(SourceWriter writer, ValueType type) {
            try {
                context.typeToClsString(writer, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void useLongLibrary() {
            longLibraryUsed = true;
        }

        @Override
        public boolean isDynamicInitializer(String className) {
            return context.isDynamicInitializer(className);
        }
    }

    private void appendMonitor(StatementRenderer statementRenderer, MethodNode methodNode) throws IOException {
        if (methodNode.getModifiers().contains(ElementModifier.STATIC)) {
            writer.appendFunction("$rt_cls").append("(")
                    .appendClass(methodNode.getReference().getClassName()).append(")");
        } else {
            writer.append(statementRenderer.variableName(0));
        }
    }

    @Override
    public <T> T getService(Class<T> type) {
        return services.getService(type);
    }

    private static class PostponedFieldInitializer {
        FieldReference field;
        String value;

        PostponedFieldInitializer(FieldReference field, String value) {
            this.field = field;
            this.value = value;
        }
    }

    private boolean isVirtual(MethodReference method) {
        return context.isVirtual(method);
    }
}
