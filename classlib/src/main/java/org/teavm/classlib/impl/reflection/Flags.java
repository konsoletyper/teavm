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
    public static final int INTERFACE = 2;
    public static final int FINAL = 4;
    public static final int ENUM = 8;
    public static final int ANNOTATION = 16;
    public static final int SYNTHETIC = 32;
    public static final int BRIDGE = 64;
    public static final int DEPRECATED = 128;
    public static final int NATIVE = 256;
    public static final int STATIC = 512;
    public static final int STRICT = 1024;
    public static final int SYNCHRONIZED = 2048;
    public static final int TRANSIENT = 4096;
    public static final int VARARGS = 8192;
    public static final int VOLATILE = 16384;

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
        modifiers |= (flags >>> 6) & 8;

        // final
        modifiers |= (flags << 2) & 16;

        // synchronized
        modifiers |= (flags >>> 6) & 32;

        // volatile
        modifiers |= (flags >>> 8) & 64;

        // transient
        modifiers |= (flags >>> 5) & 128;

        // native
        modifiers |= flags & 256;

        // interface
        modifiers |= (flags << 8) & 512;

        // abstract
        modifiers |= (flags << 10) & 1024;

        // strict
        modifiers |= (flags << 1) & 2048;

        return modifiers;
    }
}
