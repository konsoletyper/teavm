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
package org.teavm.tooling.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FileSystemWatcher {
    private WatchService watchService;
    private Map<WatchKey, Path> keysToPath = new HashMap<>();
    private Map<Path, WatchKey> pathsToKey = new HashMap<>();
    private Set<File> changedFiles = new LinkedHashSet<>();

    public FileSystemWatcher(String[] classPath) throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        for (String entry : classPath) {
            Path path = Paths.get(entry);
            File file = path.toFile();
            if (file.exists()) {
                if (!file.isDirectory()) {
                    registerSingle(path.getParent());
                } else {
                    register(path);
                }
            }
        }
    }

    public void dispose() throws IOException {
        watchService.close();
    }

    private List<Path> register(Path path) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerSingle(dir);
                files.addAll(Arrays.stream(dir.toFile().listFiles(File::isFile)).map(File::toPath)
                        .collect(Collectors.toList()));
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private void registerSingle(Path path) throws IOException {
        WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keysToPath.put(key, path);
        pathsToKey.put(path, key);
    }

    public boolean hasChanges() throws IOException {
        return !changedFiles.isEmpty() || pollNow();
    }

    public void waitForChange(int timeout) throws InterruptedException, IOException {
        if (!hasChanges()) {
            take();
        }
        while (poll(timeout)) {
            // continue polling
        }
        while (pollNow()) {
            // continue polling
        }
    }

    public List<File> grabChangedFiles() {
        List<File> result = new ArrayList<>(changedFiles);
        changedFiles.clear();
        return result;
    }

    private void take() throws InterruptedException, IOException {
        while (true) {
            WatchKey key = watchService.take();
            if (key != null && filter(key)) {
                return;
            }
        }
    }

    private boolean poll(int milliseconds) throws IOException, InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        while (true) {
            int timeToWait = (int) (end - System.currentTimeMillis());
            if (timeToWait <= 0) {
                return false;
            }
            WatchKey key = watchService.poll(timeToWait, TimeUnit.MILLISECONDS);
            if (key == null) {
                continue;
            }
            if (filter(key)) {
                return true;
            }
        }
    }

    private boolean pollNow() throws IOException {
        WatchKey key = watchService.poll();
        if (key == null) {
            return false;
        }
        return filter(key);
    }

    private boolean filter(WatchKey key) throws IOException {
        boolean hasNew = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            List<Path> paths = filter(key, event);
            if (!paths.isEmpty()) {
                changedFiles.addAll(paths.stream().map(Path::toFile).collect(Collectors.toList()));
                hasNew = true;
            }
        }
        key.reset();
        return hasNew;
    }

    private List<Path> filter(WatchKey baseKey, WatchEvent<?> event) throws IOException {
        if (!(event.context() instanceof Path)) {
            return Collections.emptyList();
        }
        Path basePath = keysToPath.get(baseKey);
        Path path = basePath.resolve((Path) event.context());
        WatchKey key = pathsToKey.get(path);

        List<Path> result = new ArrayList<>();
        result.add(path);

        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            if (key != null) {
                pathsToKey.remove(path);
                keysToPath.remove(key);
                key.cancel();
            }
        } else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            if (Files.isDirectory(path)) {
                result.addAll(register(path));
            }
        }
        return result;
    }
}
