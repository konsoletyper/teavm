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

description = "JavaScript deobfuscator"

configurations {
    val teavmCompile = create("teavmCompile")
    compileClasspath.configure {
        extendsFrom(teavmCompile)
    }
    create("js")
}

dependencies {
    compileOnly(project(":jso:apis"))
    "teavmCompile"(project(":classlib"))
    "teavmCompile"(project(":tools:core"))
}

val generateJs by tasks.register<JavaExec>("generateJs") {
    outputs.dir(layout.buildDirectory.dir("teavm"))
    dependsOn(tasks.classes)
    classpath += configurations["teavmCompile"]
    classpath += java.sourceSets.main.get().output.classesDirs
    mainClass = "org.teavm.tooling.deobfuscate.js.Compiler"
    args(
        "org.teavm.tooling.deobfuscate.js.Deobfuscator",
        "\$teavm_deobfuscator",
        layout.buildDirectory.dir("teavm").get().asFile.absolutePath,
        "deobfuscator.js"
    )
}

val generateLibJs by tasks.register<JavaExec>("generateLibJs") {
    outputs.dir(layout.buildDirectory.dir("teavm-lib"))
    dependsOn(tasks.classes)
    classpath += configurations["teavmCompile"]
    classpath += java.sourceSets.main.get().output.classesDirs
    mainClass = "org.teavm.tooling.deobfuscate.js.Compiler"
    args(
        "org.teavm.tooling.deobfuscate.js.DeobfuscatorLib",
        "deobfuscator",
        layout.buildDirectory.dir("teavm-lib").get().asFile.absolutePath,
        "deobfuscator-lib.js",
    )
}

val zipWithJs by tasks.register<Jar>("zipWithJs") {
    dependsOn(generateJs, generateLibJs)
    archiveClassifier = "js"
    from(layout.buildDirectory.dir("teavm"), layout.buildDirectory.dir("teavm-lib"))
    entryCompression = ZipEntryCompression.DEFLATED
}

tasks.assemble.configure {
    dependsOn(zipWithJs)
}

artifacts.add("js", zipWithJs)