package org.teavm.eclipse;

import org.eclipse.core.runtime.CoreException;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface TeaVMProjectSettings {
    String getMainClass();

    void setMainClass(String mainClass);

    String getTargetDirectory();

    void setTargetDirectory(String targetDirectory);

    void save() throws CoreException;

    void load() throws CoreException;
}
