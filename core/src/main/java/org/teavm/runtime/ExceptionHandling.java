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
import org.teavm.interop.Export;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@StaticInit
public final class ExceptionHandling {
    private ExceptionHandling() {
    }

    @Unmanaged
    public static native CallSite findCallSiteById(int id, Address frame);

    @Unmanaged
    public static native boolean isJumpSupported();

    @Unmanaged
    public static native void jumpToFrame(Address frame, int id);

    @Unmanaged
    public static native void abort();

    @Unmanaged
    public static void printStack() {
        Address stackFrame = ShadowStack.getStackTop();
        while (stackFrame != null) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            CallSite callSite = findCallSiteById(callSiteId, stackFrame);
            CallSiteLocation location = callSite.location;
            MethodLocation methodLocation = location != null ? location.method : null;

            Console.printString("    at ");
            if (methodLocation.className == null || methodLocation.methodName == null) {
                Console.printString("(Unknown method)");
            } else {
                Console.printString(methodLocation.className.value);
                Console.printString(".");
                Console.printString(methodLocation.methodName.value);
            }
            Console.printString("(");
            if (methodLocation.fileName != null && location.lineNumber >= 0) {
                Console.printString(methodLocation.fileName.value);
                Console.printString(":");
                Console.printInt(location.lineNumber);
            }
            Console.printString(")\n");
            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        }
    }

    private static Throwable thrownException;

    @Export(name = "teavm_catchException")
    @Unmanaged
    public static Throwable catchException() {
        Throwable exception = thrownException;
        thrownException = null;
        return exception;
    }

    @Unmanaged
    public static void throwException(Throwable exception) {
        thrownException = exception;

        RuntimeObject exceptionPtr = Address.ofObject(exception).toStructure();
        RuntimeClass exceptionClass = RuntimeClass.getClass(exceptionPtr);

        Address stackFrame = ShadowStack.getStackTop();
        int handlerId = 0;
        stackLoop: while (stackFrame != null) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            CallSite callSite = findCallSiteById(callSiteId, stackFrame);
            ExceptionHandler handler = callSite.firstHandler;

            while (handler != null) {
                if (handler.exceptionClass == null || handler.exceptionClass.isSupertypeOf.apply(exceptionClass)) {
                    handlerId = handler.id;
                    ShadowStack.setExceptionHandlerId(stackFrame, handler.id);
                    break stackLoop;
                }
                handler = handler.next;
            }

            ShadowStack.setExceptionHandlerId(stackFrame, callSiteId - 1);
            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        }

        if (stackFrame == null) {
            stackFrame = ShadowStack.getStackTop();
            while (stackFrame != null) {
                ShadowStack.setExceptionHandlerId(stackFrame, ShadowStack.getCallSiteId(stackFrame) + 1);
                stackFrame = ShadowStack.getNextStackFrame(stackFrame);
            }
            printStack();
            abort();
        } else if (isJumpSupported()) {
            jumpToFrame(stackFrame, handlerId);
        }
    }

    @Unmanaged
    public static void throwClassCastException() {
        throw new ClassCastException();
    }

    @Unmanaged
    @Export(name = "teavm_throwNullPointerException")
    public static void throwNullPointerException() {
        throw new NullPointerException();
    }

    @Unmanaged
    @Export(name = "teavm_throwArrayIndexOutOfBoundsException")
    public static void throwArrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Unmanaged
    public static int callStackSize() {
        Address stackFrame = ShadowStack.getStackTop();
        int size = 0;
        while (stackFrame != null) {
            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
            size++;
        }
        return size + 1;
    }

    public static void fillStackTrace(StackTraceElement[] target) {
        Address stackFrame = ShadowStack.getStackTop();
        stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        int index = 0;
        while (stackFrame != null && index < target.length) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            CallSite callSite = findCallSiteById(callSiteId, stackFrame);
            CallSiteLocation location = callSite.location;
            MethodLocation methodLocation = location != null ? location.method : null;
            StackTraceElement element = createElement(
                    methodLocation != null && methodLocation.className != null ? methodLocation.className.value : "",
                    methodLocation != null && methodLocation.methodName != null ? methodLocation.methodName.value : "",
                    methodLocation != null && methodLocation.fileName != null ? methodLocation.fileName.value : null,
                    location != null ? location.lineNumber : -1);
            target[index++] = element;
            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        }
    }

    private static StackTraceElement createElement(String className, String methodName, String fileName,
            int lineNumber) {
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }
}
