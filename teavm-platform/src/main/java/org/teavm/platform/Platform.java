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

import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.spi.GeneratedBy;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.jso.JS;
import org.teavm.platform.metadata.ClassResource;
import org.teavm.platform.plugin.PlatformGenerator;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public final class Platform {
    private Platform() {
    }

    public static PlatformObject getPlatformObject(Object obj) {
        return (PlatformObject)JS.marshall(obj);
    }

    public static boolean isInstance(PlatformObject obj, PlatformClass cls) {
        return obj != null && !JS.isUndefined(obj.getPlatformClass().getMetadata()) &&
                isAssignable(obj.getPlatformClass(), cls);
    }

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

    @InjectedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native Class<?> asJavaClass(PlatformObject obj);

    public static PlatformPrimitives getPrimitives() {
        return (PlatformPrimitives)JS.getGlobal();
    }

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native <T> T newInstance(PlatformClass cls);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native PlatformClass lookupClass(String name);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native void initClass(PlatformClass cls);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native PlatformClass classFromResource(ClassResource resource);
}
