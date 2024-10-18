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
package org.teavm.backend.wasm.transformation.gc;

import java.lang.ref.ReferenceQueue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;

public class ReferenceQueueTransformation implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(ReferenceQueue.class.getName())) {
           transformReferenceQueue(cls, context);
        }
    }

    private void transformReferenceQueue(ClassHolder cls, ClassHolderTransformerContext context) {
        var templateClass = context.getHierarchy().getClassSource().get(ReferenceQueueTemplate.class.getName());
        for (var method : templateClass.getMethods()) {
            if (!method.getName().equals("<init>")) {
                copyMethod(cls, method);
            }
        }
        for (var field : templateClass.getFields()) {
            cls.addField(ModelUtils.copyField(field));
        }
    }

    private void copyMethod(ClassHolder cls, MethodReader method) {
        var targetMethod = cls.getMethod(method.getDescriptor());
        if (targetMethod == null) {
            targetMethod = new MethodHolder(method.getDescriptor());
            cls.addMethod(targetMethod);
            targetMethod.getModifiers().addAll(method.readModifiers());
            targetMethod.setLevel(method.getLevel());
        }

        var targetProgram = ProgramUtils.copy(method.getProgram());
        targetMethod.setProgram(targetProgram);
        for (var block : targetProgram.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof GetFieldInstruction) {
                    var getField = (GetFieldInstruction) instruction;
                    getField.setField(mapField(getField.getField()));
                } else if (instruction instanceof PutFieldInstruction) {
                    var putField = (PutFieldInstruction) instruction;
                    putField.setField(mapField(putField.getField()));
                }
            }
        }
    }

    private FieldReference mapField(FieldReference field) {
        if (field.getClassName().equals(ReferenceQueueTemplate.class.getName())) {
            return new FieldReference(ReferenceQueue.class.getName(), field.getFieldName());
        }
        return field;
    }
}
