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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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
    systemProperty("teavm.junit.target", "${project.buildDir.absolutePath }/js-tests")
    systemProperty("teavm.junit.js.runner", "browser-chrome")
    systemProperty("teavm.junit.threads", "1")
    systemProperty("teavm.junit.minified", providers.gradleProperty("teavm.tests.minified"))
    systemProperty("teavm.junit.optimized", providers.gradleProperty("teavm.tests.optimized"))
    systemProperty("teavm.junit.js.decodeStack", providers.gradleProperty("teavm.tests.decodeStack"))
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}