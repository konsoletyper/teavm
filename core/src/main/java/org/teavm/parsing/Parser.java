/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.parsing;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainer;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.Phi;
import org.teavm.model.PrimitiveType;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.optimization.UnreachableBasicBlockEliminator;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.PhiUpdater;
import org.teavm.model.util.ProgramNodeSplittingBackend;
import org.teavm.model.util.ProgramUtils;

public class Parser {
    private static final int DECL_CLASS = 0;
    private static final int DECL_METHOD = 1;
    private static final int DECL_FIELD = 2;
    private ReferenceCache referenceCache;

    public Parser(ReferenceCache referenceCache) {
        this.referenceCache = referenceCache;
    }

    public MethodHolder parseMethod(MethodNode node, String fileName) {
        MethodNode nodeWithoutJsr = new MethodNode(Opcodes.ASM7, node.access, node.name, node.desc, node.signature,
                node.exceptions.toArray(new String[0]));
        JSRInlinerAdapter adapter = new JSRInlinerAdapter(nodeWithoutJsr, node.access, node.name, node.desc,
                node.signature, node.exceptions.toArray(new String[0]));
        node.accept(adapter);
        node = nodeWithoutJsr;
        ValueType[] signature = MethodDescriptor.parseSignature(node.desc);
        MethodHolder method = new MethodHolder(referenceCache.getCached(new MethodDescriptor(node.name, signature)));
        parseModifiers(node.access, method, DECL_METHOD);
        parseAnnotations(method.getAnnotations(), node.visibleAnnotations, node.invisibleAnnotations);

        if (node.instructions.size() > 0) {
            ProgramParser programParser = new ProgramParser(referenceCache);
            programParser.setFileName(fileName);
            Program program = programParser.parse(node);
            new UnreachableBasicBlockEliminator().optimize(program);

            Graph cfg = ProgramUtils.buildControlFlowGraph(program);
            if (GraphUtils.isIrreducible(cfg)) {
                ProgramNodeSplittingBackend be = new ProgramNodeSplittingBackend(program);
                int[] weights = new int[program.basicBlockCount()];
                for (int i = 0; i < weights.length; ++i) {
                    int count = 0;
                    Instruction insn = program.basicBlockAt(i).getFirstInstruction();
                    while (insn != null) {
                        count++;
                        insn = insn.getNext();
                    }
                    weights[i] = count;
                }
                GraphUtils.splitIrreducibleGraph(cfg, weights, be);
            }

            PhiUpdater phiUpdater = new PhiUpdater();
            Variable[] argumentMapping = applySignature(program, method.getParameterTypes());
            phiUpdater.updatePhis(program, argumentMapping);
            method.setProgram(program);
            applyDebugNames(program, phiUpdater, programParser, argumentMapping);

            applyDebugNames(program, phiUpdater, programParser,
                    applySignature(program, method.getDescriptor().getParameterTypes()));

            while (program.variableCount() <= method.parameterCount()) {
                program.createVariable();
            }
        }

        if (node.annotationDefault != null) {
            method.setAnnotationDefault(parseAnnotationValue(node.annotationDefault));
        }
        for (int i = 0; i < method.parameterCount(); ++i) {
            parseAnnotations(method.parameterAnnotation(i),
                    node.visibleParameterAnnotations != null ? node.visibleParameterAnnotations[i] : null,
                    node.invisibleParameterAnnotations != null ? node.invisibleParameterAnnotations[i] : null);
        }

        if (node.signature != null) {
            parseMethodGenericSignature(node.signature, method);
        }

        return method;
    }

