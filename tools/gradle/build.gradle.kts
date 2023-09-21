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
    `java-gradle-plugin`
    publishing
    `teavm-publish`
    alias(libs.plugins.pluginPublish)
}

description = "TeaVM Gradle plugin"

dependencies {
    implementation(project(":core"))
    implementation(project(":tools:core"))
}

gradlePlugin {
    website = "https://teavm.org"
    vcsUrl = "https://github.com/konsoletyper/teavm"
    plugins {
        create("TeaVMPlugin") {
            id = "org.teavm"
            implementationClass = "org.teavm.gradle.TeaVMPlugin"
            displayName = "TeaVM application plugin"
            description = "Installs TeaVM compilation tasks, configurations and source sets"
            tags = listOf("teavm", "javascript", "webassembly", "compiler", "aot-compiler")
        }
        create("TeaVMLibraryPlugin") {
            id = "org.teavm.library"
            implementationClass = "org.teavm.gradle.TeaVMLibraryPlugin"
            displayName = "TeaVM library plugin"
            description = "Installs TeaVM DSL for consuming TeaVM libraries and running tests in a browser"
            tags = listOf("teavm", "javascript", "webassembly", "compiler", "aot-compiler")
        }
    }
}

fun findArtifactCoordinates(path: String): String {
    val project = project(path)
    val publishing = project.extensions.findByType<PublishingExtension>()
    return publishing
            ?.publications
            ?.filterIsInstance<MavenPublication>()
            ?.firstOrNull()
            ?.let { "${it.groupId}:${it.artifactId}:${it.version}" }
            ?: "${project.group}:${project.name}:${project.version}"
}

val configPath = project.layout.buildDirectory.dir("generated/sources/config")

val createConfig by tasks.registering {
    outputs.dir(configPath)
    inputs.property("version", project.version)
    val baseDir = configPath.get().asFile
    val jso = findArtifactCoordinates(":jso:core")
    val jsoApis = findArtifactCoordinates(":jso:apis")
    val interop = findArtifactCoordinates(":interop:core")
    val metaprogramming = findArtifactCoordinates(":metaprogramming:api")
    val classlib = findArtifactCoordinates(":classlib")
    val jsoImpl = findArtifactCoordinates(":jso:impl")
    val metaprogrammingImpl = findArtifactCoordinates(":metaprogramming:impl")
    val tools = findArtifactCoordinates(":tools:core")
    val junit = findArtifactCoordinates(":tools:junit")
    doLast {
        val file = File(baseDir, "org/teavm/gradle/config/ArtifactCoordinates.java")
        file.parentFile.mkdirs()
        file.writeText("""
            package org.teavm.gradle.config;
            
            public final class ArtifactCoordinates {
                public static final String JSO = "$jso";
                public static final String JSO_APIS = "$jsoApis";
                public static final String INTEROP = "$interop";
                public static final String METAPROGRAMMING = "$metaprogramming";
                
                public static final String CLASSLIB = "$classlib";
                public static final String JSO_IMPL = "$jsoImpl";
                public static final String METAPROGRAMMING_IMPL = "$metaprogrammingImpl";
                public static final String JUNIT = "$junit";
                
                public static final String TOOLS = "$tools";
            
                private ArtifactCoordinates() {
                }
            }
        """.trimIndent())
    }
}

tasks.compileJava.configure { dependsOn(createConfig) }
tasks.sourcesJar {
    dependsOn(createConfig)
}

sourceSets.main.configure { java.srcDir(configPath) }

tasks.withType<Checkstyle> {
    exclude("org/teavm/gradle/config/*")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "org.teavm"
            artifactId = "teavm-gradle-plugin"
        }
    }
}

