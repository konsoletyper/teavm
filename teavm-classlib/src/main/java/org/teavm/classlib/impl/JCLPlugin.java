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

import org.teavm.classlib.impl.unicode.CLDRReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 *
 * @author Alexey Andreev
 */
public class JCLPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        host.add(new EnumDependencySupport());
        host.add(new EnumTransformer());
        host.add(new ClassLookupDependencySupport());
        host.add(new NewInstanceDependencySupport());
        host.add(new ObjectEnrichRenderer());
        ServiceLoaderSupport serviceLoaderSupp = new ServiceLoaderSupport(host.getClassLoader());
        host.add(serviceLoaderSupp);
        MethodReference loadServicesMethod = new MethodReference("java.util.ServiceLoader", new MethodDescriptor(
                "loadServices", ValueType.object("java.lang.Class"),
                ValueType.arrayOf(ValueType.object("java.lang.Object"))));
        host.add(loadServicesMethod, serviceLoaderSupp);
        JavacSupport javacSupport = new JavacSupport();
        host.add(javacSupport);

        host.registerService(CLDRReader.class, new CLDRReader(host.getProperties(), host.getClassLoader()));
    }
}
