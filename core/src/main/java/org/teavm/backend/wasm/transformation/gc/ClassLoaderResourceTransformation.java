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

import java.io.InputStream;
import org.teavm.backend.wasm.runtime.gc.WasmGCResources;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

public class ClassLoaderResourceTransformation implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getName().equals(ClassLoader.class.getName())) {
            var method = cls.getMethod(new MethodDescriptor("getResourceAsStream", String.class, InputStream.class));
            transformGetResourceAsStream(method);
        }
    }

    private void transformGetResourceAsStream(MethodHolder method) {
        var program = new Program();
        program.createVariable();
        var nameVar = program.createVariable();
        var block = program.createBasicBlock();

        var invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(new MethodReference(WasmGCResources.class, "getResource", String.class,
                InputStream.class));
        invoke.setArguments(nameVar);
        invoke.setReceiver(program.createVariable());
        block.add(invoke);

        var exit = new ExitInstruction();
        exit.setValueToReturn(invoke.getReceiver());
        block.add(exit);

        method.setProgram(program);
    }
}
