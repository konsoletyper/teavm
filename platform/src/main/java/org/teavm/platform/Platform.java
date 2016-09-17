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
import org.teavm.interop.DelegateTo;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.platform.metadata.ClassResource;
import org.teavm.platform.metadata.StaticFieldResource;
import org.teavm.platform.plugin.PlatformGenerator;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public final class Platform {
    private Platform() {
    }

    @InjectedBy(PlatformGenerator.class)
    public static native PlatformObject getPlatformObject(Object obj);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Object clone(Object obj);

    @DelegateTo("isInstanceLowLevel")
    public static boolean isInstance(PlatformObject obj, PlatformClass cls) {
        return obj != null && !isUndefined(obj.getPlatformClass().getMetadata())
                && isAssignable(obj.getPlatformClass(), cls);
    }

    @SuppressWarnings("unused")
    private static boolean isInstanceLowLevel(RuntimeClass self, RuntimeObject object) {
        return isAssignableLowLevel(RuntimeClass.getClass(object), self);
    }

    @JSBody(params = "object", script = "return typeof object === 'undefined';")
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
    private static boolean isAssignableLowLevel(RuntimeClass from, RuntimeClass to) {
        return to.isSupertypeOf.apply(from);
    }

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Class<?> asJavaClass(PlatformObject obj);

    public static PlatformConsole getConsole() {
        return (PlatformConsole) Window.current();
    }

    @JSBody(params = {}, script = "return $rt_nextId();")
    public static native int nextObjectId();

    public static <T> T newInstance(PlatformClass cls) {
        prepareNewInstance();
        return newInstanceImpl(cls);
    }

    @GeneratedBy(PlatformGenerator.class)
    private static native void prepareNewInstance();

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    private static native <T> T newInstanceImpl(PlatformClass cls);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native PlatformClass lookupClass(String name);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native void initClass(PlatformClass cls);

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native PlatformClass classFromResource(ClassResource resource);

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Object objectFromResource(StaticFieldResource resource);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Enum<?>[] getEnumConstants(PlatformClass cls);

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
        ((PlatformHelper) Window.current()).killSchedule(id);
    }

    @JSBody(params = {}, script = "return [];")
    public static native <T> PlatformQueue<T> createQueue();

    public static PlatformString stringFromCharCode(int charCode) {
        return ((PlatformHelper) Window.current()).getStringClass().fromCharCode(charCode);
    }

    @DelegateTo("isPrimitiveLowLevel")
    public static boolean isPrimitive(PlatformClass cls) {
        return cls.getMetadata().isPrimitive();
    }

    @SuppressWarnings("unused")
    private static boolean isPrimitiveLowLevel(RuntimeClass cls) {
        return (cls.flags & RuntimeClass.PRIMITIVE) != 0;
    }

    @DelegateTo("getArrayItemLowLevel")
    public static PlatformClass getArrayItem(PlatformClass cls) {
        return cls.getMetadata().getArrayItem();
    }

    @SuppressWarnings("unused")
    private static RuntimeClass getArrayItemLowLevel(RuntimeClass cls) {
        return cls.itemType;
    }

    @DelegateTo("getNameLowLevel")
    public static String getName(PlatformClass cls) {
        return cls.getMetadata().getName();
    }
}
