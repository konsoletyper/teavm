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
package org.teavm.runtime;

import org.teavm.interop.Address;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
public final class ExceptionHandling {
    private ExceptionHandling() {
    }

    public static native void registerCallSite(int id);

    public static native int callSiteResult();

    public static native Address getStackTop();

    public static native Address getNextStackFrame(Address stackFrame);

    public static native int getCallSiteId(Address stackFrame);

    public static native void setHandlerId(Address stackFrame, int id);

    public static native int getHandlerId();

    public static native CallSite findCallSiteById(int id);

    public static void throwException(Throwable exception) {
        RuntimeObject exceptionPtr = Address.ofObject(exception).toStructure();
        RuntimeClass exceptionClass = RuntimeClass.getClass(exceptionPtr);
        IsSupertypeFunction isExceptionSupertype = exceptionClass.isSupertypeOf;

        Address stackFrame = getStackTop();
        stackLoop: while (stackFrame != null) {
            int callSiteId = getCallSiteId(stackFrame);
            CallSite callSite = findCallSiteById(callSiteId);
            ExceptionHandler handler = callSite.firstHandler;

            for (int i = 0; i < callSite.handlerCount; ++i) {
                if (isExceptionSupertype.apply(handler.exceptionClass)) {
                    setHandlerId(stackFrame, handler.id);
                    break stackLoop;
                }

                handler = Structure.add(ExceptionHandler.class, handler, 1);
            }

            setHandlerId(stackFrame, callSiteId - 1);
            stackFrame = getNextStackFrame(stackFrame);
        }
    }
}
