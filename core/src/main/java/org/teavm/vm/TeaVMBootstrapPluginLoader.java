/*
 *  Copyright 2017 Alexey Andreev.
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

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.findClass;
import java.util.ArrayList;
import java.util.List;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectMethod;
import org.teavm.vm.spi.TeaVMPlugin;

@CompileTime
public final class TeaVMBootstrapPluginLoader {
    private TeaVMBootstrapPluginLoader() {
    }

    @Meta
    public native static List<TeaVMPlugin> loadPlugins(boolean ignore);

    private static void loadPlugins(Value<Boolean> ignore) {
        Value<List<TeaVMPlugin>> plugins = emit(() -> new ArrayList<>());
        List<String> pluginClassNames = new ArrayList<>();
        TeaVMPluginReader.load(Metaprogramming.getClassLoader(), pluginClassNames::add);
        for (String pluginClassName : pluginClassNames) {
            ReflectClass<?> cls = findClass(pluginClassName);
            ReflectMethod constructor = cls.getMethod("<init>");
            emit(() -> plugins.get().add((TeaVMPlugin) constructor.construct()));
        }
        exit(() -> plugins.get());
    }
}
