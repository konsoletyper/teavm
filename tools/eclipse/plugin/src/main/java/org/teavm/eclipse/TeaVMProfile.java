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
package org.teavm.eclipse;

import java.util.Properties;
import java.util.Set;

public interface TeaVMProfile {
    String getName();

    void setName(String name);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getMainClass();

    void setMainClass(String mainClass);

    String getTargetDirectory();

    void setTargetDirectory(String targetDirectory);

    String getTargetFileName();

    void setTargetFileName(String targetFileName);

    boolean isIncremental();

    void setIncremental(boolean incremental);

    String getCacheDirectory();

    void setCacheDirectory(String cacheDirectory);

    boolean isSourceMapsGenerated();

    void setSourceMapsGenerated(boolean sourceMapsGenerated);

    boolean isDebugInformationGenerated();

    void setDebugInformationGenerated(boolean debugInformationGenerated);

    boolean isSourceFilesCopied();

    void setSourceFilesCopied(boolean sourceFilesCopied);

    Properties getProperties();

    void setProperties(Properties properties);

    String[] getTransformers();

    void setTransformers(String[] transformers);

    Set<? extends String> getClassesToPreserve();

    void setClassesToPreserve(Set<? extends String> classesToPreserve);

    String getExternalToolId();

    void setExternalToolId(String toolId);
}
