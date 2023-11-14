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
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
public final class ShadowStack {
    private ShadowStack() {
    }

    public static native void allocStack(int size);

    public static native void registerGCRoot(int index, Object object);

    public static native void removeGCRoot(int index);

    public static native void releaseStack(int size);

    public static native Address getStackTop();

    public static native Address getNextStackFrame(Address stackFrame);

    public static native int getStackRootCount(Address stackFrame);

    public static native Address getStackRootPointer(Address stackFrame);

    public static native int getCallSiteId(Address stackFrame);

    public static native void registerCallSite(int id);

    public static native int getExceptionHandlerId();

    public static native void setExceptionHandlerId(Address stackFrame, int id);

    public static native void setExceptionHandlerSkip(Address stackFrame);

    public static native void setExceptionHandlerRestore(Address stackFrame);
}
