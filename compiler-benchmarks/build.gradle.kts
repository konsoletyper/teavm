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

description = "Benchmarks that measure performance of TeaVM compiler itself"

dependencies {
    implementation(project(":core"))
    implementation(project(":classlib"))
    implementation(project(":platform"))
    implementation(project(":jso:impl"))
    implementation(project(":metaprogramming:impl"))
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator.annprocess)
}

/*
 * Properties:
 *
 * - benchmark.args: JMH arguments, e.g. "-wi 5 -i 10 -f 1 -prof gc WasmGC"
 * - benchmark.jvmArgs: additional arguments for forked JVM, e.g. "-Xmx4g"
 *
 * Example:
 * ./gradlew :compiler-benchmarks:jmh -Pbenchmark.args="-wi 5 -i 10 -f 2"
 */
fun splitArgs(text: String?) = text?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs compiler benchmarks using JMH"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    val resultFile = layout.buildDirectory.file("reports/jmh/results.json").get().asFile
    doFirst {
        resultFile.parentFile.mkdirs()
    }
    val jvmArgs = providers.gradleProperty("benchmark.jvmArgs").orNull
    if (jvmArgs != null) {
        args("-jvmArgsAppend", jvmArgs)
    }
    args(splitArgs(providers.gradleProperty("benchmark.args").orNull))
    args("-rf", "json", "-rff", resultFile.absolutePath)
    outputs.upToDateWhen { false }
}
