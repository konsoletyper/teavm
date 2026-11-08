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
package org.teavm.buildutil;

import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers {@code localProperties} extension, see {@link LocalPropertiesExtension}.
 */
public class LocalPropertiesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var commandLineProperties = Map.copyOf(project.getGradle().getStartParameter().getProjectProperties());
        var file = project.getLayout().getProjectDirectory().file(LocalPropertiesExtension.FILE_NAME);
        project.getExtensions().add(LocalPropertiesExtension.class, "localProperties",
                new LocalPropertiesExtension(project.getProviders(), commandLineProperties, file));
    }
}
