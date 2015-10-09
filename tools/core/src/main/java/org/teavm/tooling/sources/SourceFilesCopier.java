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
package org.teavm.tooling.sources;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.teavm.model.ClassReader;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMToolLog;

/**
 *
 * @author Alexey Andreev
 */
public class SourceFilesCopier {
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private List<SourceFileProvider> sourceFileProviders;
    private Set<String> sourceFiles = new HashSet<>();

    public SourceFilesCopier(List<SourceFileProvider> sourceFileProviders) {
        this.sourceFileProviders = sourceFileProviders;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public void addClasses(ListableClassReaderSource classReader) {
        ProgramSourceAggregator sourceAggregator = new ProgramSourceAggregator(sourceFiles);
        for (String className : classReader.getClassNames()) {
            ClassReader cls = classReader.get(className);
            for (MethodReader method : cls.getMethods()) {
                if (method.getProgram() == null) {
                    continue;
                }
                sourceAggregator.addLocationsOfProgram(method.getProgram());
            }
        }
    }

    public void copy(File targetDirectory) {
        for (SourceFileProvider provider : sourceFileProviders) {
            try {
                provider.open();
            } catch (IOException e) {
                log.warning("Error opening source file provider", e);
            }
        }
        targetDirectory.mkdirs();
        for (String fileName : sourceFiles) {
            try (InputStream input = findSourceFile(fileName)) {
                if (input != null) {
                    File outputFile = new File(targetDirectory, fileName);
                    outputFile.getParentFile().mkdirs();
                    try (OutputStream output = new FileOutputStream(outputFile)) {
                        IOUtils.copy(input, output);
                    }
                } else {
                    log.info("Missing source file: " + fileName);
                }
            } catch (IOException e) {
                log.warning("Could not copy source file " + fileName, e);
            }
        }
        for (SourceFileProvider provider : sourceFileProviders) {
            try {
                provider.close();
            } catch (IOException e) {
                log.warning("Error closing source file provider", e);
            }
        }
    }

    private InputStream findSourceFile(String fileName) throws IOException {
        for (SourceFileProvider provider : sourceFileProviders) {
            InputStream input = provider.openSourceFile(fileName);
            if (input != null) {
                return input;
            }
        }
        return null;
    }
}
