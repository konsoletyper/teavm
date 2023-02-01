/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.buildutil;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

public class JavaVersionPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var ext = new ExtensionImpl();
        project.getExtensions().add(JavaVersionExtension.class, "javaVersion", ext);
        project.afterEvaluate(p -> {
            var java = p.getExtensions().findByType(JavaPluginExtension.class);
            if (java != null) {
                if (ext.version != null) {
                    java.setSourceCompatibility(ext.version);
                    java.setTargetCompatibility(ext.version);
                }
            }
        });
    }

    private static class ExtensionImpl implements JavaVersionExtension {
        private JavaVersion version = JavaVersion.VERSION_11;

        @Override
        public JavaVersion getVersion() {
            return version;
        }

        @Override
        public void setVersion(JavaVersion version) {
            this.version = version;
        }
    }
}
