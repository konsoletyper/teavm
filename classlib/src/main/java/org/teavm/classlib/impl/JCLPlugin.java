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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ServiceLoader;
import org.teavm.classlib.impl.lambda.LambdaMetafactorySubstitutor;
import org.teavm.classlib.impl.unicode.CLDRReader;
import org.teavm.classlib.java.lang.reflect.AnnotationDependencyListener;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.model.MethodReference;
import org.teavm.platform.PlatformClass;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class JCLPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
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

        host.registerService(CLDRReader.class, new CLDRReader(host.getProperties(), host.getClassLoader()));

        host.add(new AnnotationDependencyListener());
        host.add(new MethodReference(LambdaMetafactory.class, "metafactory", MethodHandles.Lookup.class,
                String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class,
                CallSite.class), new LambdaMetafactorySubstitutor());
        host.add(new ScalaHacks());
    }
}
