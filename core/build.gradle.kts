import com.github.gradle.node.npm.task.NpmTask

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
    alias(libs.plugins.nodejs)
}

description = "Compiler, backends and runtime"

node {
    download = providers.gradleProperty("teavm.localNodeJS")
        .map { it == "true" }
        .map { !it }
        .orElse(true)
}

dependencies {
    api(project(":interop:core"))
    api(project(":metaprogramming:api"))

    implementation(libs.asm.commons)
    implementation(libs.asm.util)
    implementation(libs.hppc)
    implementation(libs.rhino)

    compileOnly(libs.jackson.annotations)

    testImplementation(libs.junit)
}

val jsOutputDir = layout.buildDirectory.dir("generated/js")
val jsOutputPackageDir = jsOutputDir.map { it.dir("org/teavm/backend/wasm") }
val jsInputDir = layout.projectDirectory.dir("src/main/js/wasm-gc-runtime")
val jsInput = jsInputDir.file("runtime.js")

fun registerRuntimeTasks(taskName: String, wrapperType: String, outputName: String) {
    val generateTask by tasks.register<DefaultTask>("generate${taskName}Runtime") {
        dependsOn(tasks.npmInstall)
        val wrapperFile = jsInputDir.file(wrapperType)
        val runtimeFile = jsInput
        val outputFile = jsOutputPackageDir.map { it.file("$outputName.js") }
        inputs.files(wrapperFile, runtimeFile)
        outputs.file(outputFile)
        doLast {
            val wrapper = wrapperFile.asFile.readText()
            var runtime = runtimeFile.asFile.readText()
            val startText = "// !BEGINNING!\n"
            val startIndex = runtime.indexOf(startText)
            if (startIndex >= 0) {
                runtime = runtime.substring(startIndex + startText.length)
            }
            outputFile.get().asFile.writeText(wrapper.replace("include();", runtime))
        }
    }

    val optimizeTask = tasks.register<NpmTask>("optimize${taskName}Runtime") {
        dependsOn(tasks.npmInstall)
        val inputFiles = generateTask.outputs.files
        val outputFile = jsOutputPackageDir.map { it.file("$outputName.min.js") }
        inputs.files(inputFiles)
        outputs.file(outputFile)
        npmCommand.addAll("run", "uglify")
        args.addAll(provider {
            listOf(
                "--",
                "-m", "--module", "--toplevel",
                inputFiles.singleFile.absolutePath,
                "-o", outputFile.get().asFile.absolutePath
            )
        })
    }

    sourceSets.main.configure {
        output.dir(mapOf("builtBy" to generateTask), jsOutputDir)
        output.dir(mapOf("builtBy" to optimizeTask), jsOutputDir)
    }
}

registerRuntimeTasks("Simple", "simple-wrapper.js", "wasm-gc-runtime")
registerRuntimeTasks("Module", "module-wrapper.js", "wasm-gc-module-runtime")

teavmPublish {
    artifactId = "teavm-core"
}