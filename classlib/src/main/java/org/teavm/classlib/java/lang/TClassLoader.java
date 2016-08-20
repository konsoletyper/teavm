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
import org.teavm.classlib.impl.Base64;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TClassLoader extends TObject {
    private TClassLoader parent;
    private static TSystemClassLoader systemClassLoader = new TSystemClassLoader();
    private static ResourceContainer resources;

    protected TClassLoader() {
        this(null);
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
        if (resources == null) {
            resources = supplyResources();
        }
        String data = resources.getResource(name);
        return data == null ? null : new ByteArrayInputStream(Base64.decode(data));
    }

    @InjectedBy(ClassLoaderNativeGenerator.class)
    private static native ResourceContainer supplyResources();

    interface ResourceContainer extends JSObject {
        @JSIndexer
        String getResource(String name);
    }
}
