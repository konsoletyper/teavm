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
package org.teavm.classlib.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.parsing.resource.ResourceProvider;

public class ResourceBundleTransformer implements ClassHolderTransformer {
    private static final String IMPL_CLASS = ResourceBundleImpl.class.getName();
    private static final MethodDescriptor BUNDLE_NAMES = new MethodDescriptor("bundleNames", String[].class);
    private static final MethodDescriptor CREATE_BUNDLE = new MethodDescriptor("createBundle", int.class,
            ResourceBundle.class);
    private static final MethodReference CREATE_PROPERTIES_BUNDLE = new MethodReference(IMPL_CLASS,
            "createPropertiesBundle", ValueType.parse(String[].class), ValueType.parse(ResourceBundle.class));
    private static final MethodDescriptor INIT = new MethodDescriptor("<init>", void.class);
    private ResourceProvider resourceProvider;

    public ResourceBundleTransformer(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals(IMPL_CLASS)) {
            return;
        }

        var bundles = collectBundles(context);
        generateBundleNames(cls.getMethod(BUNDLE_NAMES), bundles, context);
        generateCreateBundle(cls.getMethod(CREATE_BUNDLE), bundles, context);
    }

    private List<Bundle> collectBundles(ClassHolderTransformerContext context) {
        var bundles = new ArrayList<Bundle>();
        for (var implementation : readImplementations()) {
            var implCls = context.getHierarchy().getClassSource().get(implementation);
            if (implCls != null) {
                if (implCls.getMethod(INIT) != null) {
                    bundles.add(new Bundle(implementation, implementation, null));
                }
                continue;
            }
            var resource = resourceProvider.getResource(implementation.replace('.', '/') + ".properties");
            if (resource != null) {
                var properties = new Properties();
                try (var input = resource.open()) {
                    properties.load(input);
                } catch (IOException e) {
                    // do nothing
                }
                bundles.add(new Bundle(implementation, null, properties));
            }
        }
        return bundles;
    }

    private Set<String> readImplementations() {
        var resources = resourceProvider.getResources("META-INF/services/java.util.ResourceBundle");
        var implementations = new LinkedHashSet<String>();
        while (resources.hasNext()) {
            var resource = resources.next();
            try (var reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                while (true) {
                    var line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        implementations.add(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return implementations;
    }

    private void generateBundleNames(MethodHolder method, List<Bundle> bundles,
            ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var array = pe.constructArray(String.class, bundles.size());
        for (var i = 0; i < bundles.size(); ++i) {
            array.setElement(i, pe.constant(bundles.get(i).name));
        }
        array.returnValue();
    }

    private void generateCreateBundle(MethodHolder method, List<Bundle> bundles,
            ClassHolderTransformerContext context) {
        method.getModifiers().remove(ElementModifier.NATIVE);
        var pe = ProgramEmitter.create(method, context.getHierarchy());
        var choice = pe.choice(pe.var(1, int.class));
        for (var i = 0; i < bundles.size(); ++i) {
            var bundle = bundles.get(i);
            choice.option(i, () -> createBundle(pe, bundle).returnValue());
        }
        choice.otherwise(() -> pe.constantNull(ResourceBundle.class).returnValue());
    }

    private ValueEmitter createBundle(ProgramEmitter pe, Bundle bundle) {
        if (bundle.className != null) {
            return pe.construct(bundle.className).cast(ResourceBundle.class);
        }

        // Properties is a hash table, sort keys to emit them in deterministic order
        var keys = new ArrayList<String>();
        for (var propertyName : bundle.properties.keySet()) {
            if (propertyName instanceof String && bundle.properties.getProperty((String) propertyName) != null) {
                keys.add((String) propertyName);
            }
        }
        Collections.sort(keys);
        var array = pe.constructArray(String.class, keys.size() * 2);
        for (var i = 0; i < keys.size(); ++i) {
            var key = keys.get(i);
            array.setElement(i * 2, pe.constant(key));
            array.setElement(i * 2 + 1, pe.constant(bundle.properties.getProperty(key)));
        }
        return pe.invoke(CREATE_PROPERTIES_BUNDLE, array);
    }

    private static class Bundle {
        final String name;
        final String className;
        final Properties properties;

        Bundle(String name, String className, Properties properties) {
            this.name = name;
            this.className = className;
            this.properties = properties;
        }
    }
}
