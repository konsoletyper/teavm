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

// Classpath of the program compiled by BootstrapCompilerBenchmark. It's loaded by a separate class loader,
// so it is not a part of benchmark's own classpath.
val bootstrapCompilerClasspath = configurations.create("bootstrapCompilerClasspath")

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
}

/*
 * Properties:
 *
 * - benchmark.args: JMH arguments, e.g. "-wi 5 -i 10 -f 1 -prof gc WasmGC"
 * - benchmark.jvmArgs: additional arguments for forked JVM, e.g. "-Xmx4g"
 * - benchmark.profile: when true, records JFR profile of measurement iterations into
 *   build/reports/jmh/profile/<benchmark>-<params>/profile.jfr (readable by IDEA and VisualVM)
 *
 * Example:
 * ./gradlew :compiler-benchmarks:jmh -Pbenchmark.args="-wi 5 -i 10 -f 2"
 * ./gradlew :compiler-benchmarks:jmh -Pbenchmark.profile=true -Pbenchmark.args="-f 1 bufferedInputStream"
 * ./gradlew :compiler-benchmarks:jmh -Pbenchmark.args="-p reuseParsedClasses=true,false BootstrapCompiler"
 */
fun splitArgs(text: String?) = text?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs compiler benchmarks using JMH"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.openjdk.jmh.Main"
    // Forked JVMs inherit system properties of the host JVM
    val bootstrapClasspath: FileCollection = bootstrapCompilerClasspath
    inputs.files(bootstrapClasspath)
    jvmArgumentProviders.add {
        listOf("-Dteavm.compilerBenchmarks.bootstrapCompilerClasspath=" + bootstrapClasspath.asPath)
    }
    val resultFile = layout.buildDirectory.file("reports/jmh/results.json").get().asFile
    doFirst {
        resultFile.parentFile.mkdirs()
    }
    val jvmArgs = providers.gradleProperty("benchmark.jvmArgs").orNull
    if (jvmArgs != null) {
        args("-jvmArgsAppend", jvmArgs)
    }
    if (providers.gradleProperty("benchmark.profile").orNull == "true") {
        val profileDir = layout.buildDirectory.dir("reports/jmh/profile").get().asFile
        args("-prof", "jfr:dir=${profileDir.absolutePath};configName=profile;stackDepth=256")
    }
    args(splitArgs(providers.gradleProperty("benchmark.args").orNull))
    args("-rf", "json", "-rff", resultFile.absolutePath)
    outputs.upToDateWhen { false }
}
