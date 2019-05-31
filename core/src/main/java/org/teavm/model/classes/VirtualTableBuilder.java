/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.model.classes;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.LCATree;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.util.GraphColorer;

public class VirtualTableBuilder {
    private ListableClassReaderSource classes;
    private Map<String, List<MethodDescriptor>> methodsUsedAtCallSites = new HashMap<>();
    private Predicate<MethodReference> methodCalledVirtually = m -> true;
    private Map<String, TableBuilder> tables;
    private Map<String, List<String>> classChildren;
    private LCATree classTree;
    private ObjectIntMap<String> classTreeIndexes;
    private List<String> classList;
    private VirtualTableProvider result;
    private List<MethodDescriptor> methodDescriptors;
    private ObjectIntMap<MethodDescriptor> methodDescriptorIndexes;
    private int[] methodColors;
    private int methodColorCount;

    public VirtualTableBuilder(ListableClassReaderSource classes) {
        this.classes = classes;
    }

    public void setMethodsUsedAtCallSites(Collection<? extends MethodReference> methodsUsedAtCallSites) {
        for (MethodReference method : methodsUsedAtCallSites) {
            this.methodsUsedAtCallSites.computeIfAbsent(method.getClassName(), k -> new ArrayList<>())
                    .add(method.getDescriptor());
        }
    }

    public void setMethodCalledVirtually(Predicate<MethodReference> methodCalledVirtually) {
        this.methodCalledVirtually = methodCalledVirtually;
    }

    public VirtualTableProvider build() {
        tables = new HashMap<>();

        buildVirtualTables();
        cleanupVirtualTables();

        classChildren = new HashMap<>();
        buildClassChildren();
        pack();
        liftEntries();

        buildResult();
        tables = null;
        return result;
    }

    private void buildVirtualTables() {
        for (String className : classes.getClassNames()) {
            fillClass(className);
        }
    }

    private void fillClass(String className) {
        ClassReader cls = classes.get(className);
        if (cls == null) {
            return;
        }
        if (tables.containsKey(className)) {
            return;
        }

        TableBuilder table = new TableBuilder();
        tables.put(className, table);

        String parent = cls.getParent();
        if (parent != null) {
            fillClass(parent);
            TableBuilder parentTable = tables.get(parent);
            if (parentTable != null) {
                copyEntries(parentTable, table);
            }
        }

        for (String itf : cls.getInterfaces()) {
            fillClass(itf);
            TableBuilder itfTable = tables.get(itf);
            if (itfTable != null) {
                copyEntries(itfTable, table);
            }
        }

        List<MethodDescriptor> methodsAtCallSites = methodsUsedAtCallSites.get(className);
        if (methodsAtCallSites != null) {
            for (MethodDescriptor methodDesc : methodsAtCallSites) {
                if (cls.hasModifier(ElementModifier.FINAL) && !table.entries.containsKey(methodDesc)) {
                    continue;
                }
                MethodReader method = cls.getMethod(methodDesc);
                if (method != null && method.getLevel() == AccessLevel.PRIVATE) {
                    continue;
                }
                table.entries.computeIfAbsent(methodDesc, k -> new EntryBuilder());
            }
        }

        for (MethodReader method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.ABSTRACT)
                    || method.hasModifier(ElementModifier.STATIC)
                    || method.getName().equals("<init>")
                    || method.getLevel() == AccessLevel.PRIVATE) {
                continue;
            }

