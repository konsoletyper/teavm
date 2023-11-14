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
package org.teavm.backend.javascript;

import java.util.function.Function;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.RendererListener;
import org.teavm.vm.spi.TeaVMHostExtension;

public interface TeaVMJavaScriptHost extends TeaVMHostExtension {
    void add(MethodReference methodRef, Generator generator);

    void add(MethodReference methodRef, Injector injector);

    void addGeneratorProvider(Function<ProviderContext, Generator> provider);

    void addInjectorProvider(Function<ProviderContext, Injector> provider);

    void add(RendererListener listener);

    void addVirtualMethods(VirtualMethodContributor virtualMethods);
}
