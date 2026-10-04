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

description = "Benchmarks that measure performance of code produced by TeaVM"

val perf by configurations.creating

dependencies {
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator.annprocess)
    perf(project(":tools:perf"))
}

/*
 * Properties that control both `jmh` and `teavmBenchmark` tasks:
 *
 * - benchmark.args: JMH-style arguments, e.g. "-wi 3 -i 5 -f 1 -p digits=100 String"
 *
 * Properties that control `teavmBenchmark` task:
 *
 * - benchmark.backends: comma-separated list of js, wasm-gc, c (default is js)
 * - benchmark.browser: browser-chrome (default), browser-firefox or browser (open URL manually)
 * - benchmark.optimization: simple, advanced, full (default)
 * - benchmark.formats: comma-separated list of text, json, html (default is all of them)
 * - benchmark.compareJvm: when true, runs benchmarks with JMH on JVM first and includes results in the report
 * - benchmark.baseline: comma-separated list of JMH JSON result files to compare with, each optionally
 *   prefixed with label, e.g. jvm=build/reports/jmh/jvm.json
 *
 * Example:
 * ./gradlew :benchmarks:teavmBenchmark -Pbenchmark.backends=js,wasm-gc -Pbenchmark.args="-wi 1 -i 3 StringSplit"
 */
fun splitArgs(text: String?) = text?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()

val benchmarkArgs = splitArgs(providers.gradleProperty("benchmark.args").orNull)
val jvmResultFile = layout.buildDirectory.file("reports/jmh/jvm.json")
val compareJvm = providers.gradleProperty("benchmark.compareJvm").map { it.toBoolean() }.getOrElse(false)

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
    args("-b", providers.gradleProperty("benchmark.backends").getOrElse("js"))
    args("--browser", providers.gradleProperty("benchmark.browser").getOrElse("browser-chrome"))
    args("--optimization", providers.gradleProperty("benchmark.optimization").getOrElse("full"))
    args("-rf", providers.gradleProperty("benchmark.formats").getOrElse("text,json,html"))
    for (baseline in providers.gradleProperty("benchmark.baseline").getOrElse("").split(",")) {
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
