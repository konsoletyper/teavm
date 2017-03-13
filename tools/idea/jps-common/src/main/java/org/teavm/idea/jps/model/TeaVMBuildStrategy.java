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
package org.teavm.idea.jps.model;

import java.util.List;
import org.teavm.tooling.TeaVMTargetType;

public interface TeaVMBuildStrategy {
    void init();

    void addSourcesDirectory(String directory);

    void addSourcesJar(String jarFile);

    void setClassPathEntries(List<String> entries);

    void setTargetType(TeaVMTargetType targetType);

    void setMainClass(String mainClass);

    void setTargetDirectory(String targetDirectory);

    void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated);

    void setDebugInformationGenerated(boolean debugInformationGenerated);

    void setSourceFilesCopied(boolean sourceFilesCopied);

    TeaVMBuildResult build();
}
