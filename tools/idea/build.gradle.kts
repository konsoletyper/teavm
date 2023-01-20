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
    id("java")
    id("checkstyle")
    id("org.jetbrains.intellij") version "1.10.1"
}

group = "org.teavm"
version = "0.7.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

intellij {
    version.set("2020.1.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("java", "org.intellij.scala:2020.1.43", "org.jetbrains.kotlin"))
}

checkstyle {
    toolVersion = "8.41"
    configFile = File("../../checkstyle.xml")
}

dependencies {
    implementation(group = "org.teavm", name = "teavm-ide-deps", version = project.version.toString(),
            classifier = "shaded")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("201")
        untilBuild.set("231.*")
    }
}
