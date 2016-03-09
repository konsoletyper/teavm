/*
 *  Copyright 2016 MJ.
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

package org.teavm.gradle.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.tooling.RuntimeCopyOperation;

/** POJO file with TeaVM plugin settings.
 *
 * @author MJ
 * @see <a href=
 *      "https://github.com/konsoletyper/teavm/wiki/Building-JavaScript-with-Maven-and-TeaVM#the-build-javascript-goal">
 *      Maven properties</a> */
public class TeaVMExtension {
    private boolean includeDependencies = true;
    private String version = "0.4.3";
    private String targetDirectory = "javascript";
    private String targetFileName = "classes.js";
    private boolean minifying = true;
    private boolean debugInformationGenerated;
    private boolean sourceMapsGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private String cacheDirectory = "teavm-cache";
    private Map<String, String> properties = new HashMap<String, String>();
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;
    private boolean mainPageIncluded;
    private List<String> transformers;
    private Map<String, String> classAliases = new HashMap<String, String>();
    private List<Map<String, Object>> methodAliases = new ArrayList<Map<String, Object>>();

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(final boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(final String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(final String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(final boolean minifying) {
        this.minifying = minifying;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(final boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public boolean isSourceMapsGenerated() {
        return sourceMapsGenerated;
    }

    public void setSourceMapsGenerated(final boolean sourceMapsGenerated) {
        this.sourceMapsGenerated = sourceMapsGenerated;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(final boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(final boolean incremental) {
        this.incremental = incremental;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(final String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    public RuntimeCopyOperation getRuntime() {
        return runtime;
    }

    public void setRuntime(final RuntimeCopyOperation runtime) {
        this.runtime = runtime;
    }

    public boolean isMainPageIncluded() {
        return mainPageIncluded;
    }

    public void setMainPageIncluded(final boolean mainPageIncluded) {
        this.mainPageIncluded = mainPageIncluded;
    }

    public List<String> getTransformers() {
        return transformers;
    }

    public void setTransformers(final List<String> transformers) {
        this.transformers = transformers;
    }

    public Map<String, String> getClassAliases() {
        return classAliases;
    }

    public void setClassAliases(final Map<String, String> classAliases) {
        this.classAliases = classAliases;
    }

    public List<Map<String, Object>> getMethodAliases() {
        return methodAliases;
    }

    public void setMethodAliases(final List<Map<String, Object>> methodAliases) {
        this.methodAliases = methodAliases;
    }
}
