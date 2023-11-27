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
import org.teavm.jso.JSExceptions;
import org.teavm.jso.JSObject;
import org.teavm.model.MethodReference;
import org.teavm.platform.plugin.PlatformPlugin;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.After;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

@After(PlatformPlugin.class)
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
        host.add(new JSExceptionsDependencyListener());

        jsHost.add(aliasRenderer);
        jsHost.addGeneratorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicGenerator.class.getName()));
        jsHost.addInjectorProvider(new GeneratorAnnotationInstaller<>(new JSBodyGenerator(),
                DynamicInjector.class.getName()));
        jsHost.addVirtualMethods(aliasRenderer);

        JSExceptionsGenerator exceptionsGenerator = new JSExceptionsGenerator();
        jsHost.add(new MethodReference(JSExceptions.class, "getJavaException", JSObject.class, Throwable.class),
                exceptionsGenerator);
        jsHost.add(new MethodReference(JSExceptions.class, "getJSException", Throwable.class, JSObject.class),
                exceptionsGenerator);

        var wrapperGenerator = new JSWrapperGenerator();
        jsHost.add(new MethodReference(JSWrapper.class, "directJavaToJs", Object.class, JSObject.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "directJsToJava", JSObject.class, Object.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "dependencyJavaToJs", Object.class, JSObject.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "dependencyJsToJava", JSObject.class, Object.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "isJava", Object.class, boolean.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "isJava", JSObject.class, boolean.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "wrapperToJs", JSWrapper.class, JSObject.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "jsToWrapper", JSObject.class, JSWrapper.class),
                wrapperGenerator);
        jsHost.add(new MethodReference(JSWrapper.class, "isJSImplementation", Object.class, boolean.class),
                wrapperGenerator);

        host.add(new MethodReference(JSWrapper.class, "jsToWrapper", JSObject.class, JSWrapper.class),
                wrapperGenerator);
        host.add(new MethodReference(JSWrapper.class, "dependencyJavaToJs", Object.class, JSObject.class),
                wrapperGenerator);
        host.add(new MethodReference(JSWrapper.class, "dependencyJsToJava", JSObject.class, Object.class),
                wrapperGenerator);

        TeaVMPluginUtil.handleNatives(host, JS.class);
    }
}
