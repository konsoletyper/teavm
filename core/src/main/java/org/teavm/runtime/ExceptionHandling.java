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
    private static native boolean isObfuscated();

    @Unmanaged
    public static void printStack() {
        Address stackFrame = ShadowStack.getStackTop();
        while (stackFrame != null) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            if (isObfuscated()) {
                Console.printString("\tat Obfuscated.obfuscated(Obfuscated.java:");
                Console.printInt(callSiteId);
                Console.printString(")\n");
            } else {
                CallSite callSite = findCallSiteById(callSiteId, stackFrame);
                CallSiteLocation location = callSite.location;
                while (location != null) {
                    MethodLocation methodLocation = location.method;

                    if (methodLocation != null) {
                        Console.printString("\tat ");
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
                    }

                    location = location.next;
                }
            }
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
            if (callSiteId >= 0) {
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
            }
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
    private static int callStackSize() {
        Address stackFrame = ShadowStack.getStackTop();
        int size = 0;
        while (stackFrame != null) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            if (callSiteId >= 0) {
                CallSite callSite = findCallSiteById(callSiteId, stackFrame);
                CallSiteLocation location = callSite.location;
                if (isObfuscated() || location == null) {
                    size++;
                } else {
                    while (location != null) {
                        size++;
                        location = location.next;
                    }
                }
            }

            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        }
        return size;
    }

    @Unmanaged
    public static StackTraceElement[] fillStackTrace() {
        Address stackFrame = ShadowStack.getStackTop();
        int size = callStackSize();

        ShadowStack.allocStack(1);
        ShadowStack.removeGCRoot(0);
        StackTraceElement[] target = new StackTraceElement[size];
        ShadowStack.registerGCRoot(0, target);

        int index = 0;
        while (stackFrame != null) {
            int callSiteId = ShadowStack.getCallSiteId(stackFrame);
            if (callSiteId >= 0) {
                CallSite callSite = findCallSiteById(callSiteId, stackFrame);
                CallSiteLocation location = callSite.location;
                if (isObfuscated()) {
                    target[index++] = new StackTraceElement("Obfuscated", "obfuscated", "Obfuscated.java", callSiteId);
                } else if (location == null) {
                    target[index++] = new StackTraceElement("", "", null, -1);
                } else {
                    while (location != null) {
                        MethodLocation methodLocation = location.method;
                        StackTraceElement element;
                        if (methodLocation != null) {
                            element = new StackTraceElement(
                                    methodLocation.className != null ? methodLocation.className.value : "",
                                    methodLocation.methodName != null ? methodLocation.methodName.value : "",
                                    methodLocation.fileName != null ? methodLocation.fileName.value : null,
                                    location.lineNumber);
                        } else {
                            element = new StackTraceElement("", "", null, location.lineNumber);
                        }
                        target[index++] = element;
                        location = location.next;
                    }
                }
            }
            stackFrame = ShadowStack.getNextStackFrame(stackFrame);
        }
        ShadowStack.releaseStack(1);

        return target;
    }
}
