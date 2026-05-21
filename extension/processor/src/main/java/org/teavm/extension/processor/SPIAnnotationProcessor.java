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
package org.teavm.extension.processor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("org.teavm.extension.Autoregistered")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class SPIAnnotationProcessor extends AbstractProcessor {
    private static final String[] SUPPORTED_SERVICES = {
        "org.teavm.extension.spi.reflection.ReflectionPolicy",
        "org.teavm.extension.spi.resources.ResourcesPolicy",
        "org.teavm.metaprogramming.MetaprogrammingProvider"
    };

    private final Map<String, Set<String>> serviceProviders = new LinkedHashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var annotation : annotations) {
            for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement) {
                    processType((TypeElement) element);
                }
            }
        }
        if (roundEnv.processingOver()) {
            writeServiceFiles();
        }
        return false;
    }

    private void processType(TypeElement typeElement) {
        var types = processingEnv.getTypeUtils();
        var elements = processingEnv.getElementUtils();
        for (var serviceName : SUPPORTED_SERVICES) {
            var serviceType = elements.getTypeElement(serviceName);
            if (serviceType != null && types.isAssignable(typeElement.asType(), serviceType.asType())) {
                serviceProviders.computeIfAbsent(serviceName, k -> new LinkedHashSet<>())
                        .add(typeElement.getQualifiedName().toString());
            }
        }
    }

    private void writeServiceFiles() {
        for (var entry : serviceProviders.entrySet()) {
            var serviceName = entry.getKey();
            var providers = entry.getValue();
            try {
                var file = processingEnv.getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + serviceName);
                try (var writer = file.openWriter()) {
                    for (var provider : providers) {
                        writer.write(provider);
                        writer.write('\n');
                    }
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to write service file for " + serviceName + ": " + e.getMessage());
            }
        }
    }
}
