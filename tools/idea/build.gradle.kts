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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
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
    instrumentedJar {
        archiveFileName = "teavm-plugin.jar"
    }
}