            EntryBuilder entry = table.entries.get(method.getDescriptor());
            if (entry == null) {
                if (cls.hasModifier(ElementModifier.FINAL)) {
                    continue;
                }
                entry = new EntryBuilder();
                table.entries.put(method.getDescriptor(), entry);
            }
            entry.implementor = method.getReference();
        }
    }

    private void copyEntries(TableBuilder source, TableBuilder target) {
        for (Map.Entry<MethodDescriptor, EntryBuilder> entry : source.entries.entrySet()) {
            EntryBuilder targetEntry = target.entries.computeIfAbsent(entry.getKey(), k -> new EntryBuilder());
            targetEntry.addParent(entry.getValue());
            if (entry.getValue().implementor != null && targetEntry.implementor == null) {
                targetEntry.implementor = entry.getValue().implementor;
            }
        }
    }

    private void cleanupVirtualTables() {
        for (String className : classes.getClassNames()) {
            TableBuilder table = tables.get(className);
            for (MethodDescriptor method : table.entries.keySet().toArray(new MethodDescriptor[0])) {
                EntryBuilder entry = table.entries.get(method);
                if (entry.implementor != null && !methodCalledVirtually.test(entry.implementor)) {
                    entry.implementor = null;
                }
            }
        }
    }

    private void buildClassChildren() {
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }
            if (cls.getParent() != null) {
                classChildren.computeIfAbsent(cls.getParent(), c -> new ArrayList<>()).add(className);
            }
        }
    }

    private void liftEntries() {
        buildClassTree();
        for (Map.Entry<MethodDescriptor, List<String>> group : groupMethods().entrySet()) {
            String commonSuperclass = commonSuperclass(group.getValue());
            Set<String> visited = new HashSet<>();
            for (String cls : group.getValue()) {
                liftEntriesAtTable(cls, commonSuperclass, group.getKey(), visited);
            }
        }
        classTree = null;
        classTreeIndexes = null;
        classList = null;
    }

    private void buildClassTree() {
        classTree = new LCATree(classes.getClassNames().size());
        classTreeIndexes = new ObjectIntHashMap<>();
        classList = new ArrayList<>();
        classList.add(null);
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }
            insertClassToTree(className);
        }
    }

    private int insertClassToTree(String className) {
        int index = classTreeIndexes.getOrDefault(className, 0);
        if (index == 0) {
            ClassReader cls = classes.get(className);
            int parent = cls != null && cls.getParent() != null ? insertClassToTree(cls.getParent()) : 0;
            index = classTree.addNode(parent);
            classList.add(className);
            classTreeIndexes.put(className, index);
        }
        return index;
    }

    private String commonSuperclass(List<String> classNames) {
        int result = classTreeIndexes.get(classNames.get(0));
        for (int i = 1; i < classNames.size(); ++i) {
            int next = classTreeIndexes.get(classNames.get(i));
            result = classTree.lcaOf(result, next);
        }
        return classList.get(result);
    }

    private Map<MethodDescriptor, List<String>> groupMethods() {
        Map<MethodDescriptor, List<String>> groups = new LinkedHashMap<>();
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }

            TableBuilder table = tables.get(className);
            TableBuilder parentTable = cls.getParent() != null ? tables.get(cls.getParent()) : null;
            for (MethodDescriptor method : table.entries.keySet()) {
                EntryBuilder entry = table.entries.get(method);
                if (entry.implementor == null) {
                    continue;
                }
                if (parentTable != null) {
                    EntryBuilder parentEntry = parentTable.entries.get(method);
                    if (parentEntry != null && entry.implementor.equals(parentEntry.implementor)) {
                        continue;
                    }
                }

                groups.computeIfAbsent(method, k -> new ArrayList<>()).add(className);
            }
        }

        groups.entrySet().removeIf(entry -> entry.getValue().size() == 1);
        return groups;
    }

    private void liftEntriesAtTable(String className, String toClass, MethodDescriptor method,
            Set<String> visited) {
        while (visited.add(className)) {
            TableBuilder table = tables.get(className);
            if (table == null) {
                break;
            }
            EntryBuilder entry = table.entries.get(method);
            if (entry == null) {
                entry = new EntryBuilder();
                entry.fake = true;
                table.entries.put(method, entry);
            }

            if (className.equals(toClass)) {
                break;
            }
            ClassReader cls = classes.get(className);
            if (cls == null) {
                break;
            }
            className = cls.getParent();
        }
    }

    private void pack() {
        methodDescriptorIndexes = new ObjectIntHashMap<>();
        methodDescriptors = new ArrayList<>();

        GraphBuilder graphBuilder = new GraphBuilder();
        for (String className : classes.getClassNames()) {
            TableBuilder table = tables.get(className);
            MethodDescriptor[] methods = table.entries.keySet().toArray(new MethodDescriptor[0]);
            for (int i = 0; i < methods.length; ++i) {
                for (int j = i + 1; j < methods.length; ++j) {
                    int a = getMethodIndex(methods[i]);
                    int b = getMethodIndex(methods[j]);
                    graphBuilder.addEdge(a, b);
                    graphBuilder.addEdge(b, a);
                }
            }
        }

        Graph interferenceGraph = graphBuilder.build();
        methodColors = new int[methodDescriptors.size()];
        Arrays.fill(methodColors, -1);
        new GraphColorer().colorize(interferenceGraph, methodColors);
        int colorCount = 0;
        for (int i = 0; i < methodColors.length; ++i) {
            colorCount = Math.max(colorCount, methodColors[i]--);
        }
        methodColorCount = colorCount;
    }

    private int getMethodIndex(MethodDescriptor method) {
        int index = methodDescriptorIndexes.getOrDefault(method, -1);
        if (index < 0) {
            index = methodDescriptors.size();
            methodDescriptors.add(method);
            methodDescriptorIndexes.put(method, index);
        }
        return index;
    }

    private void buildResult() {
        result = new VirtualTableProvider();
        buildResultForClasses();
        buildResultForInterfaces();
    }

    private void buildResultForClasses() {
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (cls.hasModifier(ElementModifier.INTERFACE) || cls.getParent() != null) {
                continue;
            }

            Context context = new Context();
            context.indexes = new int[methodColorCount];
            Arrays.fill(context.indexes, -1);
            buildResultForClass(className, context, null);
        }
    }

    private void buildResultForClass(String className, Context context, VirtualTable parent) {
        TableBuilder table = tables.get(className);
        ClassReader cls = classes.get(className);

        int colorsStart = context.colors.size();
        int initialMethodsStart = parent != null ? parent.size() : 0;
        int methodsStart = initialMethodsStart;
        IntArrayList resolvedIndexes = new IntArrayList();
        Map<MethodDescriptor, VirtualTableEntry> resultEntries = new HashMap<>();
        for (MethodDescriptor method : table.entries.keySet()) {
            int color = methodColors[methodDescriptorIndexes.get(method)];
            EntryBuilder entry = table.entries.get(method);
            int index = context.indexes[color];
            if (index < 0) {
                index = context.colors.size();
                context.indexes[color] = index;
                context.colors.add(color);
                context.methods.add(null);
            }

            if (entry.implementor != null) {
                VirtualTableEntry resultEntry = new VirtualTableEntry(method, entry.implementor, index);
                resultEntries.put(method, resultEntry);
                propagateInterfaceIndexes(cls, method, index);
            }

            if (context.methods.get(index) == null && !entry.fake) {
                context.methods.set(index, method);
                resolvedIndexes.add(index);
                while (index < methodsStart) {
                    methodsStart -= parent.getMethods().size();
                    parent = parent.getParent();
                    if (parent == null) {
                        break;
                    }
                }
            }
        }

        List<MethodDescriptor> newMethods = context.methods.subList(methodsStart, context.methods.size());
        Set<MethodDescriptor> methodSet = new HashSet<>();
        for (MethodDescriptor method : newMethods) {
            if (method != null) {
                methodSet.add(method);
            }
        }
        List<? extends MethodDescriptor> readonlyNewMethods = Collections.unmodifiableList(
                Arrays.asList(newMethods.toArray(new MethodDescriptor[0])));
        VirtualTable resultTable = new VirtualTable(className, parent, readonlyNewMethods,
                methodSet, resultEntries);
        result.virtualTables.put(className, resultTable);

        List<String> children = classChildren.get(className);
        if (children != null) {
            for (String child : children) {
                buildResultForClass(child, context, resultTable);
            }
        }

        for (int i = colorsStart; i < context.colors.size(); ++i) {
            context.indexes[context.colors.get(i)] = -1;
        }
        context.colors.removeRange(colorsStart, context.colors.size());
        for (IntCursor resolvedIndex : resolvedIndexes) {
            context.methods.set(resolvedIndex.value, null);
        }
        context.methods.subList(initialMethodsStart, context.methods.size()).clear();
    }

    private void propagateInterfaceIndexes(ClassReader cls, MethodDescriptor method, int index) {
        while (true) {
            for (String itf : cls.getInterfaces()) {
                TableBuilder itfTable = tables.get(itf);
                if (itfTable != null) {
                    EntryBuilder itfEntry = itfTable.entries.get(method);
                    if (itfEntry != null) {
                        propagateInterfaceIndex(itfEntry, index);
                    }
                }
            }

            if (cls.getParent() == null) {
                break;
            }
            cls = classes.get(cls.getParent());
            if (cls == null) {
                break;
            }

            TableBuilder table = tables.get(cls.getName());
            EntryBuilder entry = table.entries.get(method);
            if (entry == null || entry.implementor != null) {
                break;
            }
        }
    }

    private void propagateInterfaceIndex(EntryBuilder entry, int index) {
        if (entry.index >= 0) {
            return;
        }
        entry.index = index;
        if (entry.parents != null) {
            for (EntryBuilder parent : entry.parents) {
                propagateInterfaceIndex(parent, index);
            }
        }
    }

    private void buildResultForInterfaces() {
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (!cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }

            List<MethodDescriptor> methods = new ArrayList<>();
            Set<MethodDescriptor> methodSet = new HashSet<>();
            TableBuilder table = tables.get(className);
            for (MethodDescriptor method : table.entries.keySet()) {
                EntryBuilder entry = table.entries.get(method);
                if (entry.index < 0) {
                    continue;
                }
                if (entry.index >= methods.size()) {
                    methods.addAll(Collections.nCopies(entry.index - methods.size() + 1, null));
                }
                methods.set(entry.index, method);
                methodSet.add(method);
            }

            List<? extends MethodDescriptor> readonlyNewMethods = Collections.unmodifiableList(
                    Arrays.asList(methods.toArray(new MethodDescriptor[0])));
            VirtualTable resultTable = new VirtualTable(className, null, readonlyNewMethods,
                    methodSet, Collections.emptyMap());
            result.virtualTables.put(className, resultTable);
        }
    }

    static class TableBuilder {
        Map<MethodDescriptor, EntryBuilder> entries = new LinkedHashMap<>();
    }

    static class EntryBuilder {
        MethodReference implementor;
        EntryBuilder[] parents;
        int index = -1;
        boolean fake;

        void addParent(EntryBuilder parent) {
            if (parents == null) {
                parents = new EntryBuilder[] { parent };
            } else {
                parents = Arrays.copyOf(parents, parents.length + 1);
                parents[parents.length - 1] = parent;
            }
        }
    }

    static class Context {
        int[] indexes;
        IntArrayList colors = new IntArrayList();
        List<MethodDescriptor> methods = new ArrayList<>();
    }
}
