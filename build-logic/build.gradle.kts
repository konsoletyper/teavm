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
    checkstyle
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.shadowPlugin)
    implementation(libs.maven.plugin.tools.api)
    implementation(libs.maven.plugin.tools.generators)
    implementation(libs.maven.plugin.tools.annotations)
    implementation(libs.maven.embedder)
    implementation(libs.maven.compat)
}

gradlePlugin {
    plugins {
        create("teavm-publish") {
            id = "teavm-publish"
            implementationClass = "org.teavm.buildutil.PublishTeaVMPlugin"
        }
        create("dependency-relocation") {
            id = "dependency-relocation"
            implementationClass = "org.teavm.buildutil.DependencyRelocationPlugin"
        }
        create("shadowApply") {
            id = "shadowApply"
            implementationClass = "org.teavm.buildutil.ShadowApplyPlugin"
        }
        create("mavenPlugin") {
            id = "mavenPlugin"
            implementationClass = "org.teavm.buildutil.MavenPluginPlugin"
        }
        create("javaVersion") {
            id = "javaVersion"
            implementationClass = "org.teavm.buildutil.JavaVersionPlugin"
        }
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory = project.layout.projectDirectory.dir("../config/checkstyle")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val generatedConfigDir = project.layout.buildDirectory.dir("generated/config").get()

val generateConfig by tasks.registering {
    val file = generatedConfigDir.file("org/teavm/buildutil/Config.java").asFile
    outputs.file(file)
    doLast {
        file.parentFile.mkdirs()
        file.writeText("""
            package org.teavm.buildutil;
            
            class Config {
                static final String MAVEN_PLUGIN_API = "${libs.maven.plugin.api.get()}";
                static final String MAVEN_PLUGIN_ANNOTATIONS = "${libs.maven.plugin.annotations.get()}";
                static final String MAVEN_ARTIFACT = "${libs.maven.artifact.get()}";
                static final String MAVEN_CORE = "${libs.maven.core.get()}";
            }
        """.trimIndent())
    }
}
tasks.compileJava.configure {
    dependsOn(generateConfig)
    options.encoding = "UTF-8"
}

sourceSets.main.configure { java.srcDir(generatedConfigDir) }