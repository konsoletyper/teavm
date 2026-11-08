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
package org.teavm.extension.bridge.guest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.teavm.extension.ExtensionEnvironment;
import org.teavm.extension.bridge.BridgeAnnotation;
import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeElement;
import org.teavm.extension.bridge.BridgeField;
import org.teavm.extension.bridge.BridgeGenericArray;
import org.teavm.extension.bridge.BridgeHost;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeMember;
import org.teavm.extension.bridge.BridgeMethod;
import org.teavm.extension.bridge.BridgeParameter;
import org.teavm.extension.bridge.BridgeParameterizedType;
import org.teavm.extension.bridge.BridgeTypeArgument;
import org.teavm.extension.bridge.BridgeTypeVariable;
import org.teavm.extension.diagnostics.Diagnostics;
import org.teavm.extension.diagnostics.SourceLocation;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.resource.Resource;
import org.teavm.jso.core.JSSymbol;

/**
 * Implementation of {@link ExtensionEnvironment} that delegates to host through {@link BridgeHost}.
 * Maintains one-to-one correspondence between host's elements and local elements: local element is stored
 * in a property of host's JS object, keyed by a symbol, private to this environment.
 */
public class GuestEnvironment implements ExtensionEnvironment {
    final BridgeHost host;
    private final JSSymbol<GuestElement> symbol = JSSymbol.create("teavmGuestElement");

    public GuestEnvironment(BridgeHost host) {
        this.host = host;
    }

    @Override
    public IntrospectClass<?> findClass(String name) {
        return (IntrospectClass<?>) element(host.findClass(name));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> IntrospectClass<T> findClass(Class<T> cls) {
        return (IntrospectClass<T>) element(host.findClassByDesc(toDesc(cls)));
    }

    private static String toDesc(Class<?> cls) {
        if (cls.isPrimitive()) {
            if (cls == boolean.class) {
                return "Z";
            } else if (cls == byte.class) {
                return "B";
            } else if (cls == short.class) {
                return "S";
            } else if (cls == char.class) {
                return "C";
            } else if (cls == int.class) {
                return "I";
            } else if (cls == long.class) {
                return "J";
            } else if (cls == float.class) {
                return "F";
            } else if (cls == double.class) {
                return "D";
            } else if (cls == void.class) {
                return "V";
            }
            throw new IllegalArgumentException("Unknown primitive class: " + cls);
        } else if (cls.isArray()) {
            return "[" + toDesc(cls.getComponentType());
        }
        return "L" + cls.getName().replace('.', '/') + ";";
    }

    @Override
    public Iterator<Resource> resources(String name) {
        var remoteResources = host.resources(name);
        var result = new ArrayList<Resource>(remoteResources.length);
        for (var remoteResource : remoteResources) {
            result.add(() -> new ByteArrayInputStream(remoteResource.read()));
        }
        return result.iterator();
    }

    @Override
    public Diagnostics diagnostics() {
        return diagnostics;
    }

    @Override
    public ClassLoader classLoader() {
        throw new UnsupportedOperationException("Class loader is not available when extensions run "
                + "in a separate module");
    }

    @Override
    public String property(String name) {
        return host.property(name);
    }

    Object element(BridgeElement remote) {
        if (remote == null) {
            return null;
        }
        if (symbol.presentIn(remote)) {
            return symbol.get(remote);
        }
        var result = createElement(remote);
        symbol.set(remote, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    <T> List<T> elements(BridgeElement[] remotes) {
        if (remotes.length == 0) {
            return Collections.emptyList();
        }
        var result = new ArrayList<T>(remotes.length);
        for (var remote : remotes) {
            result.add((T) element(remote));
        }
        return Collections.unmodifiableList(result);
    }

    BridgeElement remoteOf(Object element) {
        if (element == null) {
            return null;
        }
        if (!(element instanceof GuestElement) || ((GuestElement) element).env != this) {
            throw new IllegalArgumentException("Element " + element + " does not belong to this environment");
        }
        return ((GuestElement) element).remote;
    }

    BridgeClass[] remoteClasses(IntrospectClass<?>[] classes) {
        var result = new BridgeClass[classes.length];
        for (var i = 0; i < classes.length; ++i) {
            result[i] = ((GuestClass<?>) remoteOwner(classes[i])).remoteClass;
        }
        return result;
    }

    BridgeMember[] remoteMembers(List<?> members) {
        var result = new BridgeMember[members.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = ((GuestMember) remoteOwner(members.get(i))).remoteMember;
        }
        return result;
    }

    private GuestElement remoteOwner(Object element) {
        remoteOf(element);
        return (GuestElement) element;
    }

    private GuestElement createElement(BridgeElement remote) {
        switch (remote.kind()) {
            case BridgeKinds.CLASS:
                return new GuestClass<>(this, (BridgeClass) remote);
            case BridgeKinds.FIELD:
                return new GuestField(this, (BridgeField) remote);
            case BridgeKinds.METHOD:
                return new GuestMethod(this, (BridgeMethod) remote);
            case BridgeKinds.PARAMETER:
                return new GuestParameter(this, (BridgeParameter) remote);
            case BridgeKinds.ANNOTATION:
                return new GuestAnnotation<>(this, (BridgeAnnotation) remote);
            case BridgeKinds.TYPE_VARIABLE:
                return new GuestTypeVariable(this, (BridgeTypeVariable) remote);
            case BridgeKinds.PARAMETERIZED_TYPE:
                return new GuestParameterizedType(this, (BridgeParameterizedType) remote);
            case BridgeKinds.GENERIC_ARRAY:
                return new GuestGenericArray(this, (BridgeGenericArray) remote);
            case BridgeKinds.TYPE_ARGUMENT:
                return new GuestTypeArgument(this, (BridgeTypeArgument) remote);
            default:
                throw new IllegalArgumentException("Unexpected element kind: " + remote.kind());
        }
    }

    private Diagnostics diagnostics = new Diagnostics() {
        @Override
        public void error(SourceLocation location, String message, Object... params) {
            report(true, location, message, params);
        }

        @Override
        public void warning(SourceLocation location, String message, Object... params) {
            report(false, location, message, params);
        }
    };

    private void report(boolean error, SourceLocation location, String message, Object[] params) {
        var paramElements = new BridgeElement[params.length];
        var paramStrings = new String[params.length];
        for (var i = 0; i < params.length; ++i) {
            var param = params[i];
            if (param instanceof Class<?>) {
                var cls = findClass((Class<?>) param);
                if (cls != null) {
                    param = cls;
                }
            }
            if (param instanceof GuestElement && ((GuestElement) param).env == this) {
                paramElements[i] = ((GuestElement) param).remote;
            } else {
                paramStrings[i] = String.valueOf(param);
            }
        }
        BridgeMethod method = null;
        String fileName = null;
        var lineNumber = -1;
        if (location != null) {
            method = ((GuestMethod) remoteOwner(location.method())).remoteMethod;
            fileName = location.fileName();
            lineNumber = location.lineNumber();
        }
        host.report(error, method, fileName, lineNumber, message, paramElements, paramStrings);
    }
}
