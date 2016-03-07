package org.teavm.gradle.extension;

import org.teavm.tooling.RuntimeCopyOperation;

public class TeaVMExtension {
    private String targetDirectory = "dist";
    private String cacheDirectory = "teavm-cache";
    private String targetFile = "app.js";
    private String version = "0.4.3";
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;
    private boolean minify = true;
    private boolean debugInfo = true;
    private boolean sourceMaps = true;
    private boolean sourceFilesCopied = true;
    private boolean mainPageIncluded = true;

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(final String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(final String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(final String targetFile) {
        this.targetFile = targetFile;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public RuntimeCopyOperation getRuntime() {
        return runtime;
    }

    public void setRuntime(final RuntimeCopyOperation runtime) {
        this.runtime = runtime;
    }

    public boolean isMinify() {
        return minify;
    }

    public void setMinify(final boolean minify) {
        this.minify = minify;
    }

    public boolean isDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(final boolean debugInfo) {
        this.debugInfo = debugInfo;
    }

    public boolean isSourceMaps() {
        return sourceMaps;
    }

    public void setSourceMaps(final boolean sourceMaps) {
        this.sourceMaps = sourceMaps;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(final boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public boolean isMainPageIncluded() {
        return mainPageIncluded;
    }

    public void setMainPageIncluded(final boolean mainPageIncluded) {
        this.mainPageIncluded = mainPageIncluded;
    }
}
