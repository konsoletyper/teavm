/*
 *  Copyright 2022 Alexey Andreev.
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
    alias(libs.plugins.intellij)
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

javaVersion {
    version = JavaVersion.VERSION_17
}

intellijPlatform {
    pluginConfiguration {
        name = "TeaVM Integration"
    }
    publishing {
        token = providers.gradleProperty("teavm.idea.publishToken")
    }
}

dependencies {
    compileOnly(project(":tools:ide-deps"))
    runtimeOnly(project(path = ":tools:ide-deps", configuration = "shadow").setTransitive(false))
    intellijPlatform {
        intellijIdeaCommunity(libs.versions.idea.asProvider())
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        plugin(provider { "org.intellij.scala" }, libs.versions.idea.scala, provider { "com.jetbrains.plugins" })
    }
}

tasks {
    composedJar {
        archiveFileName = "teavm-plugin.jar"
    }
    build {
        dependsOn(buildPlugin)
    }
}

tasks.whenTaskAdded {
    if (name == "relocateJar") {
        val t = this
        tasks.prepareTestSandbox {
            dependsOn(t)
        }
        tasks.prepareSandbox {
            dependsOn(t)
        }
    }
}

val configPath = project.layout.buildDirectory.dir("generated/sources/config")

val createConfig by tasks.registering {
    outputs.dir(configPath)
    inputs.property("version", project.version)
    val basePath = configPath
    val version = project.version
    doLast {
        val file = File(basePath.get().asFile, "org/teavm/idea/BuildConfig.java")
        file.parentFile.mkdirs()
        file.writeText("""
            package org.teavm.idea;
            
            public final class BuildConfig {
                public static final String VERSION = "$version";
            
                private BuildConfig() {
                }
            }
        """.trimIndent())
    }
}

tasks.compileJava.configure {
    dependsOn(createConfig)
    options.forkOptions.memoryMaximumSize = "1g"
}
sourceSets.main.configure { java.srcDir(configPath) }

tasks.withType<Checkstyle> {
    exclude("org/teavm/idea/BuildConfig.java")
}