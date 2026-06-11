/*
 *  Copyright 2025 Alexey Andreev.
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

package org.teavm.buildutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Runs tests on a remote host; result is console output, not a cached artifact")
public abstract class RunMacRemoteTestsTask extends DefaultTask {
    @Input
    public abstract Property<String> getMacHost();

    @Input
    public abstract Property<String> getMacUser();

    @Input
    @Optional
    public abstract Property<String> getMacPassword();

    @InputFile
    public abstract RegularFileProperty getClassListFile();

    @Internal
    public abstract DirectoryProperty getCTestOutputDir();

    @Input
    @Optional
    public abstract Property<String> getTestFilter();

    @TaskAction
    public void run() throws IOException, InterruptedException {
        var cOutputDir = getCTestOutputDir().get().getAsFile().toPath();
        var rawLines = Files.readAllLines(getClassListFile().get().getAsFile().toPath(), StandardCharsets.UTF_8);

        List<Path> classDirPaths = new ArrayList<>();
        var seen = new LinkedHashSet<String>();
        for (var line : rawLines) {
            var trimmed = line.strip();
            if (!trimmed.isEmpty() && seen.add(trimmed)) {
                classDirPaths.add(Path.of(trimmed));
            }
        }

        if (classDirPaths.isEmpty()) {
            classDirPaths = scanOutputDir(cOutputDir);
            if (classDirPaths.isEmpty()) {
                getLogger().lifecycle("No C test classes found. Run :tests:test with -Pteavm.tests.c=true first.");
                return;
            }
        }

        var allTests = new LinkedHashMap<String, List<TestEntry>>();
        for (var classDir : classDirPaths) {
            var relPath = cOutputDir.relativize(classDir).toString().replace('\\', '/');
            allTests.put(relPath, parseTestsJson(classDir.resolve("tests.json")));
        }

        var archive = createArchive(cOutputDir, classDirPaths);
        var script = generateScript(allTests, archive);
        executeViaSSH(script);
    }

    private List<Path> scanOutputDir(Path cOutputDir) throws IOException {
        if (!Files.isDirectory(cOutputDir)) {
            return List.of();
        }
        var filter = getTestFilter().getOrNull();
        var result = new ArrayList<Path>();
        try (var stream = Files.walk(cOutputDir)) {
            stream.filter(p -> p.getFileName().toString().equals("tests.json"))
                    .map(Path::getParent)
                    .filter(p -> matchesFilter(cOutputDir.relativize(p).toString().replace('\\', '/'), filter))
                    .sorted()
                    .forEach(result::add);
        }
        return result;
    }

    private static boolean matchesFilter(String relPath, String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        var classPattern = filter.replace('.', '/');
        if (classPattern.endsWith("/*")) {
            return relPath.startsWith(classPattern.substring(0, classPattern.length() - 1));
        }
        if (classPattern.endsWith("*")) {
            return relPath.startsWith(classPattern.substring(0, classPattern.length() - 1));
        }
        return relPath.equals(classPattern);
    }

    private byte[] createArchive(Path cOutputDir, List<Path> classDirPaths)
            throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add("tar");
        command.add("czf");
        command.add("-");
        command.add("--exclude=run_test");
        command.add("--exclude=*.o");
        command.add("--exclude=*.d");
        command.add("-C");
        command.add(cOutputDir.toString());
        for (var classDir : classDirPaths) {
            command.add(cOutputDir.relativize(classDir).toString());
        }

        var process = new ProcessBuilder(command).start();
        var archiveBytes = process.getInputStream().readAllBytes();
        var errorBytes = process.getErrorStream().readAllBytes();
        var exit = process.waitFor();
        if (exit != 0) {
            throw new GradleException("Failed to create tar archive: "
                    + new String(errorBytes, StandardCharsets.UTF_8));
        }
        return archiveBytes;
    }

    private String generateScript(Map<String, List<TestEntry>> allTests, byte[] archive) {
        var sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -o pipefail\n\n");
        sb.append("REMOTE_DIR=$(mktemp -d /tmp/teavm-test-XXXXXX)\n");
        sb.append("trap 'rm -rf \"$REMOTE_DIR\"' EXIT\n\n");

        appendArchiveExtraction(sb, archive);
        appendBuildFunction(sb);

        sb.append("PASS=0\n");
        sb.append("FAIL=0\n");
        sb.append("declare -a FAILURES=()\n");

        for (var entry : allTests.entrySet()) {
            appendClassSection(sb, entry.getKey(), entry.getValue());
        }

        appendSummary(sb);
        return sb.toString();
    }

    private void appendArchiveExtraction(StringBuilder sb, byte[] archive) {
        sb.append("base64 -d << 'TEAVM_ARCHIVE_EOF' | tar xzf - -C \"$REMOTE_DIR\"\n");
        var b64 = Base64.getEncoder().encodeToString(archive);
        int lineLen = 76;
        for (int i = 0; i < b64.length(); i += lineLen) {
            sb.append(b64, i, Math.min(i + lineLen, b64.length()));
            sb.append('\n');
        }
        sb.append("TEAVM_ARCHIVE_EOF\n\n");
    }

    private void appendBuildFunction(StringBuilder sb) {
        sb.append("_build() {\n");
        sb.append("    local src=$1\n");
        sb.append("    local log=\"$src/_build.log\"\n");
        sb.append("    local srcs=()\n");
        sb.append("    while IFS= read -r f; do\n");
        sb.append("        srcs+=(\"$src/$f\")\n");
        sb.append("    done < \"$src/all.txt\"\n");
        sb.append("    ( cd \"$src\" && clang -std=c11 -g -iquote . \\\n");
        sb.append("        -D_DARWIN_C_SOURCE \\\n");
        sb.append("        -o run_test \"${srcs[@]}\" -lm ) >\"$log\" 2>&1 || {\n");
        sb.append("        echo \"  ERROR: clang build failed for $(basename \\\"$src\\\")\"\n");
        sb.append("        tail -30 \"$log\"\n");
        sb.append("        return 1\n");
        sb.append("    }\n");
        sb.append("}\n\n");
    }

    private void appendClassSection(StringBuilder sb, String classRelPath, List<TestEntry> tests) {
        var className = classRelPath.substring(classRelPath.lastIndexOf('/') + 1);
        var escapedRelPath = classRelPath.replace("$", "\\$");

        sb.append("\necho \"\"\n");
        sb.append("echo \"--- ").append(className).append(" ---\"\n\n");

        var builtSrcDirs = new LinkedHashSet<String>();
        for (var test : tests) {
            builtSrcDirs.add(test.sourceDir());
        }
        for (var srcDir : builtSrcDirs) {
            sb.append("_build \"$REMOTE_DIR/").append(escapedRelPath).append("/").append(srcDir);
            sb.append("\" || exit 1\n");
        }
        sb.append("\n");

        for (var test : tests) {
            appendTestRun(sb, escapedRelPath, className, test);
        }
    }

    private void appendTestRun(StringBuilder sb, String escapedRelPath, String className, TestEntry test) {
        var binaryPath = "$REMOTE_DIR/" + escapedRelPath + "/" + test.sourceDir() + "/run_test";
        var excTag = (className + "_" + test.name).replaceAll("[^A-Za-z0-9_]", "_");
        var excPath = "$REMOTE_DIR/_exc_" + excTag + ".txt";

        sb.append("if TEAVM_TEST_EXCEPTION_FILE=\"").append(excPath).append("\" \\\n");
        sb.append("   \"").append(binaryPath).append("\"");
        if (test.argument != null) {
            sb.append(" \\\n   ").append(shQuote(test.argument));
        }
        sb.append(" >/dev/null 2>&1; then\n");
        sb.append("  echo \"  PASS  ").append(test.name).append("\"\n");
        sb.append("  PASS=$((PASS + 1))\n");
        sb.append("else\n");
        sb.append("  _e=$?\n");
        sb.append("  echo \"  FAIL  ").append(test.name).append(" (exit $_e)\"\n");
        sb.append("  grep -E 'TEAVM_(CLASS|MESSAGE|AT):' \"").append(excPath).append("\" 2>/dev/null \\\n");
        sb.append("    | sed 's/TEAVM_CLASS:/    Exception: /;");
        sb.append(" s/TEAVM_MESSAGE:/    Message:   /; s/TEAVM_AT:/        at /' \\\n");
        sb.append("    | head -20 || true\n");
        sb.append("  if [[ $_e -ge 132 && $_e -le 159 ]]; then\n");
        sb.append("    echo \"  --- crash backtrace ---\"\n");
        appendLldbInvocation(sb, binaryPath, test.argument);
        sb.append("  fi\n");
        sb.append("  FAIL=$((FAIL + 1))\n");
        sb.append("  FAILURES+=('").append(className).append(".").append(test.name).append("')\n");
        sb.append("fi\n");
        sb.append("rm -f \"").append(excPath).append("\"\n\n");
    }

    private void appendLldbInvocation(StringBuilder sb, String binaryPath, String argument) {
        sb.append("    lldb");
        sb.append(" -o 'settings set stop-line-count-before 0'");
        sb.append(" -o 'settings set stop-line-count-after 0'");
        sb.append(" -o run -o 'bt 20' -o quit");
        sb.append(" -- \"").append(binaryPath).append("\"");
        if (argument != null) {
            sb.append(" ").append(shQuote(argument));
        }
        sb.append(" 2>&1 || true\n");
    }

    private void appendSummary(StringBuilder sb) {
        sb.append("\necho \"\"\n");
        sb.append("echo \"==============================\"\n");
        sb.append("echo \"Results: $PASS passed, $FAIL failed\"\n");
        sb.append("if [[ ${#FAILURES[@]} -gt 0 ]]; then\n");
        sb.append("  echo \"Failed tests:\"\n");
        sb.append("  printf \"  %s\\n\" \"${FAILURES[@]}\"\n");
        sb.append("fi\n");
        sb.append("echo \"==============================\"\n");
        sb.append("[[ $FAIL -eq 0 ]]\n");
    }

    private void executeViaSSH(String script) throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        var password = getMacPassword().getOrNull();
        if (password != null) {
            command.add("sshpass");
            command.add("-p");
            command.add(password);
        }
        command.add("ssh");
        command.add("-o"); command.add("StrictHostKeyChecking=no");
        command.add("-o"); command.add("BatchMode=no");
        command.add("-o"); command.add("ConnectTimeout=30");
        command.add("-o"); command.add("ServerAliveInterval=60");
        command.add(getMacUser().get() + "@" + getMacHost().get());
        command.add("bash");
        command.add("-s");

        var process = new ProcessBuilder(command).redirectErrorStream(true).start();

        var logger = getLogger();
        var outputThread = new Thread(() -> {
            try (var reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.lifecycle(line);
                }
            } catch (IOException e) {
                logger.warn("Error reading SSH output: {}", e.getMessage());
            }
        });
        outputThread.start();

        try (var stdin = process.getOutputStream()) {
            stdin.write(script.getBytes(StandardCharsets.UTF_8));
        }
        outputThread.join();
        var exit = process.waitFor();
        if (exit != 0) {
            throw new GradleException("Remote Mac tests failed (exit code " + exit + ")");
        }
    }

    private static String shQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static final Gson GSON = new Gson();

    private static List<TestEntry> parseTestsJson(Path jsonFile) throws IOException {
        var content = Files.readString(jsonFile, StandardCharsets.UTF_8);
        return List.of(GSON.fromJson(content, TestEntry[].class));
    }

    private static final class TestEntry {
        @SerializedName("fileName") String fileName;
        @SerializedName("argument") String argument;
        @SerializedName("name") String name;

        String sourceDir() {
            return argument != null ? fileName : name + "/" + fileName;
        }
    }
}
