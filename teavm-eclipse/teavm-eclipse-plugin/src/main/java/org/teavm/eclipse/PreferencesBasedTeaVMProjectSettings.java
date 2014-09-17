package org.teavm.eclipse;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class PreferencesBasedTeaVMProjectSettings implements TeaVMProjectSettings {
    public static final String ENABLED = "enabled";
    public static final String MAIN_CLASS = "mainClass";
    public static final String TARGET_DIRECTORY = "targetDirectory";
    public static final String TARGET_FILE_NAME = "targetFileName";
    public static final String RUNTIME = "runtime";
    public static final String MINIFYING = "minifying";
    public static final String INCREMENTAL = "incremental";
    public static final String CACHE_DIRECTORY = "cacheDirectory";
    public static final String SOURCE_MAPS = "sourceMaps";
    public static final String DEBUG_INFORMATION = "debugInformation";
    public static final String PROPERTIES = "properties";

    private static final String NEW_PROFILE_NAME = "New profile";
    private List<ProfileImpl> profiles = new ArrayList<>();
    private Map<String, ProfileImpl> profileMap = new HashMap<>();
    private IEclipsePreferences globalPreferences;
    private String projectName;

    public PreferencesBasedTeaVMProjectSettings(IProject project) {
        ProjectScope scope = new ProjectScope(project);
        globalPreferences = scope.getNode(TeaVMEclipsePlugin.ID);
        projectName = project.getName();
    }

    @Override
    public TeaVMProfile[] getProfiles() {
        return profiles.toArray(new TeaVMProfile[profiles.size()]);
    }

    @Override
    public TeaVMProfile getProfile(String name) {
        return profileMap.get(name);
    }

    @Override
    public void deleteProfile(TeaVMProfile profile) {
        if (profileMap.get(profile.getName()) != profile) {
            return;
        }
        profileMap.remove(profile);
        profiles.remove(profile);
    }

    @Override
    public TeaVMProfile createProfile() {
        String name = NEW_PROFILE_NAME;
        if (profileMap.containsKey(name)) {
            int i = 1;
            do {
                name = NEW_PROFILE_NAME + " (" + i++ + ")";
            } while (profileMap.containsKey(name));
        }
        ProfileImpl profile = new ProfileImpl();
        profile.name = name;
        IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
        profile.setEnabled(true);
        profile.setTargetDirectory(varManager.generateVariableExpression("workspace_loc", "/" + projectName));
        profile.setTargetFileName("classes.js");
        profile.setMinifying(true);
        profile.setIncremental(false);
        profile.setCacheDirectory(varManager.generateVariableExpression("workspace_loc", "/" + projectName));
        profile.setSourceMapsGenerated(true);
        profile.setDebugInformationGenerated(true);
        profiles.add(profile);
        profileMap.put(name, profile);
        return profile;
    }

    @Override
    public void save() throws CoreException {
        try {
            for (ProfileImpl profile : profiles) {
                profile.preferences = globalPreferences.node(profile.name);
                profile.save();
            }
            for (String key : globalPreferences.childrenNames()) {
                if (!profileMap.containsKey(key)) {
                    globalPreferences.node(key).removeNode();
                }
            }
            globalPreferences.flush();
        } catch (BackingStoreException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }

    @Override
    public void load() throws CoreException {
        try {
            globalPreferences.sync();
            for (String nodeName : globalPreferences.childrenNames()) {
                ProfileImpl profile = profileMap.get(nodeName);
                if (profile == null) {
                    profile = new ProfileImpl();
                    profile.name = nodeName;
                    profileMap.put(nodeName, profile);
                    profiles.add(profile);
                }
                profile.preferences = globalPreferences.node(nodeName);
                profile.load();
            }
            for (int i = 0; i < profiles.size(); ++i) {
                ProfileImpl profile = profiles.get(i);
                if (!globalPreferences.nodeExists(profile.name)) {
                    profiles.remove(i--);
                    profileMap.remove(profile.name);
                }
            }
        } catch (BackingStoreException e) {
            throw new CoreException(TeaVMEclipsePlugin.makeError(e));
        }
    }

    private class ProfileImpl implements TeaVMProfile {
        Preferences preferences;
        String name;
        private boolean enabled;
        private String mainClass;
        private String targetDirectory;
        private String targetFileName;
        private boolean minifying;
        private TeaVMRuntimeMode runtimeMode = TeaVMRuntimeMode.SEPARATE;
        private boolean incremental;
        private String cacheDirectory;
        private boolean sourceMapsGenerated;
        private boolean debugInformationGenerated;
        private Properties properties = new Properties();

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            ProfileImpl existingProfile = profileMap.get(name);
            if (existingProfile != null && existingProfile != this) {
                throw new IllegalArgumentException("Profile " + name + " already exists");
            }
            profileMap.remove(this.name);
            this.name = name;
            profileMap.put(name, this);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public String getMainClass() {
            return mainClass;
        }

        @Override
        public void setMainClass(String mainClass) {
            this.mainClass = mainClass;
        }

        @Override
        public String getTargetDirectory() {
            return targetDirectory;
        }

        @Override
        public void setTargetDirectory(String targetDirectory) {
            this.targetDirectory = targetDirectory;
        }

        @Override
        public String getTargetFileName() {
            return targetFileName;
        }

        @Override
        public void setTargetFileName(String targetFileName) {
            this.targetFileName = targetFileName;
        }

        @Override
        public boolean isMinifying() {
            return minifying;
        }

        @Override
        public void setMinifying(boolean minifying) {
            this.minifying = minifying;
        }

        @Override
        public TeaVMRuntimeMode getRuntimeMode() {
            return runtimeMode;
        }

        @Override
        public void setRuntimeMode(TeaVMRuntimeMode runtimeMode) {
            this.runtimeMode = runtimeMode;
        }

        @Override
        public boolean isIncremental() {
            return incremental;
        }

        @Override
        public void setIncremental(boolean incremental) {
            this.incremental = incremental;
        }

        @Override
        public String getCacheDirectory() {
            return cacheDirectory;
        }

        @Override
        public void setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
        }

        @Override
        public boolean isSourceMapsGenerated() {
            return sourceMapsGenerated;
        }

        @Override
        public void setSourceMapsGenerated(boolean sourceMapsGenerated) {
            this.sourceMapsGenerated = sourceMapsGenerated;
        }

        @Override
        public boolean isDebugInformationGenerated() {
            return debugInformationGenerated;
        }

        @Override
        public void setDebugInformationGenerated(boolean debugInformationGenerated) {
            this.debugInformationGenerated = debugInformationGenerated;
        }

        @Override
        public Properties getProperties() {
            Properties copy = new Properties();
            copy.putAll(properties);
            return copy;
        }

        @Override
        public void setProperties(Properties properties) {
            this.properties = new Properties();
            this.properties.putAll(properties);
        }

        public void load() throws BackingStoreException {
            preferences.sync();
            enabled = preferences.getBoolean(ENABLED, true);
            mainClass = preferences.get(MAIN_CLASS, "");
            targetDirectory = preferences.get(TARGET_DIRECTORY, "");
            targetFileName = preferences.get(TARGET_FILE_NAME, "");
            minifying = preferences.getBoolean(MINIFYING, true);
            runtimeMode = TeaVMRuntimeMode.valueOf(preferences.get(RUNTIME, TeaVMRuntimeMode.SEPARATE.name()));
            incremental = preferences.getBoolean(INCREMENTAL, false);
            cacheDirectory = preferences.get(CACHE_DIRECTORY, "");
            sourceMapsGenerated = preferences.getBoolean(SOURCE_MAPS, true);
            debugInformationGenerated = preferences.getBoolean(DEBUG_INFORMATION, true);
            Preferences propertiesPrefs = preferences.node(PROPERTIES);
            propertiesPrefs.sync();
            properties = new Properties();
            for (String key : propertiesPrefs.keys()) {
                properties.setProperty(key, propertiesPrefs.get(key, ""));
            }
        }

        public void save() throws BackingStoreException {
            preferences.clear();
            preferences.putBoolean(ENABLED, enabled);
            preferences.put(MAIN_CLASS, mainClass);
            preferences.put(TARGET_DIRECTORY, targetDirectory);
            preferences.put(TARGET_FILE_NAME, targetFileName);
            preferences.putBoolean(MINIFYING, minifying);
            preferences.put(RUNTIME, runtimeMode.name());
            preferences.putBoolean(INCREMENTAL, incremental);
            preferences.put(CACHE_DIRECTORY, cacheDirectory);
            preferences.putBoolean(SOURCE_MAPS, sourceMapsGenerated);
            preferences.putBoolean(DEBUG_INFORMATION, debugInformationGenerated);
            Preferences propertiesPrefs = preferences.node(PROPERTIES);
            propertiesPrefs.clear();
            for (Object key : properties.keySet()) {
                propertiesPrefs.put((String)key, properties.getProperty((String)key));
            }
            propertiesPrefs.flush();
            preferences.flush();
        }
    }
}
