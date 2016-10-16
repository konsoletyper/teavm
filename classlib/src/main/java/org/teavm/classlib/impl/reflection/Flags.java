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
package org.teavm.classlib.impl.reflection;

public final class Flags {
    private Flags() {
    }

    public static final int ABSTRACT = 1;
    public static final int ANNOTATION = 2;
    public static final int BRIDGE = 4;
    public static final int DEPRECATED = 8;
    public static final int ENUM = 16;
    public static final int FINAL = 32;
    public static final int INTERFACE = 64;
    public static final int NATIVE = 128;
    public static final int STATIC = 256;
    public static final int STRICT = 512;
    public static final int SUPER = 1024;
    public static final int SYNCHRONIZED = 2048;
    public static final int SYNTHETIC = 4096;
    public static final int TRANSIENT = 8192;
    public static final int VARARGS = 16384;
    public static final int VOLATILE = 32768;

    public static final int PACKAGE_PRIVATE = 0;
    public static final int PRIVATE = 1;
    public static final int PROTECTED = 2;
    public static final int PUBLIC = 3;

    public static int getModifiers(int flags, int access) {
        int modifiers = 0;
        switch (access) {
            case PUBLIC:
                modifiers |= 1;
                break;
            case PRIVATE:
                modifiers |= 2;
                break;
            case PROTECTED:
                modifiers |= 4;
                break;
        }

        // static
        modifiers |= (flags >>> 5) & 8;

        // final
        modifiers |= (flags >>> 1) & 16;

        // synchronized
        modifiers |= (flags >>> 5) & 32;

        // volatile
        modifiers |= (flags >>> 9) & 64;

        // transient
        modifiers |= (flags >>> 6) & 128;

        // native
        modifiers |= (flags << 1) & 256;

        // interface
        modifiers |= (flags << 3) & 512;

        // abstract
        modifiers |= (flags << 10) & 1024;

        // strict
        modifiers |= (flags << 2) & 2048;

        return modifiers;
    }
}
