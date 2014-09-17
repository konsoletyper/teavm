package org.teavm.eclipse;

import org.eclipse.core.runtime.CoreException;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface TeaVMProjectSettings {
    TeaVMProfile[] getProfiles();

    TeaVMProfile getProfile(String name);

    void deleteProfile(TeaVMProfile profile);

    TeaVMProfile createProfile();

    void save() throws CoreException;

    void load() throws CoreException;
}
