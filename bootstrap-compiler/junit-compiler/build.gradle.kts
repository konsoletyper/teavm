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

plugins {
    java
}

description = "TeaVM compiler for JUnit tests, compiled into Wasm GC and running in Node.js"

val teavmCli = configurations.create("teavmCli")
val wasmGCRuntime = configurations.create("wasmGCRuntime") {
    isTransitive = false
}

dependencies {
    implementation(project(":bootstrap-compiler:core"))
    implementation(project(":tools:junit"))
    implementation(project(":jso:apis"))
    compileOnly(project(":extension:spi"))
    annotationProcessor(project(":extension:processor"))

    teavmCli(project(":tools:cli"))
    wasmGCRuntime(project(":core"))
}

val compilerDir = layout.buildDirectory.dir("compiler")
val wasmDir = layout.buildDirectory.dir("compiler-wasm")

val buildWasmGC = tasks.register<JavaExec>("buildWasmGC") {
    group = "build"
    description = "Compiles test compiler into Wasm GC module"
    val outputDir = wasmDir
    inputs.files(sourceSets.main.map { it.runtimeClasspath })
    outputs.dir(outputDir)
    classpath(teavmCli, sourceSets.main.map { it.runtimeClasspath })
    mainClass = "org.teavm.cli.TeaVMRunner"
    maxHeapSize = "2g"
    argumentProviders.add {
        listOf(
            "-t", "wasm-gc",
            "-O", "1",
            "-d", outputDir.get().asFile.absolutePath,
            "-f", "junit-compiler.wasm",
            "org.teavm.compiler.bootstrap.junit.TestCompiler"
        )
    }
}

val assembleCompiler = tasks.register<Sync>("assembleCompiler") {
    group = "build"
    description = "Assembles directory with test compiler and Node.js script to run it"
    from(buildWasmGC) {
        include("junit-compiler.wasm")
    }
    from(provider { wasmGCRuntime.map { zipTree(it) } }) {
        include("org/teavm/backend/wasm/wasm-gc-runtime.js")
        eachFile { path = name }
    }
    from("src/main/js")
    includeEmptyDirs = false
    into(compilerDir)
}

configurations.consumable("bootstrapCompiler") {
    outgoing.artifact(compilerDir) {
        builtBy(assembleCompiler)
    }
}
