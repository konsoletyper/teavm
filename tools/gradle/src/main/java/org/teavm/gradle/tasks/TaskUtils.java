/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.gradle.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.teavm.gradle.api.SourceFilePolicy;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.builder.BuildStrategy;

final class TaskUtils {
    private TaskUtils() {
    }

    static void applySourceFiles(ConfigurableFileCollection sourceFiles, BuildStrategy builder) {
        for (var file : sourceFiles) {
            if (file.isFile()) {
                if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                    builder.addSourcesJar(file.getAbsolutePath());
                }
            } else if (file.isDirectory()) {
                builder.addSourcesDirectory(file.getAbsolutePath());
            }
        }
    }

    static void applySourceFilePolicy(Property<SourceFilePolicy> policy, BuildStrategy builder) {
        switch (policy.get()) {
            case DO_NOTHING:
                builder.setSourceFilePolicy(TeaVMSourceFilePolicy.DO_NOTHING);
                break;
            case COPY:
                builder.setSourceFilePolicy(TeaVMSourceFilePolicy.COPY);
                break;
            case LINK_LOCAL_FILES:
                builder.setSourceFilePolicy(TeaVMSourceFilePolicy.LINK_LOCAL_FILES);
                break;
        }
    }
}
