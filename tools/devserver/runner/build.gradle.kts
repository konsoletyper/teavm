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
    `teavm-publish`
}

description = "Command line tools"

dependencies {
    implementation(project(":tools:core"))
    implementation(project(":tools:devserver:core"))
    implementation(libs.commons.cli)
    implementation(libs.slf4j)
}

teavmPublish {
    artifactId = "teavm-devserver-runner"
}

val depsFile = layout.buildDirectory.file("dependencies.txt")

val generateDependenciesList by tasks.registering {
    val depsFile = depsFile
    outputs.file(depsFile)
    val cp = configurations.runtimeClasspath.get().incoming.artifacts
    inputs.files(configurations.runtimeClasspath)
    val version = project.version
    val deps = provider {
        cp.mapNotNull { artifact ->
            when (val id = artifact.id.componentIdentifier) {
                is ProjectComponentIdentifier -> findArtifactCoordinates(id.projectPath)
                is ModuleComponentIdentifier -> "${id.group}:${id.module}:${id.version}"
                else -> null
            }
        }
    }
    doLast {
        val fullList = deps.get() + "org.teavm:teavm-devserver-runner:$version"
        depsFile.get().asFile.writeText(fullList.joinToString("\n"))
    }
}

fun findArtifactCoordinates(path: String): String? {
    val project = project(path)
    val publishing = project.extensions.findByType<PublishingExtension>()
    return publishing
        ?.publications
        ?.filterIsInstance<MavenPublication>()
        ?.firstOrNull()
        ?.let { "${it.groupId}:${it.artifactId}:${it.version}" }
}

publishing.publications {
    create<MavenPublication>("depsList") {
        artifact(generateDependenciesList) {
            group = "org.teavm"
            artifactId = "teavm-devserver-runner"
            classifier = "dependencies"
            extension = "txt"
        }
    }
}