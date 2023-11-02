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
import org.teavm.backend.javascript.codegen.NamingOrderer;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.OutputSourceWriter;
import org.teavm.backend.javascript.codegen.RememberedSource;
import org.teavm.backend.javascript.codegen.ScopedName;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.decompile.PreparedClass;
import org.teavm.backend.javascript.decompile.PreparedMethod;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DummyDebugInformationEmitter;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
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
    private final OutputSourceWriter writer;
    private final ListableClassReaderSource classSource;
    private final ClassLoader classLoader;
    private boolean minifying;
    private final Properties properties = new Properties();
    private final ServiceRepository services;
    private DebugInformationEmitter debugEmitter = new DummyDebugInformationEmitter();
    private final Set<MethodReference> asyncMethods;
    private final Set<MethodReference> asyncFamilyMethods;
    private RenderingContext context;
    private List<PostponedFieldInitializer> postponedFieldInitializers = new ArrayList<>();
    private IntFunction<TeaVMProgressFeedback> progressConsumer = p -> TeaVMProgressFeedback.CONTINUE;
    public static final MethodDescriptor CLINIT_METHOD = new MethodDescriptor("<clinit>", ValueType.VOID);

    private ObjectIntMap<String> sizeByClass = new ObjectIntHashMap<>();
    private int stringPoolSize;
    private int metadataSize;

    public Renderer(OutputSourceWriter writer, Set<MethodReference> asyncMethods,
            Set<MethodReference> asyncFamilyMethods, RenderingContext context) {
        this.naming = context.getNaming();
        this.writer = writer;
        this.classSource = context.getClassSource();
        this.classLoader = context.getClassLoader();
        this.services = context.getServices();
        this.asyncMethods = new HashSet<>(asyncMethods);
        this.asyncFamilyMethods = new HashSet<>(asyncFamilyMethods);
        this.context = context;
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
        int start = writer.getOffset();
        writer.appendFunction("$rt_stringPool").append("([");
        for (int i = 0; i < context.getStringPool().size(); ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            RenderingUtil.writeString(writer, context.getStringPool().get(i));
        }
        writer.append("]);").newLine();
        stringPoolSize = writer.getOffset() - start;
    }

    public void renderStringConstants() throws RenderingException {
        for (PostponedFieldInitializer initializer : postponedFieldInitializers) {
            int start = writer.getOffset();
            writer.appendStaticField(initializer.field).ws().append("=").ws();
            context.constantToString(writer, initializer.value);
            writer.append(";").softNewLine();
            int sz = writer.getOffset() - start;
            appendClassSize(initializer.field.getClassName(), sz);
        }
    }

    public void renderCompatibilityStubs() throws RenderingException {
        renderJavaStringToString();
        renderJavaObjectToString();
        renderTeaVMClass();
    }

    private void renderJavaStringToString() {
        writer.appendClass("java.lang.String").append(".prototype.toString").ws().append("=").ws().append("()")
                .sameLineWs().append("=>").ws().append("{").indent().softNewLine();
        writer.append("return ").appendFunction("$rt_ustr").append("(this);").softNewLine();
        writer.outdent().append("};").newLine();
        writer.appendClass("java.lang.String").append(".prototype.valueOf").ws().append("=").ws()
            .appendClass("java.lang.String").append(".prototype.toString;").softNewLine();
    }

    private void renderJavaObjectToString() {
        writer.appendClass("java.lang.Object").append(".prototype.toString").ws().append("=").ws()
                .append("()").sameLineWs().append("=>").ws().append("{").indent().softNewLine();
        writer.append("return ").appendFunction("$rt_ustr").append("(")
                .appendMethodBody(Object.class, "toString", String.class).append("(this));")
                .softNewLine();
        writer.outdent().append("};").newLine();
    }

    private void renderTeaVMClass() {
        writer.appendClass("java.lang.Object").append(".prototype.__teavm_class__").ws().append("=").ws()
                .append("function()").ws().append("{").indent().softNewLine();
        writer.append("return ").appendFunction("$dbg_class").append("(this);").softNewLine();
        writer.outdent().append("};").newLine();
    }

    private void appendClassSize(String className, int sz) {
        sizeByClass.put(className, sizeByClass.getOrDefault(className, 0) + sz);
    }

    public void prepare(List<PreparedClass> classes) {
        if (minifying) {
            NamingOrderer orderer = new NamingOrderer();
            NameFrequencyEstimator estimator = new NameFrequencyEstimator(orderer, classSource,
                    asyncFamilyMethods);
            for (PreparedClass cls : classes) {
                estimator.estimate(cls);
            }
            naming.getScopeName();
            orderer.apply(naming);
        }
    }

    public boolean render(List<PreparedClass> classes) throws RenderingException {
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
                writer.append("let ");
            }
            writer.append(fieldName.value).ws().append("=").ws();
            context.constantToString(writer, value);
            writer.append(";").softNewLine();
        }
    }

    private void renderFullClassFunctionDeclaration(PreparedClass cls, ScopedName jsName,
            List<FieldHolder> nonStaticFields) {
        boolean thisAliased = false;
        renderFunctionDeclaration(jsName);
        writer.append("()").ws().append("{").indent().softNewLine();
        if (nonStaticFields.size() > 1) {
            thisAliased = true;
            writer.append("let a").ws().append("=").ws().append("this;").ws();
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

    private void renderShortClassFunctionDeclaration(PreparedClass cls, ScopedName jsName) {
        if (jsName.scoped) {
            writer.append(naming.getScopeName()).append(".");
        } else {
            writer.append("let ");
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

        MethodReader clinit = classSource.get(cls.getName()).getMethod(CLINIT_METHOD);

        if (clinit != null && context.isDynamicInitializer(cls.getName())) {
            renderCallClinit(clinit, cls);
        }
        if (!cls.getClassHolder().hasModifier(ElementModifier.INTERFACE)
                && !cls.getClassHolder().hasModifier(ElementModifier.ABSTRACT)) {
            for (PreparedMethod method : cls.getMethods()) {
                if (!method.modifiers.contains(ElementModifier.STATIC)) {
                    if (method.reference.getName().equals("<init>")) {
                        renderInitializer(method);
                    }
                }
            }
        }

        for (PreparedMethod method : cls.getMethods()) {
            renderBody(method);
        }

        debugEmitter.emitClass(null);
    }

    private void renderCallClinit(MethodReader clinit, PreparedClass cls) {
        boolean isAsync = asyncMethods.contains(clinit.getReference());

        ScopedName className = naming.getNameFor(cls.getName());
        String clinitCalled = (className.scoped ? naming.getScopeName() + "_" : "") + className.value
                + "_$clinitCalled";
        if (isAsync) {
            writer.append("let ").append(clinitCalled).ws().append("=").ws().append("false;").softNewLine();
        }

        ScopedName name = naming.getNameForClassInit(cls.getName());
        renderLambdaDeclaration(name);
        writer.append("()").sameLineWs().append("=>").ws().append("{").softNewLine().indent();

        if (isAsync) {
            writer.append("let ").append(context.pointerName()).ws().append("=").ws()
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

            renderAsyncPrologue(writer, context);

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

            renderAsyncEpilogue(writer);
            writer.appendFunction("$rt_nativeThread").append("().push(" + context.pointerName() + ");").softNewLine();
        }

        writer.outdent().append("}");
        if (name.scoped) {
            writer.append(";");
        }
        writer.newLine();
    }

    private void renderEraseClinit(PreparedClass cls) {
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

        writer.appendFunction("$rt_packages").append("([");
        ObjectIntMap<String> packageIndexes = generatePackageMetadata(classes, metadataRequirements);
        writer.append("]);").newLine();

        for (int i = 0; i < classes.size(); i += 50) {
            int j = Math.min(i + 50, classes.size());
            renderClassMetadataPortion(classes.subList(i, j), packageIndexes, metadataRequirements);
        }

        metadataSize = writer.getOffset() - start;
    }

    private void renderClassMetadataPortion(List<PreparedClass> classes, ObjectIntMap<String> packageIndexes,
            ClassMetadataRequirements metadataRequirements) {
        writer.appendFunction("$rt_metadata").append("([");
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
                if (!method.modifiers.contains(ElementModifier.STATIC)
                        && method.accessLevel != AccessLevel.PRIVATE) {
                    virtualMethods.put(method.reference.getDescriptor(), method.reference);
                }
            }

            renderVirtualDeclarations(virtualMethods.values());
            debugEmitter.emitClass(null);
        }
        writer.append("]);").newLine();
    }

    private ObjectIntMap<String> generatePackageMetadata(List<PreparedClass> classes,
            ClassMetadataRequirements metadataRequirements) {
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

    private int writePackageStructure(PackageNode node, int startIndex, String prefix, ObjectIntMap<String> indexes) {
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

    private void renderInitializer(PreparedMethod method) {
        MethodReference ref = method.reference;
        debugEmitter.emitMethod(ref.getDescriptor());
        ScopedName name = naming.getNameForInit(ref);
        renderLambdaDeclaration(name);
        if (ref.parameterCount() != 1) {
            writer.append("(");
        }
        for (int i = 0; i < ref.parameterCount(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(variableNameForInitializer(i));
        }
        if (ref.parameterCount() != 1) {
            writer.append(")");
        }
        writer.sameLineWs().append("=>").ws().append("{").softNewLine().indent();

        String instanceName = variableNameForInitializer(ref.parameterCount());
        writer.append("let " + instanceName).ws().append("=").ws().append("new ").appendClass(
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

    private void renderVirtualDeclarations(Collection<MethodReference> methods) {
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

    private void emitVirtualDeclaration(MethodReference ref) {
        String methodName = naming.getNameFor(ref.getDescriptor());
        writer.append("\"").append(methodName).append("\"");
        writer.append(",").ws();
        emitVirtualFunctionWrapper(ref);
    }

    private void emitVirtualFunctionWrapper(MethodReference method) {
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

    private void renderBody(PreparedMethod method) {
        MethodReference ref = method.reference;
        debugEmitter.emitMethod(ref.getDescriptor());
        ScopedName name = naming.getFullNameFor(ref);

        renderLambdaDeclaration(name);
        method.parameters.replay(writer, RememberedSource.FILTER_ALL);
        if (method.variables != null) {
            for (var variable : method.variables) {
                variable.emit(debugEmitter);
            }
        }
        writer.sameLineWs().append("=>").ws().append("{").indent().softNewLine();
        method.body.replay(writer, RememberedSource.FILTER_ALL);

        writer.outdent().append("}");
        if (name.scoped) {
            writer.append(";");
        }

        writer.newLine();
        debugEmitter.emitMethod(null);
    }

    private void renderLambdaDeclaration(ScopedName name) {
        if (name.scoped) {
            writer.append(naming.getScopeName()).append(".").append(name.value);
        } else {
            writer.append("let ").append(name.value);
        }
        writer.ws().append("=").ws();
    }

    private void renderFunctionDeclaration(ScopedName name) {
        if (name.scoped) {
            writer.append(naming.getScopeName()).append(".").append(name.value).ws().append("=").ws();
        }
        writer.append("function");
        if (!name.scoped) {
            writer.append(" ").append(name.value);
        }
    }

    static void renderAsyncPrologue(SourceWriter writer, RenderingContext context) {
        writer.append(context.mainLoopName()).append(":").ws().append("while").ws().append("(true)")
                .ws().append("{").ws();
        writer.append("switch").ws().append("(").append(context.pointerName()).append(")").ws()
                .append('{').softNewLine();
    }

    static void renderAsyncEpilogue(SourceWriter writer) {
        writer.append("default:").ws().appendFunction("$rt_invalidPointer").append("();").softNewLine();
        writer.append("}}").softNewLine();
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