    private void parseMethodGenericSignature(String signature, MethodHolder method) {
        GenericValueType.ParsePosition position = new GenericValueType.ParsePosition();

        List<GenericTypeParameter> typeParameters = new ArrayList<>();
        String elementName = "method '" + method.getDescriptor() + "'";
        if (signature.charAt(position.index) == '<') {
            parseTypeParameters(signature, typeParameters, position, elementName);
        }

        List<GenericValueType> parameters = new ArrayList<>();
        if (signature.charAt(position.index) != '(') {
            throw couldNotParseSignature(elementName, signature);
        }
        position.index++;
        while (signature.charAt(position.index) != ')') {
            GenericValueType parameter = GenericValueType.parse(signature, position);
            if (parameter == null) {
                throw couldNotParseSignature(elementName, signature);
            }
            parameters.add(parameter);
        }
        position.index++;
        GenericValueType returnType = GenericValueType.parse(signature, position);
        if (returnType == null) {
            throw couldNotParseSignature(elementName, signature);
        }

        if (position.index < signature.length() && signature.charAt(position.index) != '^') {
            throw couldNotParseSignature(elementName, signature);
        }

        method.setTypeParameters(typeParameters.toArray(new GenericTypeParameter[0]));
        method.setGenericSignature(returnType, parameters.toArray(new GenericValueType[0]));
    }

