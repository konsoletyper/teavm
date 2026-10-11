/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.vm;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;

public class WasmTestClassTransformer implements ClassHolderTransformer {
    static final int VIRTUAL_METHOD_COUNT = 1000;
    private static final int SUBCLASS_COUNT = 900;
    private static final int ANNOTATED_CLASS_COUNT = 400;
    private static final int ANNOTATION_VALUE_COUNT = 2000;
    private static final String GENERATED_PREFIX = WasmTest.class.getName() + "Generated";

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(WasmTest.class.getName())) {
            var method = cls.getMethod(new MethodDescriptor("tryCatchWithReturnInside", int.class));
            method.getModifiers().remove(ElementModifier.NATIVE);
            method.setProgram(createTryCatchWithReturnInside());
            generateManyVirtualMethods(cls, context);
            generateManyAnnotatedClasses(cls, context);
        }
    }
    
    private Program createTryCatchWithReturnInside() {
        var program = new Program();
        program.createVariable();
        
        var mainBlock = program.createBasicBlock();
        var thenBlock = program.createBasicBlock();
        var elseBlock = program.createBasicBlock();
        var call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setMethod(new MethodReference(WasmTest.class, "foo", int.class));
        call.setReceiver(program.createVariable());
        mainBlock.add(call);
        
        var branch = new BranchingInstruction(BranchingCondition.EQUAL);
        branch.setOperand(call.getReceiver());
        branch.setConsequent(thenBlock);
        branch.setAlternative(elseBlock);
        mainBlock.add(branch);
        
        var exit = new ExitInstruction();
        exit.setValueToReturn(call.getReceiver());
        thenBlock.add(exit);

        exit = new ExitInstruction();
        exit.setValueToReturn(call.getReceiver());
        elseBlock.add(exit);
        
        var catchBlock = program.createBasicBlock();
        var exceptionConstructor = new ConstructInstruction();
        exceptionConstructor.setType("java.lang.RuntimeException");
        exceptionConstructor.setReceiver(program.createVariable());
        catchBlock.add(exceptionConstructor);
        
        var initException = new InvokeInstruction();
        initException.setType(InvocationType.SPECIAL);
        initException.setMethod(new MethodReference(RuntimeException.class, "<init>", void.class));
        initException.setInstance(exceptionConstructor.getReceiver());
        catchBlock.add(initException);
        
        var throwException = new RaiseInstruction();
        throwException.setException(exceptionConstructor.getReceiver());
        catchBlock.add(throwException);
        
        var tryCatch = new TryCatchBlock();
        tryCatch.setHandler(catchBlock);
        tryCatch.setExceptionType("java.lang.Throwable");
        mainBlock.getTryCatchBlocks().add(tryCatch);
        thenBlock.getTryCatchBlocks().add(tryCatch);
        elseBlock.getTryCatchBlocks().add(tryCatch);
        
        return program;
    }

    // A base class with VIRTUAL_METHOD_COUNT methods and SUBCLASS_COUNT subclasses. The virtual table of each
    // subclass has VIRTUAL_METHOD_COUNT entries.
    private void generateManyVirtualMethods(ClassHolder testClass, ClassHolderTransformerContext context) {
        var baseName = GENERATED_PREFIX + "Base";
        var base = createClass(baseName, "java.lang.Object", context);
        for (var i = 0; i < VIRTUAL_METHOD_COUNT; ++i) {
            var method = new MethodHolder("m" + i, ValueType.INTEGER);
            method.setLevel(AccessLevel.PUBLIC);
            base.addMethod(method);
            ProgramEmitter.create(method, context.getHierarchy()).constant(i).returnValue();
        }
        context.submit(base);
        for (var i = 0; i < SUBCLASS_COUNT; ++i) {
            context.submit(createClass(baseName + i, baseName, context));
        }

        var method = testClass.getMethod(new MethodDescriptor("callManyVirtualMethods", int.class));
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var instances = pe.constructArray(ValueType.object(baseName), SUBCLASS_COUNT);
        for (var i = 0; i < SUBCLASS_COUNT; ++i) {
            instances.setElement(i, pe.construct(baseName + i));
        }
        var instance = instances.getElement(0);
        var result = pe.constant(0);
        for (var i = 0; i < VIRTUAL_METHOD_COUNT; ++i) {
            result = instance.invokeVirtual("m" + i, int.class);
        }
        result.returnValue();
    }

    // ANNOTATED_CLASS_COUNT classes, each with a runtime annotation that has ANNOTATION_VALUE_COUNT values.
    private void generateManyAnnotatedClasses(ClassHolder testClass, ClassHolderTransformerContext context) {
        var values = new ArrayList<AnnotationValue>();
        for (var i = 0; i < ANNOTATION_VALUE_COUNT; ++i) {
            values.add(new AnnotationValue(1_000_000_000 + i));
        }
        var className = GENERATED_PREFIX + "Annotated";
        for (var i = 0; i < ANNOTATED_CLASS_COUNT; ++i) {
            var cls = createClass(className + i, "java.lang.Object", context);
            var annotation = new AnnotationHolder(WasmTest.LargeAnnotation.class.getName());
            annotation.getValues().put("value", new AnnotationValue(values));
            cls.getAnnotations().add(annotation);
            context.submit(cls);
        }

        var method = testClass.getMethod(new MethodDescriptor("countAnnotationsOfGeneratedClass", int.class));
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var instances = pe.constructArray(Object.class, ANNOTATED_CLASS_COUNT);
        for (var i = 0; i < ANNOTATED_CLASS_COUNT; ++i) {
            instances.setElement(i, pe.construct(className + i));
        }
        instances.getElement(0)
                .invokeVirtual("getClass", Class.class)
                .invokeVirtual("getDeclaredAnnotations", Annotation[].class)
                .arrayLength()
                .returnValue();
    }

    private ClassHolder createClass(String name, String parent, ClassHolderTransformerContext context) {
        var cls = new ClassHolder(name);
        cls.setLevel(AccessLevel.PUBLIC);
        cls.setParent(parent);
        var constructor = new MethodHolder("<init>", ValueType.VOID);
        constructor.setLevel(AccessLevel.PUBLIC);
        cls.addMethod(constructor);
        var pe = ProgramEmitter.create(constructor, context.getHierarchy());
        pe.var(0, ValueType.object(name)).invokeSpecial(parent, "<init>");
        pe.exit();
        return cls;
    }
}
