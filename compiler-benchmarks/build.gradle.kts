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

description = "Benchmarks that measure performance of TeaVM compiler itself"

// Classpath of the program compiled by BootstrapCompilerBenchmark. It's loaded by a separate class loader,
// so it is not a part of benchmark's own classpath.
val bootstrapCompilerClasspath = configurations.create("bootstrapCompilerClasspath")
val benchmarkReport = configurations.create("benchmarkReport")

dependencies {
    implementation(project(":core"))
    implementation(project(":classlib"))
    implementation(project(":platform"))
    implementation(project(":jso:impl"))
    implementation(project(":metaprogramming:impl"))
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator.annprocess)

    bootstrapCompilerClasspath(project(":bootstrap-compiler:junit-compiler"))
    bootstrapCompilerClasspath(project(":tools:core"))
    bootstrapCompilerClasspath(project(":classlib"))

    benchmarkReport(project(":tools:benchmark-report"))
}

// All properties are described in gradle-local-template.properties
val reportsDir = layout.buildDirectory.dir("reports/jmh")
val resultFile = reportsDir.map { it.file("results.json") }
val baselinesDir = layout.buildDirectory.dir("baselines")

fun isBaselinePath(spec: String) = spec.endsWith(".json") || spec.contains('/') || spec.contains('\\')

fun baselineFile(spec: String): File =
    if (isBaselinePath(spec)) file(spec) else baselinesDir.get().file("$spec.json").asFile

fun baselineLabel(spec: String): String = if (isBaselinePath(spec)) file(spec).nameWithoutExtension else spec

val saveBaseline = localProperties.string("benchmark.saveBaseline")
val baseline = localProperties.string("benchmark.baseline")

val jmhCompare by tasks.registering(JavaExec::class) {
    group = "benchmark"
    description = "Compares results of the last jmh run with baseline (benchmark.baseline property)"
    classpath = benchmarkReport
    mainClass = "org.teavm.benchmark.report.ReportCli"
    val hasBaseline = baseline != null
    onlyIf("benchmark.baseline is set") { hasBaseline }
    if (baseline != null) {
        args("--title", "TeaVM compiler benchmarks", "--name", "comparison",
            "-o", reportsDir.get().asFile.absolutePath,
            baselineLabel(baseline) + "=" + baselineFile(baseline).absolutePath,
            "current=" + resultFile.get().asFile.absolutePath)
    }
    outputs.upToDateWhen { false }
}

fun JavaExec.configureJmh() {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    // Forked JVMs inherit system properties of the host JVM
    val bootstrapClasspath: FileCollection = bootstrapCompilerClasspath
    inputs.files(bootstrapClasspath)
    jvmArgumentProviders.add {
        listOf("-Dteavm.compilerBenchmarks.bootstrapCompilerClasspath=" + bootstrapClasspath.asPath)
    }
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs compiler benchmarks using JMH"
    configureJmh()
    val result = resultFile.get().asFile
    val savedBaseline = saveBaseline?.let { baselineFile(it) }
    doFirst {
        result.parentFile.mkdirs()
    }
    localProperties.string("benchmark.jvmArgs")?.let { args("-jvmArgsAppend", it) }
    if (localProperties.flag("benchmark.gc")) {
        args("-prof", "gc")
    }
    if (localProperties.flag("benchmark.profile")) {
        val profileDir = layout.buildDirectory.dir("reports/jmh/profile").get().asFile
        args("-prof", "jfr:dir=${profileDir.absolutePath};configName=profile;stackDepth=256")
    }
    // Fail the build when benchmark throws exception instead of silently skipping it
    args("-foe", "true")
    args(JmhArguments.fromProperties(localProperties))
    args("-rf", "json", "-rff", result.absolutePath)
    if (savedBaseline != null) {
        doLast {
            savedBaseline.parentFile.mkdirs()
            result.copyTo(savedBaseline, overwrite = true)
            println("Results saved as baseline to $savedBaseline")
        }
    }
    if (baseline != null) {
        finalizedBy(jmhCompare)
    }
}

tasks.register<JavaExec>("jmhList") {
    group = "benchmark"
    description = "Lists benchmarks selected by benchmark.include/benchmark.exclude, with their parameters"
    configureJmh()
    args("-lp")
    args(JmhArguments.fromProperties(localProperties))
}
