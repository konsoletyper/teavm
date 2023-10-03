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
package org.teavm.model.lowlevel;

import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.runtime.ExceptionHandling;

public final class ExceptionHandlingUtil {
    private static final MethodReference FILL_STACK_TRACE = new MethodReference(ExceptionHandling.class,
            "fillStackTrace", StackTraceElement[].class);

    private ExceptionHandlingUtil() {
    }

    public static boolean isCallInstruction(Characteristics characteristics, Instruction insn) {
        if (insn instanceof InitClassInstruction || insn instanceof ConstructInstruction
                || insn instanceof ConstructArrayInstruction || insn instanceof ConstructMultiArrayInstruction
                || insn instanceof CloneArrayInstruction || insn instanceof RaiseInstruction
                || insn instanceof MonitorEnterInstruction || insn instanceof MonitorExitInstruction
                || insn instanceof NullCheckInstruction || insn instanceof BoundCheckInstruction
                || insn instanceof CastInstruction) {
            return true;
        } else if (insn instanceof InvokeInstruction) {
            return isManagedMethodCall(characteristics, ((InvokeInstruction) insn).getMethod());
        }
        return false;
    }

    public static boolean isManagedMethodCall(Characteristics characteristics, MethodReference method) {
        if (characteristics.isManaged(method) || method.equals(FILL_STACK_TRACE)) {
            return true;
        }
        return method.getClassName().equals(ExceptionHandling.class.getName())
                && method.getName().startsWith("throw");
    }
}
