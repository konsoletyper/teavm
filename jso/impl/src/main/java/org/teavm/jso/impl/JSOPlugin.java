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
package org.teavm.jso.impl;

import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class JSOPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        TeaVMJavaScriptHost jsHost = host.getExtension(TeaVMJavaScriptHost.class);
        if (jsHost == null) {
            return;
        }

        JSBodyRepository repository = new JSBodyRepository();
        host.registerService(JSBodyRepository.class, repository);
        host.add(new JSObjectClassTransformer(repository));
        JSDependencyListener dependencyListener = new JSDependencyListener(repository);
        JSAliasRenderer aliasRenderer = new JSAliasRenderer();
        host.add(dependencyListener);


        jsHost.add(aliasRenderer);
        jsHost.addGeneratorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicGenerator.class.getName()));
        jsHost.addInjectorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicInjector.class.getName()));

        TeaVMPluginUtil.handleNatives(host, JS.class);
    }
}
