/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.idea.devserver.DevServerRunner;
import org.teavm.tooling.daemon.BuildDaemon;

public final class DaemonUtil {
    private static final Set<String> PLUGIN_FILES = new HashSet<>(Arrays.asList("teavm-jps-common.jar",
            "teavm-plugin.jar", "teavm.jar"));
    private static final String DAEMON_CLASS = BuildDaemon.class.getName().replace('.', '/') + ".class";
    private static final String DEV_SERVER_CLASS = DevServerRunner.class.getName().replace('.', '/') + ".class";
    private static final int DAEMON_CLASS_DEPTH;
    private static final int DEV_SERVER_CLASS_DEPTH;

    static {
        int depth = 0;
        for (int i = 0; i < DAEMON_CLASS.length(); ++i) {
            if (DAEMON_CLASS.charAt(i) == '/') {
                depth++;
            }
        }
        DAEMON_CLASS_DEPTH = depth;

        depth = 0;
        for (int i = 0; i < DEV_SERVER_CLASS.length(); ++i) {
            if (DEV_SERVER_CLASS.charAt(i) == '/') {
                depth++;
            }
        }
        DEV_SERVER_CLASS_DEPTH = depth;
    }

    private DaemonUtil() {
    }

    public static List<String> detectClassPath() {
        PluginId id = PluginId.getId("org.teavm.idea");
        for (IdeaPluginDescriptor plugin : PluginManager.getPlugins()) {
            if (plugin.getPluginId().compareTo(id) == 0) {
                Set<File> visited = new HashSet<>();
                List<String> classPath = new ArrayList<>();
                findInHierarchy(plugin.getPath(), classPath, visited);
                return classPath;
            }
        }
        return Collections.emptyList();
    }

    private static void findInHierarchy(File file, List<String> targetFiles, Set<File> visited) {
        if (!visited.add(file)) {
            return;
        }
        if (file.isFile() && PLUGIN_FILES.contains(file.getName())) {
            targetFiles.add(file.getAbsolutePath());
        } else if (file.getPath().endsWith(DAEMON_CLASS)) {
            for (int i = 0; i <= DAEMON_CLASS_DEPTH; ++i) {
                file = file.getParentFile();
            }
            targetFiles.add(file.getAbsolutePath());
        } else if (file.getPath().endsWith(DEV_SERVER_CLASS)) {
            for (int i = 0; i <= DEV_SERVER_CLASS_DEPTH; ++i) {
                file = file.getParentFile();
            }
            targetFiles.add(file.getAbsolutePath());
        } else if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                findInHierarchy(childFile, targetFiles, visited);
            }
        }
    }
}
