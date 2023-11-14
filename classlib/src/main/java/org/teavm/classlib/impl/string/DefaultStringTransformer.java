/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.classlib.impl.string;

import java.util.List;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class DefaultStringTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals("java.lang.String")) {
            transformString(cls);
        }
    }

    private void transformString(ClassHolder cls) {
        var fields = List.copyOf(cls.getFields());
        for (var field : fields) {
            cls.removeField(field);
        }

        var charactersField = new FieldHolder("characters");
        charactersField.setType(ValueType.arrayOf(ValueType.CHARACTER));
        charactersField.setLevel(AccessLevel.PRIVATE);
        cls.addField(charactersField);

        for (var field : fields) {
            cls.addField(field);
        }

        for (var method : cls.getMethods()) {
            if (method.getProgram() != null) {
                transformProgram(method.getProgram());
            }
        }
    }

    private void transformProgram(Program program) {
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }
                var invoke = (InvokeInstruction) instruction;
                if (!invoke.getMethod().getClassName().equals("java.lang.String")) {
                    continue;
                }
                switch (invoke.getMethod().getName()) {
                    case "initWithEmptyChars":
                        replaceInitWithEmptyChars(invoke);
                        break;
                    case "borrowChars":
                        replaceBorrowChars(invoke);
                        break;
                    case "initWithCharArray":
                        replaceInitWithCharArray(invoke);
                        break;
                    case "takeCharArray":
                        replaceTakeCharArray(invoke);
                        break;
                    case "charactersLength":
                        replaceCharactersLength(invoke);
                        break;
                    case "charactersGet":
                        replaceCharactersGet(invoke);
                        break;
                    case "copyCharsToArray":
                        replaceCopyCharsToArray(invoke);
                        break;
                    case "fastCharArray":
                        replaceFastCharArray(invoke);
                        break;
                }
            }
        }
    }

    private void replaceInitWithEmptyChars(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "EMPTY_CHARS"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setReceiver(program.createVariable());
        getField.setLocation(invoke.getLocation());
        invoke.insertNext(getField);

        var putField = new PutFieldInstruction();
        putField.setField(new FieldReference("java.lang.String", "characters"));
        putField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        putField.setInstance(invoke.getInstance());
        putField.setValue(getField.getReceiver());
        putField.setLocation(invoke.getLocation());
        getField.insertNext(putField);

        invoke.delete();
    }

    private void replaceBorrowChars(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "characters"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setInstance(invoke.getArguments().get(0));
        getField.setReceiver(program.createVariable());
        getField.setLocation(invoke.getLocation());
        invoke.insertNext(getField);

        var putField = new PutFieldInstruction();
        putField.setField(new FieldReference("java.lang.String", "characters"));
        putField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        putField.setInstance(invoke.getInstance());
        putField.setValue(getField.getReceiver());
        putField.setLocation(invoke.getLocation());
        getField.insertNext(putField);

        invoke.delete();
    }

    private void replaceInitWithCharArray(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var createArray = new ConstructArrayInstruction();
        createArray.setItemType(ValueType.CHARACTER);
        createArray.setSize(invoke.getArguments().get(2));
        createArray.setReceiver(program.createVariable());
        createArray.setLocation(invoke.getLocation());
        invoke.insertNext(createArray);

        var zero = new IntegerConstantInstruction();
        zero.setReceiver(program.createVariable());
        zero.setLocation(invoke.getLocation());
        createArray.insertNext(zero);

        var arrayCopy = new InvokeInstruction();
        arrayCopy.setType(InvocationType.SPECIAL);
        arrayCopy.setMethod(new MethodReference(System.class, "arraycopy", Object.class, int.class,
                Object.class, int.class, int.class, void.class));
        arrayCopy.setArguments(invoke.getArguments().get(0), invoke.getArguments().get(1),
                createArray.getReceiver(), zero.getReceiver(), invoke.getArguments().get(2));
        zero.insertNext(arrayCopy);

        var putField = new PutFieldInstruction();
        putField.setField(new FieldReference("java.lang.String", "characters"));
        putField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        putField.setInstance(program.variableAt(0));
        putField.setValue(createArray.getReceiver());
        putField.setLocation(invoke.getLocation());
        arrayCopy.insertNext(putField);

        invoke.delete();
    }

    private void replaceTakeCharArray(InvokeInstruction invoke) {
        var putField = new PutFieldInstruction();
        putField.setField(new FieldReference("java.lang.String", "characters"));
        putField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        putField.setInstance(invoke.getInstance());
        putField.setValue(invoke.getArguments().get(0));
        putField.setLocation(invoke.getLocation());
        invoke.replace(putField);
    }

    private void replaceCharactersLength(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "characters"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setInstance(invoke.getInstance());
        getField.setReceiver(program.createVariable());
        getField.setLocation(invoke.getLocation());
        invoke.insertNext(getField);

        var unwrapArray = new UnwrapArrayInstruction(ArrayElementType.CHAR);
        unwrapArray.setArray(getField.getReceiver());
        unwrapArray.setReceiver(program.createVariable());
        unwrapArray.setLocation(invoke.getLocation());
        getField.insertNext(unwrapArray);

        var getLength = new ArrayLengthInstruction();
        getLength.setArray(unwrapArray.getReceiver());
        getLength.setReceiver(invoke.getReceiver());
        getLength.setLocation(invoke.getLocation());
        unwrapArray.insertNext(getLength);

        invoke.delete();
    }

    private void replaceCharactersGet(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "characters"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setInstance(invoke.getInstance());
        getField.setReceiver(program.createVariable());
        getField.setLocation(invoke.getLocation());
        invoke.insertNext(getField);

        var unwrapArray = new UnwrapArrayInstruction(ArrayElementType.CHAR);
        unwrapArray.setArray(getField.getReceiver());
        unwrapArray.setReceiver(program.createVariable());
        unwrapArray.setLocation(invoke.getLocation());
        getField.insertNext(unwrapArray);

        var getFromArray = new GetElementInstruction(ArrayElementType.CHAR);
        getFromArray.setArray(unwrapArray.getReceiver());
        getFromArray.setReceiver(invoke.getReceiver());
        getFromArray.setIndex(invoke.getArguments().get(0));
        getFromArray.setLocation(invoke.getLocation());
        unwrapArray.insertNext(getFromArray);

        invoke.delete();
    }

    private void replaceCopyCharsToArray(InvokeInstruction invoke) {
        var program = invoke.getProgram();

        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "characters"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setInstance(invoke.getInstance());
        getField.setReceiver(program.createVariable());
        getField.setLocation(invoke.getLocation());
        invoke.insertNext(getField);

        var arrayCopy = new InvokeInstruction();
        arrayCopy.setType(InvocationType.SPECIAL);
        arrayCopy.setMethod(new MethodReference(System.class, "arraycopy", Object.class, int.class,
                Object.class, int.class, int.class, void.class));
        arrayCopy.setArguments(getField.getReceiver(), invoke.getArguments().get(0), invoke.getArguments().get(1),
                invoke.getArguments().get(2), invoke.getArguments().get(3));
        getField.insertNext(arrayCopy);

        invoke.delete();
    }

    private void replaceFastCharArray(InvokeInstruction invoke) {
        var getField = new GetFieldInstruction();
        getField.setField(new FieldReference("java.lang.String", "characters"));
        getField.setFieldType(ValueType.arrayOf(ValueType.CHARACTER));
        getField.setInstance(invoke.getInstance());
        getField.setReceiver(invoke.getReceiver());
        getField.setLocation(invoke.getLocation());
        invoke.replace(getField);
    }
}
