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
    `java-library`
}


dependencies {
    implementation(project(":core"))
}

fun registerWasmCompareTask(taskName: String, mode: String) {
    tasks.register<JavaExec>(taskName) {
        group = "verification"
        description = "Compares Wasm GC test modules ($mode mode). Properties: " +
                "teavm.wasmCompare.a, teavm.wasmCompare.b, teavm.wasmCompare.out"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass = "org.teavm.tools.internal.WasmTestComparator"
        val testsDir = rootProject.layout.projectDirectory.dir("tests/build/teavm-tests")
        val dirA = providers.gradleProperty("teavm.wasmCompare.a")
                .orElse(testsDir.dir("wasm-gc").asFile.absolutePath)
        val dirB = providers.gradleProperty("teavm.wasmCompare.b")
                .orElse(testsDir.dir("bootstrap-wasm-gc").asFile.absolutePath)
        val outDir = providers.gradleProperty("teavm.wasmCompare.out")
                .orElse(layout.buildDirectory.dir("wasm-compare/$mode").get().asFile.absolutePath)
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(mode, dirA.get(), dirB.get(), outDir.get())
        })
    }
}

registerWasmCompareTask("compareWasmBytes", "bytes")
registerWasmCompareTask("compareWasmFunctions", "functions")
