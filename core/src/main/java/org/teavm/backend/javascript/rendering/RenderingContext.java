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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.common.ServiceRepository;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.dependency.DependencyInfo;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.parsing.resource.ResourceProvider;

public abstract class RenderingContext {
    private final DebugInformationEmitter debugEmitter;
    private ClassReaderSource initialClassSource;
    private ListableClassReaderSource classSource;
    private ResourceProvider resourceProvider;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;
    private NamingStrategy naming;
    private DependencyInfo dependencyInfo;
    private Predicate<MethodReference> virtualPredicate;
    private Predicate<MethodReference> forcedFunctionPredicate;
    private final Map<String, Integer> stringPoolMap = new HashMap<>();
    private final List<String> stringPool = new ArrayList<>();
    private final List<String> readonlyStringPool = Collections.unmodifiableList(stringPool);
    private final Map<MethodReference, InjectorHolder> injectorMap = new HashMap<>();
    private boolean minifying;
    private ClassInitializerInfo classInitializerInfo;
    private boolean strict;

    public RenderingContext(DebugInformationEmitter debugEmitter,
            ClassReaderSource initialClassSource, ListableClassReaderSource classSource,
            ResourceProvider resourceProvider,
            ClassLoader classLoader, ServiceRepository services, Properties properties,
            NamingStrategy naming, DependencyInfo dependencyInfo,
            Predicate<MethodReference> virtualPredicate,
            Predicate<MethodReference> forcedFunctionPredicate,
            ClassInitializerInfo classInitializerInfo,
            boolean strict) {
        this.debugEmitter = debugEmitter;
        this.initialClassSource = initialClassSource;
        this.classSource = classSource;
        this.resourceProvider = resourceProvider;
        this.classLoader = classLoader;
        this.services = services;
        this.properties = properties;
        this.naming = naming;
        this.dependencyInfo = dependencyInfo;
        this.virtualPredicate = virtualPredicate;
        this.forcedFunctionPredicate = forcedFunctionPredicate;
        this.classInitializerInfo = classInitializerInfo;
        this.strict = strict;
    }

    public ClassReaderSource getInitialClassSource() {
        return initialClassSource;
    }

    public ListableClassReaderSource getClassSource() {
        return classSource;
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
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

    public DependencyInfo getDependencyInfo() {
        return dependencyInfo;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public DebugInformationEmitter getDebugEmitter() {
        return debugEmitter;
    }

    public boolean isVirtual(MethodReference method) {
        return virtualPredicate.test(method);
    }

    public boolean isForcedFunction(MethodReference method) {
        return forcedFunctionPredicate.test(method);
    }

    public boolean isDynamicInitializer(String className) {
        return classInitializerInfo.isDynamicInitializer(className);
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

    public void constantToString(SourceWriter writer, Object cst) {
        if (cst == null) {
            writer.append("null");
        }
        if (cst instanceof ValueType) {
            ValueType type = (ValueType) cst;
            writer.appendFunction("$rt_cls").append("(");
            typeToClsString(writer, type);
            writer.append(")");
        } else if (cst instanceof String) {
            String string = (String) cst;
            int index = lookupString(string);
            writer.appendFunction("$rt_s").append("(" + index + ")");
        } else if (cst instanceof Long) {
            long value = (Long) cst;
            if (value == 0) {
                writer.appendFunction("Long_ZERO");
            } else if ((int) value == value) {
                writer.appendFunction("Long_fromInt").append("(").append(String.valueOf(value)).append(")");
            } else {
                writer.appendFunction("Long_create").append("(" + (value & 0xFFFFFFFFL)
                        + ", " + (value >>> 32) + ")");
            }
        } else if (cst instanceof Character) {
            writer.append(Integer.toString((Character) cst));
        } else if (cst instanceof Boolean) {
            writer.append((Boolean) cst ? "1" : "0");
        } else if (cst instanceof Integer) {
            int value = (Integer) cst;
            if (value < 0) {
                writer.append("(");
                writer.append(Integer.toString(value));
                writer.append(")");
            } else {
                writer.append(Integer.toString(value));
            }
        } else if (cst instanceof Byte) {
            int value = (Byte) cst;
            if (value < 0) {
                writer.append("(");
                writer.append(Integer.toString(value));
                writer.append(")");
            } else {
                writer.append(Integer.toString(value));
            }
        } else if (cst instanceof Short) {
            int value = (Short) cst;
            if (value < 0) {
                writer.append("(");
                writer.append(Integer.toString(value));
                writer.append(")");
            } else {
                writer.append(Integer.toString(value));
            }
        } else if (cst instanceof Double) {
            double value = (Double) cst;
            if (value < 0) {
                writer.append("(");
                writer.append(Double.toString(value));
                writer.append(")");
            } else {
                writer.append(Double.toString(value));
            }
        } else if (cst instanceof Float) {
            float value = (Float) cst;
            if (value < 0) {
                writer.append("(");
                writer.append(Double.toString((double) value));
                writer.append(")");
            } else {
                writer.append(Double.toString((double) value));
            }
        }
    }

    public void typeToClsString(SourceWriter writer, ValueType type) {
        int arrayCount = 0;
        while (type instanceof ValueType.Array) {
            arrayCount++;
            type = ((ValueType.Array) type).getItemType();
        }

        for (int i = 0; i < arrayCount; ++i) {
            writer.appendFunction("$rt_arraycls").append("(");
        }

        if (type instanceof ValueType.Object) {
            ValueType.Object objType = (ValueType.Object) type;
            writer.appendClass(objType.getClassName());
        } else if (type instanceof ValueType.Void) {
            writer.appendFunction("$rt_voidcls");
        } else if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitiveType = (ValueType.Primitive) type;
            switch (primitiveType.getKind()) {
                case BOOLEAN:
                    writer.appendFunction("$rt_booleancls");
                    break;
                case CHARACTER:
                    writer.appendFunction("$rt_charcls");
                    break;
                case BYTE:
                    writer.appendFunction("$rt_bytecls");
                    break;
                case SHORT:
                    writer.appendFunction("$rt_shortcls");
                    break;
                case INTEGER:
                    writer.appendFunction("$rt_intcls");
                    break;
                case LONG:
                    writer.appendFunction("$rt_longcls");
                    break;
                case FLOAT:
                    writer.appendFunction("$rt_floatcls");
                    break;
                case DOUBLE:
                    writer.appendFunction("$rt_doublecls");
                    break;
                default:
                    throw new IllegalArgumentException("The type is not renderable");
            }
        } else {
            throw new IllegalArgumentException("The type is not renderable");
        }

        for (int i = 0; i < arrayCount; ++i) {
            writer.append(")");
        }
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

    public boolean isStrict() {
        return strict;
    }

    public abstract String importModule(String name);

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
