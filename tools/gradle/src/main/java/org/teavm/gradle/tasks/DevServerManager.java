/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.gradle.tasks;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

public final class DevServerManager {
    private static DevServerManager instance;
    private final ConcurrentMap<String, ProjectDevServerManager> projectManagers = new ConcurrentHashMap<>();

    private DevServerManager() {
    }

    public ProjectDevServerManager getProjectManager(String path) {
        return projectManagers.computeIfAbsent(path, this::createProjectManager);
    }

    private ProjectDevServerManager createProjectManager(String path) {
        return new ProjectDevServerManager();
    }

    public void cleanup(Gradle gradle) {
        var allProjectPaths = new HashSet<String>();
        collectProjects(gradle.getRootProject(), allProjectPaths);
        var keysToRemove = new HashSet<>(projectManagers.keySet());
        keysToRemove.removeAll(allProjectPaths);
        for (var path : keysToRemove) {
            var pm = projectManagers.remove(path);
            if (pm != null) {
                pm.stop(gradle.getRootProject().getLogger());
            }
        }
    }

    public static DevServerManager instance() {
        if (instance == null) {
            instance = new DevServerManager();
        }
        return instance;
    }

    private static void collectProjects(Project project, Set<String> collector) {
        if (!collector.add(project.getPath())) {
            return;
        }
        for (var child : project.getChildProjects().values()) {
            collectProjects(child, collector);
        }
    }
}
