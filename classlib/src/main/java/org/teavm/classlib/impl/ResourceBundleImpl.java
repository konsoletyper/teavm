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
package org.teavm.classlib.impl;

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.findClass;
import static org.teavm.metaprogramming.Metaprogramming.getClassLoader;
import static org.teavm.metaprogramming.Metaprogramming.lazy;
import static org.teavm.metaprogramming.Metaprogramming.lazyFragment;
import static org.teavm.metaprogramming.Metaprogramming.proxy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Supplier;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectMethod;

@CompileTime
public class ResourceBundleImpl {
    private ResourceBundleImpl() {
    }

    @Meta
    public static native Map<String, Supplier<ResourceBundle>> createBundleMap(boolean b);
    private static void createBundleMap(Value<Boolean> b) throws IOException {
        ClassLoader loader = getClassLoader();

        Enumeration<URL> urls = loader.getResources("META-INF/services/java.util.ResourceBundle");
        Set<String> implementations = new LinkedHashSet<>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    implementations.add(line);
                }
            }
        }

        Value<Map<String, Supplier<ResourceBundle>>> result = emit(() -> new HashMap<>());

        for (String implementation : implementations) {
            String path = implementation.replace('.', '/');
            ReflectClass<?> cls = findClass(implementation);
            Value<? extends ResourceBundle> lazyResource;
            if (cls != null) {
                ReflectMethod constructor = cls.getMethod("<init>");
                if (constructor != null) {
                    lazyResource = lazy(() -> (ResourceBundle) constructor.construct());
                } else {
                    continue;
                }
            } else if (loader.getResource(path + ".properties") != null) {
                lazyResource = lazyFragment(() -> {
                    Properties properties = new Properties();
                    try (InputStream input = loader.getResourceAsStream(path + ".properties")) {
                        properties.load(input);
                    } catch (IOException e) {
                        // do nothing
                    }

                    return proxy(ListResourceBundle.class, (instance, methodName, args) -> {
                        Value<List<Object[]>> contentsBuilder = emit(() -> new ArrayList<>());
                        for (Object propertyName : properties.keySet()) {
                            if (!(propertyName instanceof String)) {
                                continue;
                            }
                            String key = (String) propertyName;
                            String value = properties.getProperty(key);
                            if (value == null) {
                                continue;
                            }

                            emit(() -> contentsBuilder.get().add(new Object[] { key, value }));
                        }

                        exit(() -> contentsBuilder.get().toArray(new Object[0][]));
                    });
                });
            } else {
                continue;
            }

            @SuppressWarnings("rawtypes")
            Value<Supplier> supplierValueRaw = proxy(Supplier.class,
                    (instance, methodName, args) -> {
                        exit(() -> lazyResource.get());
                    });
            @SuppressWarnings("unchecked")
            Value<Supplier<ResourceBundle>> supplierValue = emit(() -> supplierValueRaw.get());

            emit(() -> result.get().put(implementation, supplierValue.get()));
        }

        exit(() -> result.get());
    }
}
