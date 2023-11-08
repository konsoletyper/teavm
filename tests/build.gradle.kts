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
    `java-library`
}

description = "Tests"

javaVersion {
    version = JavaVersion.VERSION_21
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":classlib"))
    testImplementation(project(":jso:apis"))
    testImplementation(project(":platform"))
    testImplementation(project(":metaprogramming:impl"))
    testImplementation(project(":tools:core"))
    testImplementation(project(":tools:junit"))
    testImplementation(libs.hppc)
    testImplementation(libs.rhino)
    testImplementation(libs.junit)
    testImplementation(libs.testng)
}

tasks.test {
    systemProperty("teavm.junit.target", layout.buildDirectory.dir("teavm-tests").get().asFile.absolutePath)
    val browser = providers.gradleProperty("teavm.tests.browser").orElse("browser-chrome").get()

    systemProperty("teavm.junit.js", providers.gradleProperty("teavm.tests.js").orElse("true").get())
    systemProperty("teavm.junit.js.runner", browser)
    systemProperty("teavm.junit.minified", providers.gradleProperty("teavm.tests.minified").orElse("false").get())
    systemProperty("teavm.junit.optimized", providers.gradleProperty("teavm.tests.optimized").orElse("true").get())
    systemProperty("teavm.junit.js.decodeStack", providers.gradleProperty("teavm.tests.decodeStack")
            .orElse("false").get())

    systemProperty("teavm.junit.wasm", providers.gradleProperty("teavm.tests.wasm").orElse("true").get())
    systemProperty("teavm.junit.wasm.runner", browser)

    systemProperty("teavm.junit.wasi", providers.gradleProperty("teavm.tests.wasi").orElse("true").get())
    systemProperty("teavm.junit.wasi.runner", providers.gradleProperty("teavm.tests.wasi.runner")
            .orElse("./run-wasi.sh").get())

    systemProperty("teavm.junit.c", providers.gradleProperty("teavm.tests.c").orElse("true").get())
    systemProperty("teavm.junit.c.compiler", providers.gradleProperty("teavm.tests.c.compiler")
            .orElse("compile-c-unix-fast.sh").get())

    val dependencies = configurations.testRuntimeClasspath.get()
            .incoming.resolutionResult.allDependencies
            .asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .map { it.requested }
            .filterIsInstance<ProjectComponentSelector>()
            .map { project.rootProject.project(it.projectPath) }
    val projects = dependencies + project
    val dirs = projects.map { it.layout.projectDirectory }.flatMap {
        sequenceOf(
                it.dir("src/main/java"),
                it.dir("src/test/java")
        )
    }
    systemProperty("teavm.junit.sourceDirs", dirs
            .map { it.asFile.absolutePath }
            .joinToString(File.pathSeparator))

    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}