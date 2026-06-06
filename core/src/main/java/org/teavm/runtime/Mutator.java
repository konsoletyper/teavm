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

import org.teavm.backend.c.intrinsic.RuntimeInclude;
import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
public final class Mutator {
    private Mutator() {
    }

    public static native Address getStaticGCRoots();

    @RuntimeInclude("stringhash.h")
    @Import(name = "teavm_stringHashtableRewind")
    public static native void stringHashtableRewind();

    @RuntimeInclude("stringhash.h")
    @Import(name = "teavm_stringHashtableCurrent")
    public static native RuntimeObject stringHashtableCurrent();

    @RuntimeInclude("stringhash.h")
    @Import(name = "teavm_stringHashtableUpdateRef")
    public static native void stringHashtableUpdateRef(RuntimeObject str);

    @RuntimeInclude("stringhash.h")
    @Import(name = "teavm_stringHashtableDelete")
    public static native void stringHashtableDelete();

    @RuntimeInclude("stringhash.h")
    @Import(name = "teavm_stringHashtableNext")
    public static native void stringHashtableNext();
}
