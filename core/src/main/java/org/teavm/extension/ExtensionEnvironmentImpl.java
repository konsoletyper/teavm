/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.extension;

import java.util.Iterator;
import java.util.Properties;
import java.util.function.Supplier;
import org.teavm.extension.diagnostics.Diagnostics;
import org.teavm.extension.diagnostics.SourceLocation;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectClassImpl;
import org.teavm.extension.introspect.IntrospectFieldImpl;
import org.teavm.extension.introspect.IntrospectMethodImpl;
import org.teavm.extension.introspect.Introspection;
import org.teavm.extension.resource.Resource;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.parsing.resource.ResourceProvider;

public class ExtensionEnvironmentImpl implements ExtensionEnvironment {
    private ResourceProvider resourceProvider;
    private Introspection introspection;
    private ClassLoader classLoader;
    private org.teavm.diagnostics.Diagnostics underlyingDiagnostics;
    private Supplier<Properties> propertiesSupplier;
    private Properties properties;
    private boolean error;

    public ExtensionEnvironmentImpl(ResourceProvider resourceProvider, ClassHierarchy hierarchy,
            ClassLoader classLoader, org.teavm.diagnostics.Diagnostics underlyingDiagnostics,
            Supplier<Properties> propertiesSupplier) {
        this.resourceProvider = resourceProvider;
        introspection = new Introspection(hierarchy, classLoader);
        this.classLoader = classLoader;
        this.underlyingDiagnostics = underlyingDiagnostics;
        this.propertiesSupplier = propertiesSupplier;
    }

    public Introspection introspection() {
        return introspection;
    }

    public boolean hasError() {
        return error;
    }

    @Override
    public IntrospectClassImpl<?> findClass(String name) {
        return introspection.findClass(name);
    }

    @Override
    public <T> IntrospectClass<T> findClass(Class<T> cls) {
        return introspection.findClass(cls);
    }

    @Override
    public Iterator<Resource> resources(String name) {
        var underlyingResources = resourceProvider.getResources(name);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return underlyingResources.hasNext();
            }

            @Override
            public Resource next() {
                var resource = underlyingResources.next();
                return resource::open;
            }
        };
    }

    @Override
    public Diagnostics diagnostics() {
        return diagnostics;
    }

    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public String property(String name) {
        if (properties == null) {
            properties = propertiesSupplier.get();
        }
        return properties.getProperty(name);
    }

    private Diagnostics diagnostics = new Diagnostics() {
        @Override
        public void error(SourceLocation location, String message, Object... params) {
            convertParams(params);
            underlyingDiagnostics.error(convertLocation(location), message, params);
        }

        @Override
        public void warning(SourceLocation location, String message, Object... params) {
            convertParams(params);
            underlyingDiagnostics.warning(convertLocation(location), message, params);
        }

        private void convertParams(Object[] params) {
            for (int i = 0; i < params.length; ++i) {
                if (params[i] instanceof IntrospectMethodImpl) {
                    params[i] = ((IntrospectMethodImpl) params[i]).method.getReference();
                } else if (params[i] instanceof IntrospectClassImpl<?>) {
                    params[i] = ((IntrospectClassImpl<?>) params[i]).type;
                } else if (params[i] instanceof IntrospectFieldImpl) {
                    params[i] = ((IntrospectFieldImpl) params[i]).field.getReference();
                } else if (params[i] instanceof Class<?>) {
                    params[i] = ValueType.parse((Class<?>) params[i]);
                }
            }
        }

        private CallLocation convertLocation(SourceLocation location) {
            if (location == null) {
                return null;
            }
            var method = ((IntrospectMethodImpl) location.method()).method;
            return location.fileName() != null
                    ? new CallLocation(method.getReference(),
                            new TextLocation(location.fileName(), location.lineNumber()))
                    : new CallLocation(method.getReference());
        }
    };

    public org.teavm.diagnostics.Diagnostics createDiagnostics() {
        return new org.teavm.diagnostics.Diagnostics() {
            @Override
            public void error(CallLocation location, String error, Object... params) {
                ExtensionEnvironmentImpl.this.error = true;
                underlyingDiagnostics.error(location, error, params);
            }

            @Override
            public void warning(CallLocation location, String error, Object... params) {
                ExtensionEnvironmentImpl.this.error = true;
                underlyingDiagnostics.warning(location, error, params);
            }
        };
    }
}
