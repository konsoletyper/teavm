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

import org.teavm.buildutil.JmhArguments

plugins {
    java
    id("local-properties")
}

description = "Benchmarks that measure performance of code produced by TeaVM"

val perf by configurations.creating

dependencies {
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator.annprocess)
    perf(project(":tools:perf"))
}

// All properties are described in gradle-local-template.properties

val osName = System.getProperty("os.name").lowercase()
val isWindowsOs = osName.contains("win")
val isMacOs = osName.startsWith("mac")
val cCompilerOverridden = localProperties["benchmark.c.compiler"].isPresent
val defaultCCompiler = when {
    isWindowsOs -> "compile-c-windows.bat"
    isMacOs -> "compile-c-macos.sh"
    else -> "compile-c-unix.sh"
}
// only run our own env setup script when we're compiling with our own default Windows compiler
// script; a custom benchmark.c.compiler is expected to set up whatever environment it needs itself.
val defaultCEnvScript = if (isWindowsOs && !cCompilerOverridden) "setup-msvc-env.bat" else ""

val benchmarkArgs = JmhArguments.fromProperties(localProperties)
val jvmResultFile = layout.buildDirectory.file("reports/jmh/jvm.json")
val compareJvm = localProperties.flag("benchmark.compareJvm")

val jmh by tasks.registering(JavaExec::class) {
    group = "benchmark"
    description = "Runs benchmarks on JVM using JMH"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    val resultFile = jvmResultFile.get().asFile
    doFirst {
        resultFile.parentFile.mkdirs()
    }
    args(benchmarkArgs)
    args("-rf", "json", "-rff", resultFile.absolutePath)
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("teavmBenchmark") {
    group = "benchmark"
    description = "Compiles benchmarks with TeaVM and runs them"
    classpath = sourceSets.main.get().runtimeClasspath + perf
    mainClass = "org.teavm.perf.PerfCli"
    for (dir in sourceSets.main.get().output.classesDirs) {
        args("--scan", dir.absolutePath)
    }
    args("-o", layout.buildDirectory.dir("reports/teavm-perf").get().asFile.absolutePath)
    args("-b", localProperties["benchmark.backends"].getOrElse("js"))
    args("--browser", localProperties["benchmark.browser"].getOrElse("browser-chrome"))
    args("--optimization", localProperties["benchmark.optimization"].getOrElse("advanced"))
    args("--c-build-script", file(localProperties["benchmark.c.compiler"].getOrElse(defaultCCompiler))
            .absolutePath)
    val cEnvScript = localProperties["benchmark.c.envScript"].getOrElse(defaultCEnvScript)
    if (cEnvScript.isNotEmpty()) {
        args("--c-env-script", file(cEnvScript).absolutePath)
    }
    args("-rf", localProperties["benchmark.formats"].getOrElse("text,json,html,markdown"))
    for (profiler in localProperties["benchmark.profiler"].getOrElse("").split(",")) {
        if (profiler.isNotBlank()) {
            args("-prof", profiler.trim())
        }
    }
    for (baseline in localProperties["benchmark.baseline"].getOrElse("").split(",")) {
        val spec = baseline.trim()
        if (spec.isEmpty()) {
            continue
        }
        val eq = spec.indexOf('=')
        if (eq > 0 && !spec.substring(0, eq).contains('/')) {
            args("--baseline", spec.substring(0, eq) + "=" + file(spec.substring(eq + 1)).absolutePath)
        } else {
            args("--baseline", file(spec).absolutePath)
        }
    }
    if (compareJvm) {
        dependsOn(jmh)
        args("--baseline", "jvm=" + jvmResultFile.get().asFile.absolutePath)
    }
    args(benchmarkArgs)
    outputs.upToDateWhen { false }
}
