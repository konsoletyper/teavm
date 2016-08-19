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
package org.teavm.wasm.patches;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.util.ProgramUtils;
import org.teavm.runtime.RuntimeJavaObject;
import org.teavm.runtime.RuntimeObject;

public class ObjectPatch implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (!cls.getName().equals("java.lang.Object")) {
            return;
        }

        MethodHolder method = cls.getMethod(new MethodDescriptor("identity", int.class));
        Program patchedProgram = ProgramUtils.copy(innerSource.get(ObjectPatch.class.getName())
                .getMethod(new MethodDescriptor("patchedIdentity", int.class)).getProgram());
        method.setProgram(patchedProgram);
    }

    @SuppressWarnings("unused")
    private int patchedIdentity() {
        RuntimeJavaObject object = Address.ofObject(this).toStructure();
        if ((object.classReference & RuntimeObject.MONITOR_EXISTS) != 0) {
            object = (RuntimeJavaObject) object.monitor;
        }
        int result = object.monitor.toAddress().toInt();
        if (result == 0) {
            result = RuntimeJavaObject.nextId++;
            if (result == 0) {
                result = RuntimeJavaObject.nextId++;
            }
            object.monitor = Address.fromInt(result).toStructure();
        }
        return result;
    }
}
