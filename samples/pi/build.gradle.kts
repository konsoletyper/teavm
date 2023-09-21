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
}

configurations {
    create("war")
}

dependencies {
    "war"(project(":stdout-helper", "war"))
}

teavm {
    js {
        addedToWebApp = true
    }
    wasm {
        addedToWebApp = true
    }
    wasi {
        outputDir = layout.buildDirectory.dir("libs/wasi").get().asFile
        relativePathInOutputDir = ""
    }
    all {
        mainClass = "org.teavm.samples.pi.PiCalculator"
    }
}

tasks.war {
    dependsOn(configurations["war"])
    from(provider { configurations["war"].map { zipTree(it) } })
}

tasks.assemble {
    dependsOn(tasks.generateWasi,)
}

val buildNativeLinux by tasks.register<Exec>("buildNativeLinux") {
    group = "build"
    dependsOn(tasks.generateC)
    inputs.dir(layout.buildDirectory.dir("generated/teavm/c"))
    inputs.file(project.file("CMakeLists.txt"))
    outputs.file("build/dist/calc_pi")
    executable = "./build-native.sh"
    workingDir = project.projectDir
}