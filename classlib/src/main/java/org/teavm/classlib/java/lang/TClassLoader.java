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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.impl.Base64Impl;
import org.teavm.classlib.java.net.TURL;
import org.teavm.classlib.java.net.TURLConnection;
import org.teavm.classlib.java.net.TURLStreamHandler;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Platforms;
import org.teavm.interop.SupportedOn;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class TClassLoader extends TObject {
    private TClassLoader parent;
    private static TSystemClassLoader systemClassLoader = new TSystemClassLoader();
    private static ResourceContainer resources;
    private static ResourceContainer resourceLists;

    protected TClassLoader() {
        this(systemClassLoader);
    }

    protected TClassLoader(TClassLoader parent) {
        this.parent = parent;
    }

    public TClassLoader getParent() {
        return parent;
    }

    public static TClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public InputStream getResourceAsStream(String name) {
        if (PlatformDetector.isJavaScript()) {
            TURL resource = getResource(name);
            try {
                return resource != null ? resource.openStream() : null;
            } catch (IOException e) {
                return null;
            }
        }
        if (resources == null) {
            resources = supplyResources();
        }
        JSObject data = resources.getResource(name);
        String dataString = resourceToString(data);
        if (dataString == null) {
            return null;
        }
        return new ByteArrayInputStream(decodeResource(dataString));
    }

    private static byte[] decodeResource(String dataString) {
        byte[] bytes = new byte[dataString.length()];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) dataString.charAt(i);
        }
        return Base64Impl.decode(bytes);
    }

    public TClass<?> loadClass(String name) throws TClassNotFoundException {
        return loadClass(name, false);
    }

    protected TClass<?> loadClass(String name, boolean resolve) throws TClassNotFoundException {
        Objects.requireNonNull(name);
        if (parent != null) {
            try {
                return parent.loadClass(name);
            } catch (TClassNotFoundException e) {
                // All loadable classes are part of TeaVM's closed-world class table.
            }
        }
        return TClass.forName((TString) (Object) name, false, this);
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    public TURL getResource(String name) {
        Objects.requireNonNull(name);
        TURL resource = parent != null ? parent.getResource(name) : null;
        return resource != null ? resource : findResource(name);
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    protected TURL findResource(String name) {
        ResourceList list = embeddedResources(name);
        return list != null && list.getLength() > 0 ? resourceURL(name, 0, list.get(0)) : null;
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    public Enumeration<TURL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);
        var result = new ArrayList<TURL>();
        if (parent != null) {
            var inherited = parent.getResources(name);
            while (inherited.hasMoreElements()) {
                result.add(inherited.nextElement());
            }
        }
        var own = findResources(name);
        while (own.hasMoreElements()) {
            result.add(own.nextElement());
        }
        return Collections.enumeration(result);
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    protected Enumeration<TURL> findResources(String name) throws IOException {
        ResourceList list = embeddedResources(name);
        var result = new ArrayList<TURL>();
        if (list != null) {
            for (int i = 0; i < list.getLength(); ++i) {
                result.add(resourceURL(name, i, list.get(i)));
            }
        }
        return Collections.enumeration(result);
    }

    private ResourceList embeddedResources(String name) {
        if (this != systemClassLoader) {
            return null;
        }
        if (resourceLists == null) {
            resourceLists = supplyResourceLists();
        }
        return resourceToList(resourceLists.getResource(name));
    }

    private static TURL resourceURL(String name, int index, String data) {
        try {
            String spec = new URI("teavm-resource", null, "/" + index + "/" + name, null).toASCIIString();
            return new TURL(null, spec, new EmbeddedResourceHandler(decodeResource(data)));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("Could not represent embedded resource: " + name, e);
        }
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    public static TURL getSystemResource(String name) {
        return systemClassLoader.getResource(name);
    }

    @SupportedOn(Platforms.JAVASCRIPT)
    public static Enumeration<TURL> getSystemResources(String name) throws IOException {
        return systemClassLoader.getResources(name);
    }

    public static InputStream getSystemResourceAsStream(String name) {
        return getSystemClassLoader().getResourceAsStream(name);
    }

    @JSBody(params = "resource", script = "return resource !== null && resource !== void 0 ? resource : null;")
    private static native String resourceToString(JSObject resource);

    @InjectedBy(ClassLoaderNativeGenerator.class)
    @NoSideEffects
    private static native ResourceContainer supplyResources();

    @InjectedBy(ClassLoaderNativeGenerator.class)
    @NoSideEffects
    private static native ResourceContainer supplyResourceLists();

    @JSBody(params = "resource", script = "return resource == null ? null : resource;")
    private static native ResourceList resourceToList(JSObject resource);

    interface ResourceList extends JSObject {
        @JSProperty
        int getLength();

        @JSIndexer
        String get(int index);
    }

    private static final class EmbeddedResourceHandler extends TURLStreamHandler {
        private final byte[] data;

        EmbeddedResourceHandler(byte[] data) {
            this.data = data;
        }

        @Override
        protected TURLConnection openConnection(TURL url) {
            return new TURLConnection(url) {
                @Override
                public void connect() {
                    connected = true;
                }

                @Override
                public InputStream getInputStream() {
                    connect();
                    return new ByteArrayInputStream(data);
                }

                @Override
                public int getContentLength() {
                    return data.length;
                }
            };
        }
    }

    interface ResourceContainer extends JSObject {
        @JSIndexer
        JSObject getResource(String name);
    }
}
