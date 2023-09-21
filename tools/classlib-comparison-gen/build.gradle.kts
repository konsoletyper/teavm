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
    java
}

description = "Tool that generates HTML report about TeaVM support of classes from Java standard library"

dependencies {
    implementation(project(":classlib"))
    implementation(project(":core"))
    implementation(libs.asm)
}

val outputDir = layout.buildDirectory.dir("jcl-support")
val generateComparison by tasks.register<JavaExec>("generateComparison") {
    dependsOn(tasks["relocateJar"])
    inputs.files(configurations.runtimeClasspath)
    inputs.files(tasks["relocateJar"].outputs.files)
    outputs.dir(outputDir)
    classpath = configurations.runtimeClasspath.get() + tasks["relocateJar"].outputs.files
    mainClass = "org.teavm.tools.classlibcomparison.JCLComparisonBuilder"
    args("-output", outputDir.get().asFile.absolutePath)
}

tasks.build.configure {
    dependsOn(generateComparison)
}