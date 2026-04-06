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

plugins {
    `java-library`
}

configurations {
    val teavmCompile = create("teavmCompile")
    compileClasspath.configure {
        extendsFrom(teavmCompile)
    }
    create("wasm")
}

dependencies {
    compileOnly(project(":jso:apis"))
    "teavmCompile"(project(":classlib"))
    "teavmCompile"(project(":tools:core"))
}

val generateWasm by tasks.register<JavaExec>("generateWasm") {
    outputs.dir(layout.buildDirectory.dir("webapp-out"))
    dependsOn(tasks.classes)
    classpath += configurations["teavmCompile"]
    classpath += java.sourceSets.main.get().output.classesDirs
    mainClass = "org.teavm.tooling.wasm.disassembly.Compiler"
    args(
        "org.teavm.tooling.wasm.disassembly.DisassemblerTool",
        layout.buildDirectory.dir("webapp-out").get().asFile.absolutePath,
        "disassembler.wasm"
    )
}

val copyHtml by tasks.register<Copy>("copyHtml") {
    outputs.dir(layout.buildDirectory.dir("webapp-out"))
    from(sourceSets.getByName("main").resources.srcDirs)
    into(layout.buildDirectory.dir("webapp-out"))
}

val copyRuntime by tasks.register<Copy>("copyRuntime") {
    outputs.dir(layout.buildDirectory.dir("webapp-out"))
    dependsOn(configurations["teavmCompile"])
    from(providers.provider {
        configurations["teavmCompile"].map { zipTree(it) }
    })
    into(layout.buildDirectory.dir("webapp-out"))
    include("**/wasm-gc-runtime.js")
    includeEmptyDirs = false
    eachFile {
        path = name
    }
}

tasks.assemble.configure {
    dependsOn(copyHtml, generateWasm, copyRuntime)
}