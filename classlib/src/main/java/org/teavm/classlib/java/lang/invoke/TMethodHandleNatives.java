/*
 *  Copyright 2020 adam.
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
package org.teavm.classlib.java.lang.invoke;

import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_LIMIT;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_NONE;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_getField;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_getStatic;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_invokeInterface;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_invokeSpecial;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_invokeStatic;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_invokeVirtual;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_newInvokeSpecial;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_putField;
import static org.teavm.classlib.java.lang.invoke.TMethodHandleNatives.Constants.REF_putStatic;

class TMethodHandleNatives {
    static class Constants {
        Constants() { } // static only

        static final byte REF_NONE                    = 0;
        static final byte REF_getField                = 1;
        static final byte REF_getStatic               = 2;
        static final byte REF_putField                = 3;
        static final byte REF_putStatic               = 4;
        static final byte REF_invokeVirtual           = 5;
        static final byte REF_invokeStatic            = 6;
        static final byte REF_invokeSpecial           = 7;
        static final byte REF_newInvokeSpecial        = 8;
        static final byte REF_invokeInterface         = 9;
        static final byte REF_LIMIT                  = 10;
    }

    static boolean refKindIsValid(int refKind) {
        return refKind > REF_NONE && refKind < REF_LIMIT;
    }
    static String refKindName(byte refKind) {
        assert refKindIsValid(refKind);
        switch (refKind) {
        case REF_getField:          return "getField";
        case REF_getStatic:         return "getStatic";
        case REF_putField:          return "putField";
        case REF_putStatic:         return "putStatic";
        case REF_invokeVirtual:     return "invokeVirtual";
        case REF_invokeStatic:      return "invokeStatic";
        case REF_invokeSpecial:     return "invokeSpecial";
        case REF_newInvokeSpecial:  return "newInvokeSpecial";
        case REF_invokeInterface:   return "invokeInterface";
        default:                    return "REF_???";
        }
    }

    private TMethodHandleNatives() {
    }
}