    private static void applyDebugNames(Program program, PhiUpdater phiUpdater, ProgramParser parser,
            Variable[] argumentMapping) {
        if (program.basicBlockCount() == 0) {
            return;
        }

        IntIntMap[] blockEntryVariableMappings = getBlockEntryVariableMappings(program, phiUpdater, argumentMapping);

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        Map<Integer, String> debugNames = new HashMap<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            IntIntMap varMap = blockEntryVariableMappings[i];
            for (Instruction insn : block) {
                insn.acceptVisitor(defExtractor);
                Map<Integer, String> newDebugNames = parser.getDebugNames(insn);
                if (newDebugNames != null) {
                    debugNames = newDebugNames;
                }
                for (Variable definedVar : defExtractor.getDefinedVariables()) {
                    int sourceVar = phiUpdater.getSourceVariable(definedVar.getIndex());
                    if (sourceVar >= 0) {
                        varMap.put(sourceVar, definedVar.getIndex());
                    }
                }
                for (Map.Entry<Integer, String> debugName : debugNames.entrySet()) {
                    int receiver = varMap.getOrDefault(debugName.getKey(), -1);
                    if (receiver >= 0) {
                        Variable receiverVar = program.variableAt(receiver);
                        receiverVar.setDebugName(debugName.getValue());
                        receiverVar.setLabel(debugName.getValue());
                    }
                }
            }
        }
    }

    private static IntIntMap[] getBlockEntryVariableMappings(Program program, PhiUpdater phiUpdater,
            Variable[] argumentMapping) {
        class Step {
            int node;
            IntIntMap varMap;

            Step(int node, IntIntMap varMap) {
                this.node = node;
                this.varMap = varMap;
            }
        }

        IntIntMap[] result = new IntIntMap[program.basicBlockCount()];
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        Graph dom = GraphUtils.buildDominatorGraph(GraphUtils.buildDominatorTree(cfg), cfg.size());
        Step[] stack = new Step[program.basicBlockCount()];
        int top = 0;

        IntIntHashMap entryVarMap = new IntIntHashMap();
        for (int i = 0; i < argumentMapping.length; ++i) {
            Variable arg = argumentMapping[i];
            if (arg != null) {
                entryVarMap.put(i, arg.getIndex());
            }
        }
        stack[top++] = new Step(0, entryVarMap);

        while (top > 0) {
            Step step = stack[--top];
            int node = step.node;
            IntIntMap varMap = new IntIntHashMap(step.varMap);
            BasicBlock block = program.basicBlockAt(node);

            for (Phi phi : block.getPhis()) {
                int receiver = phi.getReceiver().getIndex();
                int sourceVar = phiUpdater.getSourceVariable(receiver);
                if (sourceVar >= 0) {
                    varMap.put(sourceVar, receiver);
                }
            }

            result[node] = new IntIntHashMap(varMap);

            for (Instruction insn : block) {
                insn.acceptVisitor(defExtractor);
                for (Variable definedVar : defExtractor.getDefinedVariables()) {
                    int sourceVar = phiUpdater.getSourceVariable(definedVar.getIndex());
                    if (sourceVar >= 0) {
                        varMap.put(sourceVar, definedVar.getIndex());
                    }
                }
            }

            for (int successor : dom.outgoingEdges(node)) {
                stack[top++] = new Step(successor, new IntIntHashMap(varMap));
            }
        }

        return result;
    }

    private Variable[] applySignature(Program program, ValueType[] arguments) {
        if (program.variableCount() == 0) {
            return new Variable[0];
        }

        Variable[] variableMap = new Variable[program.variableCount()];
        int index = 0;
        variableMap[index] = program.variableAt(index);
        ++index;
        for (int i = 0; i < arguments.length; ++i) {
            variableMap[index] = program.variableAt(i + 1);
            ++index;
            ValueType arg = arguments[i];
            if (arg instanceof ValueType.Primitive) {
                PrimitiveType kind = ((ValueType.Primitive) arg).getKind();
                if (kind == PrimitiveType.LONG || kind == PrimitiveType.DOUBLE) {
                    variableMap[index] = variableMap[index - 1];
                    ++index;
                }
            }
        }

        return Arrays.copyOf(variableMap, index);
    }

    public ClassHolder parseClass(ClassNode node) {
        ClassHolder cls = new ClassHolder(referenceCache.getCached(node.name.replace('/', '.')));
        parseModifiers(node.access, cls, DECL_CLASS);
        if (node.superName != null) {
            cls.setParent(referenceCache.getCached(node.superName.replace('/', '.')));
        }
        if (cls.getName().equals("java.lang.Object")) {
            cls.setParent(null);
        }
        if (node.interfaces != null) {
            for (String iface : node.interfaces) {
                cls.getInterfaces().add(referenceCache.getCached(iface.replace('/', '.')));
            }
        }

        if (node.signature != null) {
            parseSignature(cls, node.signature);
        }

        for (Object obj : node.fields) {
            FieldNode fieldNode = (FieldNode) obj;
            FieldHolder field = parseField(fieldNode);
            cls.addField(field);
            field.updateReference(referenceCache);
        }
        String fullFileName = referenceCache.getCached(node.name.substring(0, node.name.lastIndexOf('/') + 1)
                + node.sourceFile);
        for (MethodNode methodNode : node.methods) {
            MethodHolder method = parseMethod(methodNode, fullFileName);
            cls.addMethod(method);
            method.updateReference(referenceCache);
        }

        if (node.outerClass != null) {
            cls.setOwnerName(node.outerClass.replace('/', '.'));
        }

        if (node.innerClasses != null && !node.innerClasses.isEmpty()) {
            for (InnerClassNode innerClassNode : node.innerClasses) {
                if (node.name.equals(innerClassNode.name)) {
                    if (innerClassNode.outerName != null) {
                        cls.setDeclaringClassName(innerClassNode.outerName.replace('/', '.'));
                        cls.setOwnerName(cls.getDeclaringClassName());
                    }
                    cls.setSimpleName(innerClassNode.innerName);
                    break;
                }
            }
        }

        parseAnnotations(cls.getAnnotations(), node.visibleAnnotations, node.invisibleAnnotations);
        return cls;
    }

    private void parseSignature(ClassHolder cls, String signature) {
        GenericValueType.ParsePosition position = new GenericValueType.ParsePosition();
        List<GenericTypeParameter> typeParameters = new ArrayList<>();

        String elementName = "class '" + cls.getName() + "'";
        if (signature.charAt(position.index) == '<') {
            parseTypeParameters(signature, typeParameters, position, elementName);
        }

        cls.setGenericParameters(typeParameters.toArray(new GenericTypeParameter[0]));

        GenericValueType.Object supertype = GenericValueType.parseObject(signature, position);
        if (supertype == null) {
            throw couldNotParseSignature(elementName, signature);
        }
        cls.setGenericParent(supertype);

        List<GenericValueType.Object> interfaces = new ArrayList<>();
        while (position.index < signature.length()) {
            GenericValueType.Object itf = GenericValueType.parseObject(signature, position);
            if (itf == null) {
                throw couldNotParseSignature(elementName, signature);
            }
            interfaces.add(itf);
        }

        cls.getGenericInterfaces().addAll(interfaces);
    }

    private void parseTypeParameters(String signature, List<GenericTypeParameter> typeParameters,
            GenericValueType.ParsePosition position, String elementName) {
        position.index++;
        do {
            if (position.index >= signature.length()) {
                throw couldNotParseSignature(elementName, signature);
            }
            int next = signature.indexOf(':', position.index);
            if (next < 0 || next == position.index) {
                throw couldNotParseSignature(elementName, signature);
            }
            String name = signature.substring(position.index, next);
            position.index = next;

            List<GenericValueType.Reference> bounds = new ArrayList<>();
            while (true) {
                if (position.index >= signature.length()) {
                    throw couldNotParseSignature(elementName, signature);
                }
                char c = signature.charAt(position.index);
                if (c != ':') {
                    break;
                }
                position.index++;
                if (bounds.isEmpty() && signature.charAt(position.index) == ':') {
                    bounds.add(null);
                } else {
                    GenericValueType.Reference bound = GenericValueType.parseReference(signature, position);
                    if (bound == null) {
                        throw couldNotParseSignature(elementName, signature);
                    }
                    bounds.add(bound);
                }
            }

            typeParameters.add(new GenericTypeParameter(name, bounds.get(0),
                    bounds.subList(1, bounds.size()).toArray(new GenericValueType.Reference[0])));
        } while (signature.charAt(position.index) != '>');

        position.index++;
    }

    private IllegalArgumentException couldNotParseSignature(String forElement, String signature) {
        return new IllegalArgumentException("Could not parse class signature '" + signature + "' for " + forElement);
    }

    public FieldHolder parseField(FieldNode node) {
        FieldHolder field = new FieldHolder(referenceCache.getCached(node.name));
        field.setType(referenceCache.getCached(ValueType.parse(node.desc)));
        field.setInitialValue(node.value);
        parseModifiers(node.access, field, DECL_FIELD);
        parseAnnotations(field.getAnnotations(), node.visibleAnnotations, node.invisibleAnnotations);

        if (node.signature != null) {
            GenericValueType.ParsePosition position = new GenericValueType.ParsePosition();
            GenericValueType type = GenericValueType.parse(node.signature, position);
            if (type == null || position.index < node.signature.length()) {
                throw couldNotParseSignature("field '" + field.getReference() + "'", node.signature);
            }
            field.setGenericType(type);
        }

        return field;
    }

    public void parseModifiers(int access, ElementHolder member, int type) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            member.setLevel(AccessLevel.PRIVATE);
        } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
            member.setLevel(AccessLevel.PROTECTED);
        } else if ((access & Opcodes.ACC_PUBLIC) != 0) {
            member.setLevel(AccessLevel.PUBLIC);
        }

        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            member.getModifiers().add(ElementModifier.ABSTRACT);
        }
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            member.getModifiers().add(ElementModifier.ANNOTATION);
        }
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            if (type == DECL_METHOD) {
                member.getModifiers().add(ElementModifier.BRIDGE);
            }
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            member.getModifiers().add(ElementModifier.DEPRECATED);
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            member.getModifiers().add(ElementModifier.ENUM);
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            member.getModifiers().add(ElementModifier.FINAL);
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            member.getModifiers().add(ElementModifier.INTERFACE);
        }
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            member.getModifiers().add(ElementModifier.NATIVE);
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            member.getModifiers().add(ElementModifier.STATIC);
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            member.getModifiers().add(ElementModifier.STRICT);
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            if (type == DECL_METHOD) {
                member.getModifiers().add(ElementModifier.SYNCHRONIZED);
            }
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            member.getModifiers().add(ElementModifier.SYNTHETIC);
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            if (type == DECL_FIELD) {
                member.getModifiers().add(ElementModifier.TRANSIENT);
            }
        }
        if ((access & Opcodes.ACC_VARARGS) != 0) {
            if (type == DECL_FIELD) {
                member.getModifiers().add(ElementModifier.VARARGS);
            }
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            if (type == DECL_FIELD) {
                member.getModifiers().add(ElementModifier.VOLATILE);
            }
        }
    }

    private void parseAnnotations(AnnotationContainer annotations, List<AnnotationNode> visibleAnnotations,
            List<AnnotationNode> invisibleAnnotations) {
        List<Object> annotNodes = new ArrayList<>();
        if (visibleAnnotations != null) {
            annotNodes.addAll(visibleAnnotations);
        }
        if (invisibleAnnotations != null) {
            annotNodes.addAll(invisibleAnnotations);
        }
        for (Object obj : annotNodes) {
            AnnotationNode annotNode = (AnnotationNode) obj;
            String desc = annotNode.desc;
            if (desc.startsWith("L") && desc.endsWith(";")) {
                desc = desc.substring(1, desc.length() - 1);
            }
            desc = desc.replace('/', '.');

            if (annotations.get(desc) != null) {
                continue;
            }

            AnnotationHolder annot = new AnnotationHolder(referenceCache.getCached(desc));
            parseAnnotationValues(annot, annotNode.values);
            annotations.add(annot);
        }
    }

    private void parseAnnotationValues(AnnotationHolder annot, List<Object> values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.size(); i += 2) {
            String key = (String) values.get(i);
            Object value = values.get(i + 1);
            annot.getValues().put(key, parseAnnotationValue(value));
        }
    }

    private AnnotationValue parseAnnotationValue(Object value) {
        if (value instanceof String[]) {
            String[] enumInfo = (String[]) value;
            ValueType.Object object = (ValueType.Object) ValueType.parse(enumInfo[0]);
            return new AnnotationValue(referenceCache.getCached(new FieldReference(object.getClassName(),
                    enumInfo[1])));
        } else if (value instanceof Type) {
            Type cls = (Type) value;
            return new AnnotationValue(referenceCache.getCached(ValueType.parse(cls.getDescriptor())));
        } else if (value instanceof List<?>) {
            List<?> originalList = (List<?>) value;
            List<AnnotationValue> resultList = new ArrayList<>();
            for (Object item : originalList) {
                resultList.add(parseAnnotationValue(item));
            }
            return new AnnotationValue(resultList);
        } else if (value instanceof AnnotationNode) {
            AnnotationNode annotNode = (AnnotationNode) value;
            ValueType.Object object = (ValueType.Object) referenceCache.getCached(ValueType.parse(annotNode.desc));
            AnnotationHolder annotation = new AnnotationHolder(object.getClassName());
            parseAnnotationValues(annotation, annotNode.values);
            return new AnnotationValue(annotation);
        } else if (value instanceof String) {
            return new AnnotationValue((String) value);
        } else if (value instanceof Boolean) {
            return new AnnotationValue((Boolean) value);
        } else if (value instanceof Character) {
            return new AnnotationValue((Character) value);
        } else if (value instanceof Byte) {
            return new AnnotationValue((Byte) value);
        } else if (value instanceof Short) {
            return new AnnotationValue((Short) value);
        } else if (value instanceof Integer) {
            return new AnnotationValue((Integer) value);
        } else if (value instanceof Long) {
            return new AnnotationValue((Long) value);
        } else if (value instanceof Float) {
            return new AnnotationValue((Float) value);
        } else if (value instanceof Double) {
            return new AnnotationValue((Double) value);
        } else if (value.getClass().isArray()) {
            List<AnnotationValue> resultList = new ArrayList<>();
            int size = Array.getLength(value);
            for (int i = 0; i < size; ++i) {
                Object item = Array.get(value, i);
                resultList.add(parseAnnotationValue(item));
            }
            return new AnnotationValue(resultList);
        } else {
            throw new AssertionError();
        }
    }
}
