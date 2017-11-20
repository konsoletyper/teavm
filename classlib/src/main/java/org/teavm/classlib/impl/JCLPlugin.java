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
package org.teavm.classlib.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.classlib.impl.lambda.LambdaMetafactorySubstitutor;
import org.teavm.classlib.impl.unicode.CLDRReader;
import org.teavm.classlib.java.lang.reflect.AnnotationDependencyListener;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.PlatformClass;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class JCLPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        if (!isBootstrap()) {
            ServiceLoaderSupport serviceLoaderSupp = new ServiceLoaderSupport(host.getClassLoader());
            host.add(serviceLoaderSupp);
            MethodReference loadServicesMethod = new MethodReference(ServiceLoader.class, "loadServices",
                    PlatformClass.class, Object[].class);
            TeaVMJavaScriptHost jsExtension = host.getExtension(TeaVMJavaScriptHost.class);
            if (jsExtension != null) {
                jsExtension.add(loadServicesMethod, serviceLoaderSupp);
            }

            JavacSupport javacSupport = new JavacSupport();
            host.add(javacSupport);
        }

        if (!isBootstrap()) {
            host.registerService(CLDRReader.class, CLDRReader.getInstance(host.getProperties(), host.getClassLoader()));

            host.add(new ClassForNameTransformer());
        }

        host.add(new AnnotationDependencyListener());

        LambdaMetafactorySubstitutor lms = new LambdaMetafactorySubstitutor();
        host.add(new MethodReference("java.lang.invoke.LambdaMetafactory", "metafactory",
                ValueType.object("java.lang.invoke.MethodHandles$Lookup"), ValueType.object("java.lang.String"),
                ValueType.object("java.lang.invoke.MethodType"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.object("java.lang.invoke.MethodHandle"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.object("java.lang.invoke.CallSite")), lms);
        host.add(new MethodReference("java.lang.invoke.LambdaMetafactory", "altMetafactory",
                ValueType.object("java.lang.invoke.MethodHandles$Lookup"),
                ValueType.object("java.lang.String"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.arrayOf(ValueType.object("java.lang.Object")),
                ValueType.object("java.lang.invoke.CallSite")), lms);

        if (!isBootstrap()) {
            host.add(new ScalaHacks());
        }

        host.add(new NumericClassTransformer());

        if (!isBootstrap()) {
            List<ReflectionSupplier> reflectionSuppliers = new ArrayList<>();
            for (ReflectionSupplier supplier : ServiceLoader.load(ReflectionSupplier.class, host.getClassLoader())) {
                reflectionSuppliers.add(supplier);
            }
            ReflectionDependencyListener reflection = new ReflectionDependencyListener(reflectionSuppliers);
            host.registerService(ReflectionDependencyListener.class, reflection);
            host.add(reflection);

            host.add(new PlatformMarkerSupport());
        }

        TeaVMPluginUtil.handleNatives(host, Class.class);
        TeaVMPluginUtil.handleNatives(host, ClassLoader.class);
        TeaVMPluginUtil.handleNatives(host, System.class);
        TeaVMPluginUtil.handleNatives(host, Array.class);
        TeaVMPluginUtil.handleNatives(host, Math.class);
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }
}
