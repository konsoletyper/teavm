/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.impl.Base64Impl;
import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

public abstract class TClassLoader extends TObject {
    private TClassLoader parent;
    private static TSystemClassLoader systemClassLoader = new TSystemClassLoader();
    private static ResourceContainer resources;

    // As in the JDK, the default parent is the system class loader.
    protected TClassLoader() {
        this(getSystemClassLoader());
    }

    protected TClassLoader(TClassLoader parent) {
        this.parent = parent;
    }

    public TClassLoader getParent() {
        return parent;
    }

    // TeaVM has one class space, and the system loader finds every class that the compiled program kept; any other
    // name throws ClassNotFoundException, as a JVM does for a class that is not on the class path. A subclass
    // delegates as in the JDK: the parent (or, with no parent, the bootstrap classes) first, then findClass.
    public TClass<?> loadClass(String name) throws TClassNotFoundException {
        return loadClass(name, false);
    }

    protected TClass<?> loadClass(String name, boolean resolve) throws TClassNotFoundException {
        TClass<?> c = null;
        try {
            c = parent != null ? parent.loadClass(name, false) : findBootstrapClassOrNull(name);
        } catch (TClassNotFoundException e) {
            // As in the JDK: the parent does not have it, so this loader's findClass tries next.
        }
        if (c == null) {
            c = findClass(name);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    protected TClass<?> findClass(String name) throws TClassNotFoundException {
        throw new TClassNotFoundException();
    }

    // A loader can not define classes in TeaVM, and the compiled program never resolves a class reference through
    // a user loader, so no loader is ever recorded as a class's defining or initiating loader.
    protected final TClass<?> findLoadedClass(String name) {
        return null;
    }

    // TeaVM links every class at compile time, so there is nothing left to link.
    protected final void resolveClass(TClass<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
    }

    /**
     * The JDK's bootstrap loader holds the platform classes. Here that is the classes in the JDK's packages, of
     * those the program kept.
     */
    static TClass<?> findBootstrapClassOrNull(String name) {
        if (!(name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("sun.") || name.startsWith("com.sun.") || name.startsWith("org.w3c.")
                || name.startsWith("org.xml.") || name.startsWith("org.ietf."))) {
            return null;
        }
        try {
            return TClass.forName((TString) (Object) name);
        } catch (TClassNotFoundException e) {
            return null;
        }
    }

    public static TClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public InputStream getResourceAsStream(String name) {
        if (resources == null) {
            resources = supplyResources();
        }
        JSObject data = resources.getResource(name);
        String dataString = resourceToString(data);
        if (dataString == null) {
            return null;
        }
        byte[] bytes = new byte[dataString.length()];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) dataString.charAt(i);
        }
        return new ByteArrayInputStream(Base64Impl.decode(bytes));
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return getSystemClassLoader().getResourceAsStream(name);
    }

    @JSBody(params = "resource", script = "return resource !== null && resource !== void 0 ? resource : null;")
    private static native String resourceToString(JSObject resource);

    @InjectedBy(ClassLoaderNativeGenerator.class)
    @NoSideEffects
    private static native ResourceContainer supplyResources();

    interface ResourceContainer extends JSObject {
        @JSIndexer
        JSObject getResource(String name);
    }
}
