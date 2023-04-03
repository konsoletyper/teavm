import org.teavm.buildutil.DependencyRelocationExtension

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

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}
plugins {
    id("dependency-relocation")
}

rootProject.name = "teavm"

include("core")
include("interop:core")
include("metaprogramming:api", "metaprogramming:impl")
include("jso:core", "jso:apis", "jso:impl")
include("platform")
include("classlib")
include("tools:core")
include("tools:deobfuscator-js")
include("tools:junit")
include("tools:devserver")
include("tools:c-incremental")
include("tools:chrome-rdp")
include("tools:cli")
include("tools:gradle")
include("tools:ide-deps")
include("tools:idea")
include("tools:maven:plugin")
include("tools:maven:webapp")
include("tools:classlib-comparison-gen")
include("tests")
include("extras-slf4j")

val teavmVersion = providers.gradleProperty("teavm.project.version").get()

gradle.allprojects {
    repositories {
        mavenCentral()
    }
    version = teavmVersion
}

gradle.allprojects {
    apply(plugin = "javaVersion")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    tasks.withType<Javadoc>().configureEach {
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
        }
    }

    extensions.findByType<PublishingExtension>()?.apply {
        publications.all {
            if (this is MavenPublication) {
                pom { setupPom(this@afterProject) }
            }
        }
    }
}

fun MavenPom.setupPom(project: Project) {
    name.set(project.description)
    description.set(project.description)
    licenses {
        license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
            comments.set("A business-friendly OSS license")
        }
    }
    developers {
        developer {
            id.set("konsoletyper")
            name.set("Alexey Andreev")
            email.set("konsoletyper@gmail.com")
            timezone.set("Europe/Berlin")
        }
    }
    scm {
        url.set("https://github.com/konsoletyper/teavm")
        connection.set("scm:git:git@github.com:konsoletyper/teavm.git")
    }
    url.set("https://teavm.org")
}

extensions.configure<DependencyRelocationExtension> {
    library("libs", "commons-io") {
        relocate("org.apache.commons", "org.teavm.apachecommons")
    }
    for (asmLib in listOf("asm", "asm-tree", "asm-analysis", "asm-commons", "asm-util")) {
        library("libs", asmLib) {
            relocate("org.objectweb.asm", "org.teavm.asm")
        }
    }
    library("libs", "rhino") {
        relocate("org.mozilla", "org.teavm.rhino")
    }
    library("libs", "hppc") {
        relocate("com.carrotsearch.hppc", "org.teavm.hppc")
    }

    skip(":tools:gradle")
}