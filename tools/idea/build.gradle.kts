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

intellij {
    version = libs.versions.idea.asProvider().get()
    type = "IC"
    updateSinceUntilBuild = false

    plugins = listOf(
            "java",
            "org.intellij.scala:${libs.versions.idea.scala.get()}",
            "org.jetbrains.kotlin"
    )
}

dependencies {
    implementation(project(path = ":tools:ide-deps", configuration = "shadow").setTransitive(false))
}

tasks {
    instrumentedJar {
        archiveFileName = "teavm-plugin.jar"
    }
    buildSearchableOptions {
        enabled = false
    }

    publishPlugin {
        token = providers.gradleProperty("teavm.idea.publishToken")
    }
}
