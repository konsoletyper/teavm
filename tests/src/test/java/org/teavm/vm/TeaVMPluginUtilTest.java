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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.classlib.java.lang.ClassGenerator;
import org.teavm.classlib.java.lang.ObjectDependencyPlugin;
import org.teavm.classlib.java.lang.SystemDependencyPlugin;
import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.dependency.PluggableDependency;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTest;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.MethodReference;
import org.teavm.parsing.resource.ResourceProvider;
import org.teavm.vm.spi.ClassFilter;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMHostExtension;

@TeaVMTest
@SkipJVM
public class TeaVMPluginUtilTest {
    @Test
    public void registersNativeAndRegularDependencies() {
        var nativeMethod = new MethodReference(Methods.class, "nativeMethod", int[].class, Object.class);
        var regularMethod = new MethodReference(Methods.class, "regularMethod", Object.class, Object.class);
        var host = new RecordingHost();
        TeaVMPluginUtil.handleDependencies(host, Methods.class);

        assertEquals(Set.of(nativeMethod, regularMethod), host.dependencies.keySet());
        for (var plugin : host.dependencies.values()) {
            assertTrue(plugin instanceof Plugin);
        }
    }

    @Test
    public void registersClassLibraryDependencies() {
        var host = new RecordingHost();
        TeaVMPluginUtil.handleDependencies(host, Object.class);
        TeaVMPluginUtil.handleDependencies(host, Class.class);
        TeaVMPluginUtil.handleDependencies(host, System.class);

        assertTrue(host.dependencies.get(new MethodReference(Object.class, "clone", Object.class))
                instanceof ObjectDependencyPlugin);
        assertTrue(host.dependencies.get(new MethodReference(Class.class, "getEnumConstantsFast", Object[].class))
                instanceof ClassGenerator);
        assertTrue(host.dependencies.get(new MethodReference(System.class, "doArrayCopy", Object.class, int.class,
                Object.class, int.class, int.class, void.class)) instanceof SystemDependencyPlugin);
    }

    static class Methods {
        @GeneratedBy(NativeGenerator.class)
        @PluggableDependency(Plugin.class)
        public static native Object nativeMethod(int[] values);

        @PluggableDependency(Plugin.class)
        public static Object regularMethod(Object value) {
            return value;
        }
    }

    public static class Plugin implements DependencyPlugin {
        @Override
        public void methodReached(DependencyAgent agent, MethodDependency method) {
        }
    }

    public static class NativeGenerator implements Generator {
        public NativeGenerator(int value) {
            throw new AssertionError("Generator must not be instantiated");
        }

        @Override
        public void generate(GeneratorContext context, SourceWriter writer, MethodReference method) {
        }
    }

    private static class RecordingHost implements TeaVMHost {
        final Map<MethodReference, DependencyPlugin> dependencies = new HashMap<>();

        @Override
        public void add(MethodReference method, DependencyPlugin plugin) {
            dependencies.put(method, plugin);
        }

        @Override
        public <T extends TeaVMHostExtension> T getExtension(Class<T> type) {
            return null;
        }

        @Override
        public void add(DependencyListener listener) {
        }

        @Override
        public void add(ClassHolderTransformer transformer) {
        }

        @Override
        public void add(MethodReference method, BootstrapMethodSubstitutor substitutor) {
        }

        @Override
        public void addVirtualMethods(Predicate<MethodReference> predicate) {
        }

        @Override
        public void addClassFilter(ClassFilter filter) {
        }

        @Override
        public <T> void registerService(Class<T> type, T instance) {
        }

        @Override
        public <T> T getService(Class<T> type) {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public ResourceProvider getResourceProvider() {
            return null;
        }

        @Override
        public Properties getProperties() {
            return new Properties();
        }

        @Override
        public String[] getPlatformTags() {
            return new String[0];
        }
    }
}
