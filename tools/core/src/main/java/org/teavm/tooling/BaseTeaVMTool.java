/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.tooling;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.tooling.sources.SourceFileProvider;

public interface BaseTeaVMTool {
    void setTargetDirectory(File targetDirectory);

    void setMinifying(boolean minifying);

    void setIncremental(boolean incremental);

    void setDebugInformationGenerated(boolean debugInformationGenerated);

    void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated);

    void setSourceFilesCopied(boolean sourceFilesCopied);

    Properties getProperties();

    List<ClassHolderTransformer> getTransformers();

    void setLog(TeaVMToolLog log);

    void setClassLoader(ClassLoader classLoader);

    void addSourceFileProvider(SourceFileProvider sourceFileProvider);
}
