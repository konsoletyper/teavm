/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.platform.plugin.wasmgc;

import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceMap;

public class ResourceInterfaceToClassTransformer implements ClassHolderTransformer {
    private static final String RESOURCE_CLASS = Resource.class.getName();

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (context.getHierarchy().isSuperType(RESOURCE_CLASS, cls.getName(), false)
                && cls.hasModifier(ElementModifier.INTERFACE)) {
            cls.getModifiers().remove(ElementModifier.INTERFACE);
            cls.getModifiers().add(ElementModifier.ABSTRACT);

            int index = 0;
            if (!cls.getInterfaces().isEmpty()) {
                var parent = cls.getInterfaces().iterator().next();
                cls.getInterfaces().clear();
                cls.setParent(parent);
                var parentCls = context.getHierarchy().getClassSource().get(parent);
                if (parentCls != null) {
                    var resourceAnnot = parentCls.getAnnotations().get(ResourceMarker.class.getName());
                    if (resourceAnnot != null) {
                        index = resourceAnnot.getValue("fieldCount").getInt();
                    }
                }
            }

            for (var method : cls.getMethods()) {
                method.getModifiers().remove(ElementModifier.ABSTRACT);
                method.getModifiers().add(ElementModifier.NATIVE);
                if (method.getName().startsWith("get") && method.getName().length() > 3
                        && Character.isUpperCase(method.getName().charAt(3))
                        && method.getResultType() != ValueType.VOID) {
                    var fieldName = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                    var annot = new AnnotationHolder(FieldMarker.class.getName());
                    annot.getValues().put("value", new AnnotationValue(fieldName));
                    annot.getValues().put("index", new AnnotationValue(index++));
                    method.getAnnotations().add(annot);
                } else if (method.getName().startsWith("is") && method.getName().length() > 2
                        && Character.isUpperCase(method.getName().charAt(2))
                        && method.getResultType() == ValueType.BOOLEAN) {
                    var fieldName = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
                    var annot = new AnnotationHolder(FieldMarker.class.getName());
                    annot.getValues().put("value", new AnnotationValue(fieldName));
                    annot.getValues().put("index", new AnnotationValue(index++));
                    method.getAnnotations().add(annot);
                }
            }

            if (cls.getName().equals(ResourceMap.class.getName())) {
                transformResourceMapMethods(cls, context.getHierarchy());
            }

            var resourceAnnot = new AnnotationHolder(ResourceMarker.class.getName());
            resourceAnnot.getValues().put("fieldCount", new AnnotationValue(index));
            cls.getAnnotations().add(resourceAnnot);
        } else {
            for (var method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    transformResourceUsages(method.getProgram(), context);
                }
            }
        }
    }

    private void transformResourceMapMethods(ClassHolder cls, ClassHierarchy hierarchy) {
        for (var method : cls.getMethods()) {
            switch (method.getName()) {
                case "has": {
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    var pe = ProgramEmitter.create(method, hierarchy);
                    pe.invoke(ResourceMapHelper.class, "has", boolean.class, pe.var(0, ResourceMap.class),
                            pe.var(1, String.class)).returnValue();
                    break;
                }
                case "get": {
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    var pe = ProgramEmitter.create(method, hierarchy);
                    pe.invoke(ResourceMapHelper.class, "get", Resource.class, pe.var(0, ResourceMap.class),
                            pe.var(1, String.class)).returnValue();
                    break;
                }
                case "keys": {
                    method.getModifiers().remove(ElementModifier.NATIVE);
                    var pe = ProgramEmitter.create(method, hierarchy);
                    pe.invoke(ResourceMapHelper.class, "keys", String[].class, pe.var(0, ResourceMap.class))
                            .returnValue();
                    break;
                }
            }
        }
    }

    private void transformResourceUsages(Program program, ClassHolderTransformerContext context) {
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof CastInstruction) {
                    var cast = (CastInstruction) instruction;
                    if (cast.isWeak()) {
                        continue;
                    }
                    if (!(cast.getTargetType() instanceof ValueType.Object)) {
                        continue;
                    }
                    var className = ((ValueType.Object) cast.getTargetType()).getClassName();
                    if (context.getHierarchy().isSuperType(RESOURCE_CLASS, className, false)) {
                        cast.setWeak(true);
                    }
                } else if (instruction instanceof InvokeInstruction) {
                    var invoke = (InvokeInstruction) instruction;
                    if (invoke.getInstance() == null || invoke.getType() == InvocationType.SPECIAL) {
                        continue;
                    }
                    if (!context.getHierarchy().isSuperType(RESOURCE_CLASS, invoke.getMethod().getClassName(),
                            false)) {
                        continue;
                    }
                    invoke.setType(InvocationType.SPECIAL);
                }
            }
        }
    }
}
