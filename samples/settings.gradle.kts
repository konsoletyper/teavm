import org.akhikhl.gretty.GrettyExtension
import org.akhikhl.gretty.GrettyPlugin

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

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.gretty") version "4.0.3" apply false
    }
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies.classpath("org.gretty:org.gretty.gradle.plugin:4.0.3")
}

rootProject.name = "teavmSamples"

include("stdout-helper")
include("hello")
include("async")
include("benchmark")
include("pi")
include("kotlin")
include("scala")
include("web-apis")
include("software3d")

gradle.allprojects {
    apply<WarPlugin>()
    apply<GrettyPlugin>()

    extensions.configure<GrettyExtension> {
        contextPath = ""
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

gradle.afterProject {
    val java = extensions.findByType<JavaPluginExtension>()
    if (java != null) {
        apply<CheckstylePlugin>()
        extensions.configure<CheckstyleExtension> {
            toolVersion = extensions.getByType<VersionCatalogsExtension>().named("libs")
                    .findVersion("checkstyle").get().requiredVersion
            configDirectory = File(settings.rootDir, "../config/checkstyle")
        }
        java.sourceCompatibility = JavaVersion.VERSION_11
        java.targetCompatibility = JavaVersion.VERSION_11
    }
}