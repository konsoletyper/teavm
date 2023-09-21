/*
 *  Copyright 2023 Alexey Andreev.
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

plugins {
    java
    war
    id("org.teavm")
    id("org.wisepersist.gwt")
}

val gwtVersionNum = "2.10.0"
var jbox2d = "org.jbox2d:jbox2d-library:2.2.1.1"

dependencies {
    teavm(teavm.libs.jsoApis)
    teavm(teavm.libs.interop)
    teavm(jbox2d)
    teavm("com.google.gwt:gwt-user:$gwtVersionNum")
    gwt("$jbox2d:sources")
}

gwt {
    gwtVersion = gwtVersionNum
    modules.add("org.teavm.samples.benchmark.benchmark")
    devWar = File(buildDir, "gwt-war")
}

val generatedCSources = File(buildDir, "generated/teavm-c")
val executableFile = File(buildDir, "dist/teavm_benchmark")

teavm {
    js {
        addedToWebApp = true
        mainClass = "org.teavm.samples.benchmark.teavm.BenchmarkStarter"
        debugInformation = true
    }
    wasm {
        addedToWebApp = true
        mainClass = "org.teavm.samples.benchmark.teavm.WasmBenchmarkStarter"
    }
    c {
        mainClass = "org.teavm.samples.benchmark.teavm.Gtk3BenchmarkStarter"
        relativePathInOutputDir = ""
        outputDir = generatedCSources
    }
}

tasks.build.configure {
    dependsOn(tasks.generateC)
}

val buildNativeLinux by tasks.register<Exec>("buildNativeLinux") {
    group = "build"
    dependsOn(tasks.generateC)
    inputs.dir(generatedCSources)
    inputs.file(project.file("CMakeLists.txt"))
    outputs.file(executableFile)
    executable = "./build-native.sh"
    workingDir = project.projectDir
}

tasks.register<Exec>("runNativeLinux") {
    group = "run"
    dependsOn(buildNativeLinux)
    inputs.file(executableFile)
    executable = executableFile.relativeTo(project.projectDir).path
    workingDir = project.projectDir
}
