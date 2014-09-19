package org.teavm.eclipse;

import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Alexey Andreev
 */
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

    boolean isMinifying();

    void setMinifying(boolean minifying);

    TeaVMRuntimeMode getRuntimeMode();

    void setRuntimeMode(TeaVMRuntimeMode runtimeMode);

    boolean isIncremental();

    void setIncremental(boolean incremental);

    String getCacheDirectory();

    void setCacheDirectory(String cacheDirectory);

    boolean isSourceMapsGenerated();

    void setSourceMapsGenerated(boolean sourceMapsGenerated);

    boolean isDebugInformationGenerated();

    void setDebugInformationGenerated(boolean debugInformationGenerated);

    Properties getProperties();

    void setProperties(Properties properties);

    String[] getTransformers();

    void setTransformers(String[] transformers);

    Map<String, String> getClassAliases();

    void setClassAliases(Map<String, String> classAliases);
}
