/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.Arrays;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHandleType;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.InvocationType;

public final class InvokeDynamicUtil {
    private InvokeDynamicUtil() {
    }

    public static ValueEmitter invoke(ProgramEmitter pe, MethodHandle handle, ValueEmitter... arguments) {
        switch (handle.getKind()) {
            case GET_FIELD:
                return arguments[0].getField(handle.getName(), handle.getValueType());
            case GET_STATIC_FIELD:
                return pe.getField(handle.getClassName(), handle.getName(), handle.getValueType());
            case PUT_FIELD:
                arguments[0].setField(handle.getName(), arguments[0].cast(handle.getValueType()));
                return null;
            case PUT_STATIC_FIELD:
                pe.setField(handle.getClassName(), handle.getName(), arguments[0].cast(handle.getValueType()));
                return null;
            case INVOKE_VIRTUAL:
            case INVOKE_INTERFACE:
            case INVOKE_SPECIAL: {
                for (int i = 1; i < arguments.length; ++i) {
                    arguments[i] = arguments[i].cast(handle.getArgumentType(i - 1));
                }
                arguments[0] = arguments[0].cast(ValueType.object(handle.getClassName()));
                InvocationType type = handle.getKind() == MethodHandleType.INVOKE_SPECIAL
                        ? InvocationType.SPECIAL
                        : InvocationType.VIRTUAL;
                return arguments[0].invoke(type, handle.getName(), handle.getValueType(),
                        Arrays.copyOfRange(arguments, 1, arguments.length));
            }
            case INVOKE_STATIC:
                for (int i = 0; i < arguments.length; ++i) {
                    arguments[i] = arguments[i].cast(handle.getArgumentType(i));
                }
                return pe.invoke(handle.getClassName(), handle.getName(), handle.getValueType(), arguments);
            case INVOKE_CONSTRUCTOR:
                return pe.construct(handle.getClassName(), arguments);
            default:
                throw new IllegalArgumentException("Unexpected handle type: " + handle.getKind());
        }
    }
}
