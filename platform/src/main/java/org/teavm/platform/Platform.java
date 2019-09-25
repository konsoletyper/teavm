/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform;

import java.lang.annotation.Annotation;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.PlatformMarker;
import org.teavm.interop.Platforms;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;
import org.teavm.platform.metadata.StaticFieldResource;
import org.teavm.platform.plugin.PlatformGenerator;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public final class Platform {
    private Platform() {
    }

    private static boolean newInstancePrepared;

    @InjectedBy(PlatformGenerator.class)
    @Unmanaged
    @NoSideEffects
    public static native PlatformObject getPlatformObject(Object obj);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    @NoSideEffects
    public static native Object clone(Object obj);

    @DelegateTo("isInstanceLowLevel")
    public static boolean isInstance(PlatformObject obj, PlatformClass cls) {
        return obj != null && !isUndefined(obj.getPlatformClass().getMetadata())
                && isAssignable(obj.getPlatformClass(), cls);
    }

    @SuppressWarnings("unused")
    @Unmanaged
    private static boolean isInstanceLowLevel(RuntimeClass self, RuntimeObject object) {
        return isAssignableLowLevel(RuntimeClass.getClass(object), self);
    }

    @JSBody(params = "object", script = "return typeof object === 'undefined';")
    @NoSideEffects
    private static native boolean isUndefined(JSObject object);

    @DelegateTo("isAssignableLowLevel")
    public static boolean isAssignable(PlatformClass from, PlatformClass to) {
        if (from == to) {
            return true;
        }
        PlatformSequence<PlatformClass> supertypes = from.getMetadata().getSupertypes();
        for (int i = 0; i < supertypes.getLength(); ++i) {
            if (isAssignable(supertypes.get(i), to)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Unmanaged
    private static boolean isAssignableLowLevel(RuntimeClass from, RuntimeClass to) {
        return to.isSupertypeOf.apply(from);
    }

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    @Unmanaged
    @NoSideEffects
    public static native Class<?> asJavaClass(PlatformObject obj);

    @JSBody(script = "return $rt_nextId();")
    @NoSideEffects
    public static native int nextObjectId();

    public static <T> T newInstance(PlatformClass cls) {
        if (!newInstancePrepared) {
            prepareNewInstance();
            newInstancePrepared = true;
        }
        return newInstanceImpl(cls);
    }

    @GeneratedBy(PlatformGenerator.class)
    @NoSideEffects
    private static native void prepareNewInstance();

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    private static native <T> T newInstanceImpl(PlatformClass cls);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    @NoSideEffects
    public static native PlatformClass lookupClass(String name);

    @PluggableDependency(PlatformGenerator.class)
    @InjectedBy(PlatformGenerator.class)
    @DelegateTo("initClassLowLevel")
    public static native void initClass(PlatformClass cls);

    @Unmanaged
    private static void initClassLowLevel(RuntimeClass cls) {
        if (cls.init != null) {
            cls.init.run();
        }
    }

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Object objectFromResource(StaticFieldResource resource);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    @DelegateTo("getEnumConstantsLowLevel")
    @NoSideEffects
    public static native Enum<?>[] getEnumConstants(PlatformClass cls);

    private static Enum<?>[] getEnumConstantsLowLevel(PlatformClass cls) {
        int size = getEnumConstantsSize(cls);
        if (size < 0) {
            return null;
        }

        Enum<?>[] constants = new Enum<?>[size];
        fillEnumConstants(cls, constants);
        return constants;
    }

    @DelegateTo("getEnumConstantsSizeImpl")
    private static native int getEnumConstantsSize(PlatformClass cls);

    @Unmanaged
    private static int getEnumConstantsSizeImpl(RuntimeClass cls) {
        Address enumValues = cls.enumValues;
        if (enumValues == null) {
            return -1;
        }
        return enumValues.getAddress().toInt();
    }

    @DelegateTo("fillEnumConstantsImpl")
    private static native void fillEnumConstants(PlatformClass cls, Enum<?>[] array);

    @Unmanaged
    private static void fillEnumConstantsImpl(RuntimeClass cls, Address[] array) {
        Address enumValues = cls.enumValues;
        for (int i = 0; i < array.length; i++) {
            enumValues = enumValues.add(Address.sizeOf());
            array[i] = enumValues.getAddress().getAddress();
        }
    }

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Annotation[] getAnnotations(PlatformClass cls);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native void startThread(PlatformRunnable runnable);

    private static void launchThread(PlatformRunnable runnable) {
        runnable.run();
    }

    public static void postpone(PlatformRunnable runnable) {
        schedule(runnable, 0);
    }

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native int schedule(PlatformRunnable runnable, int timeout);

    public static void killSchedule(int id) {
        Window.clearTimeout(id);
    }

    public static <T> PlatformQueue<T> createQueue() {
        if (isLowLevel()) {
            return new LowLevelQueue<>();
        } else {
            return createQueueJs();
        }
    }

    @JSBody(script = "return [];")
    @NoSideEffects
    private static native  <T> PlatformQueue<T> createQueueJs();

    public static PlatformString stringFromCharCode(int charCode) {
        return JSString.fromCharCode(charCode).cast();
    }

    @DelegateTo("isPrimitiveLowLevel")
    public static boolean isPrimitive(PlatformClass cls) {
        return cls.getMetadata().isPrimitive();
    }

    @SuppressWarnings("unused")
    @Unmanaged
    private static boolean isPrimitiveLowLevel(RuntimeClass cls) {
        return (cls.flags & RuntimeClass.PRIMITIVE) != 0;
    }

    @DelegateTo("isEnumLowLevel")
    public static boolean isEnum(PlatformClass cls) {
        return cls.getMetadata().isEnum();
    }

    @Unmanaged
    private static boolean isEnumLowLevel(RuntimeClass cls) {
        return (cls.flags & RuntimeClass.ENUM) != 0;
    }

    @Unmanaged
    public static PlatformClass getArrayItem(PlatformClass cls) {
        return cls.getMetadata().getArrayItem();
    }

    @Unmanaged
    @PluggableDependency(PlatformGenerator.class)
    public static String getName(PlatformClass cls) {
        return cls.getMetadata().getName();
    }

    @PlatformMarker(Platforms.LOW_LEVEL)
    private static boolean isLowLevel() {
        return false;
    }
}
