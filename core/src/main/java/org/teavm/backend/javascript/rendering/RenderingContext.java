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
package org.teavm.backend.javascript.rendering;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class RenderingContext {
    private final DebugInformationEmitter debugEmitter;
    private ListableClassReaderSource classSource;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;
    private NamingStrategy naming;
    private final Deque<LocationStackEntry> locationStack = new ArrayDeque<>();
    private final Map<String, Integer> stringPoolMap = new HashMap<>();
    private final List<String> stringPool = new ArrayList<>();
    private final List<String> readonlyStringPool = Collections.unmodifiableList(stringPool);
    private final Map<MethodReference, InjectorHolder> injectorMap = new HashMap<>();
    private boolean minifying;

    public RenderingContext(DebugInformationEmitter debugEmitter, ListableClassReaderSource classSource,
            ClassLoader classLoader, ServiceRepository services, Properties properties,
            NamingStrategy naming) {
        this.debugEmitter = debugEmitter;
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.services = services;
        this.properties = properties;
        this.naming = naming;
    }

    public ListableClassReaderSource getClassSource() {
        return classSource;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ServiceRepository getServices() {
        return services;
    }

    public Properties getProperties() {
        return properties;
    }

    public NamingStrategy getNaming() {
        return naming;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public void pushLocation(TextLocation location) {
        LocationStackEntry prevEntry = locationStack.peek();
        if (location != null) {
            if (prevEntry == null || !location.equals(prevEntry.location)) {
                debugEmitter.emitLocation(location.getFileName(), location.getLine());
            }
        } else {
            if (prevEntry != null) {
                debugEmitter.emitLocation(null, -1);
            }
        }
        locationStack.push(new LocationStackEntry(location));
    }

    public void popLocation() {
        LocationStackEntry prevEntry = locationStack.pop();
        LocationStackEntry entry = locationStack.peek();
        if (entry != null) {
            if (!entry.location.equals(prevEntry.location)) {
                debugEmitter.emitLocation(entry.location.getFileName(), entry.location.getLine());
            }
        } else {
            debugEmitter.emitLocation(null, -1);
        }
    }

    public boolean isMinifying() {
        return minifying;
    }

    public int lookupString(String string) {
        return stringPoolMap.computeIfAbsent(string, key -> {
            stringPool.add(key);
            return stringPool.size() - 1;
        });
    }

    public List<String> getStringPool() {
        return readonlyStringPool;
    }

    public String constantToString(Object cst) {
        if (cst == null) {
            return "null";
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType) cst;
            return naming.getNameForFunction("$rt_cls") + "(" + typeToClsString(type) + ")";
        } else if (cst instanceof String) {
            String string = (String) cst;
            int index = lookupString(string);
            return naming.getNameForFunction("$rt_s") + "(" + index + ")";
        } else if (cst instanceof Long) {
            long value = (Long) cst;
            if (value == 0) {
                return "Long_ZERO";
            } else if ((int) value == value) {
                return "Long_fromInt(" + value + ")";
            } else {
                return "new Long(" + (value & 0xFFFFFFFFL) + ", " + (value >>> 32) + ")";
            }
        } else if (cst instanceof Character) {
            return Integer.toString((Character) cst);
        } else if (cst instanceof Boolean) {
            return (Boolean) cst ? "1" : "0";
        } else {
            return cst.toString();
        }
    }

    public String typeToClsString(ValueType type) {
        int arrayCount = 0;
        while (type instanceof ValueType.Array) {
            arrayCount++;
            type = ((ValueType.Array) type).getItemType();
        }
        String value;
        if (type instanceof ValueType.Object) {
            ValueType.Object objType = (ValueType.Object) type;
            value = naming.getNameFor(objType.getClassName());
        } else if (type instanceof ValueType.Void) {
            value = "$rt_voidcls()";
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive) type;
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                    value = "$rt_booleancls()";
                    break;
                case CHARACTER:
                    value = "$rt_charcls()";
                    break;
                case BYTE:
                    value = "$rt_bytecls()";
                    break;
                case SHORT:
                    value = "$rt_shortcls()";
                    break;
                case INTEGER:
                    value = "$rt_intcls()";
                    break;
                case LONG:
                    value = "$rt_longcls()";
                    break;
                case FLOAT:
                    value = "$rt_floatcls()";
                    break;
                case DOUBLE:
                    value = "$rt_doublecls()";
                    break;
                default:
                    throw new IllegalArgumentException("The type is not renderable");
            }
        } else {
            throw new IllegalArgumentException("The type is not renderable");
        }

        for (int i = 0; i < arrayCount; ++i) {
            value = "$rt_arraycls(" + value + ")";
        }
        return value;
    }

    public String pointerName() {
        return minifying ? "$p" : "$ptr";
    }

    public String mainLoopName() {
        return minifying ? "_" : "main";
    }

    public String tempVarName() {
        return minifying ? "$z" : "$tmp";
    }

    public String threadName() {
        return minifying ? "$T" : "$thread";
    }

    private static class LocationStackEntry {
        final TextLocation location;

        LocationStackEntry(TextLocation location) {
            this.location = location;
        }
    }

    public void addInjector(MethodReference method, Injector injector) {
        injectorMap.put(method, new InjectorHolder(injector));
    }

    public Injector getInjector(MethodReference ref) {
        InjectorHolder holder = injectorMap.get(ref);
        if (holder == null) {
            holder = new InjectorHolder(null);
            if (!isBootstrap()) {
                ClassReader cls = classSource.get(ref.getClassName());
                if (cls != null) {
                    MethodReader method = cls.getMethod(ref.getDescriptor());
                    if (method != null) {
                        AnnotationReader injectedByAnnot = method.getAnnotations().get(InjectedBy.class.getName());
                        if (injectedByAnnot != null) {
                            ValueType type = injectedByAnnot.getValue("value").getJavaClass();
                            holder = new InjectorHolder(instantiateInjector(((ValueType.Object) type).getClassName()));
                        }
                    }
                }
            }
            injectorMap.put(ref, holder);
        }
        return holder.injector;
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }

    private Injector instantiateInjector(String type) {
        try {
            Class<? extends Injector> cls = Class.forName(type, true, classLoader).asSubclass(Injector.class);
            Constructor<? extends Injector> cons = cls.getConstructor();
            return cons.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal injector: " + type, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Default constructor was not found in the " + type + " injector", e);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error instantiating injector " + type, e);
        }
    }

    private static class InjectorHolder {
        public final Injector injector;

        private InjectorHolder(Injector injector) {
            this.injector = injector;
        }
    }
}
