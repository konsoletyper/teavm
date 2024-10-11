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

description = "JavaScript deobfuscator"

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
    outputs.dir(layout.buildDirectory.dir("teavm"))
    dependsOn(tasks.classes)
    classpath += configurations["teavmCompile"]
    classpath += java.sourceSets.main.get().output.classesDirs
    mainClass = "org.teavm.tooling.deobfuscate.wasmgc.Compiler"
    args(
        "org.teavm.tooling.deobfuscate.wasmgc.DeobfuscatorFactory",
        layout.buildDirectory.dir("teavm").get().asFile.absolutePath,
        "deobfuscator.wasm"
    )
}

val zipWithWasm by tasks.register<Jar>("zipWithWasm") {
    dependsOn(generateWasm)
    archiveClassifier = "wasm"
    from(layout.buildDirectory.dir("teavm"), layout.buildDirectory.dir("teavm-lib"))
    include("*.wasm")
    entryCompression = ZipEntryCompression.DEFLATED
}

tasks.assemble.configure {
    dependsOn(zipWithWasm)
}

artifacts.add("wasm", zipWithWasm)