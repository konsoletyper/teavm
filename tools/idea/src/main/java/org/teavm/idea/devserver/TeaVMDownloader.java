/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.idea.devserver;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.Application;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class TeaVMDownloader implements Closeable {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private String version;
    private Thread downloadThread;
    private ConsoleView console;
    private Application application;

    public TeaVMDownloader(String version, ConsoleView console, Application application) {
        this.version = version;
        this.console = console;
        this.application = application;
    }

    @Override
    public void close() {
        if (downloadThread != null) {
            downloadThread.interrupt();
            downloadThread = null;
        }
    }

    public void downloadAndStart(Consumer<Boolean> onComplete) {
        if (hasLocally()) {
            onComplete.accept(true);
        } else {
            download(onComplete);
        }
    }

    public boolean hasLocally() {
        return localPath().isDirectory() && new File(localPath(), "downloaded").isFile();
    }

    public void download(Consumer<Boolean> onComplete) {
        downloadThread = new Thread(() -> {
            log("Downloading TeaVM Dev Server " + version + " ...");
            var client = HttpClient.newBuilder().build();
            try {
                localPath().mkdirs();
                var fileList = downloadFileList(client).lines().toList();
                log("Artifacts to download: " + fileList.size());
                for (var i = 0; i < fileList.size(); ++i) {
                    downloadFile(client, fileList.get(i));
                    log("Downloaded " + (i + 1) + " of " + fileList.size() + ": " + fileList.get(i));
                }
                new File(localPath(), "downloaded").createNewFile();
                log("Done");
                application.invokeLater(() -> {
                    onComplete.accept(true);
                });
            } catch (InterruptedException e) {
                // do nothing
            } catch (IOException | URISyntaxException | RuntimeException e) {
                var writer = new StringWriter();
                var printWriter = new PrintWriter(writer);
                e.printStackTrace(printWriter);
                printWriter.flush();
                console.print("Error downloading TeaVM Dev Server: " + writer, ConsoleViewContentType.ERROR_OUTPUT);
                application.invokeLater(() -> {
                    onComplete.accept(false);
                });
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.setName("TeaVM Dev Server Downloader");
        downloadThread.start();
    }

    public File localPath() {
        return new File(System.getProperty("user.home"), ".teavm/dev-server/" + version);
    }

    private String downloadFileList(HttpClient client) throws IOException, URISyntaxException, InterruptedException {
        var path = "org/teavm/teavm-devserver-runner/" + version + "/teavm-devserver-runner-" + version
                + "-dependencies.txt";
        if (version.endsWith("-SNAPSHOT")) {
            var localPath = System.getProperty("user.home") + "/.m2/repository/" + path;
            return Files.readString(Paths.get(localPath));
        }
        var repo = getTeaVMRepo();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(repo + path))
                .build();
        var resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed to download file list from " + repo + path
                    + ": " + resp.statusCode());
        }
        return resp.body();
    }

    private void downloadFile(HttpClient client, String coordinates) throws InterruptedException,
            IOException, URISyntaxException {
        var parts = coordinates.split(":");
        var groupId = parts[0];
        var artifactId = parts[1];
        var version = parts[2];
        var path = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId
                + "-" + version + ".jar";
        var outPath = localPath().toPath().resolve(artifactId + ".jar");
        if (groupId.equals("org.teavm") && version.endsWith("-SNAPSHOT")) {
            var pathInLocalRepository = System.getProperty("user.home") + "/.m2/repository/" + path;
            Files.copy(Paths.get(pathInLocalRepository), outPath);
            return;
        }
        var repo = groupId.endsWith("org.teavm") ? getTeaVMRepo() : MAVEN_CENTRAL;
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(repo + path))
                .build();
        var resp = client.send(request, HttpResponse.BodyHandlers.ofFile(outPath));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Failed to download file from " + repo + path
                    + ": " + resp.statusCode());
        }
    }

    private String getTeaVMRepo() {
        var repo = MAVEN_CENTRAL;
        if (version.contains("-dev-")) {
            repo = "https://teavm.org/maven/repository/";
        }
        return repo;
    }

    private void log(String text) {
        console.print(text + System.lineSeparator(), ConsoleViewContentType.NORMAL_OUTPUT);
    }
}
