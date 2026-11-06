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

description = "A core module for creating TeaVM compler in Wasm"

val classlibInput = configurations.create("classlibInput") {
    isTransitive = false
}
val classlibAuxInput = configurations.create("classlibAuxInput") {
    isTransitive = false
}
val classlibEmu = sourceSets.create("classlibEmu")

dependencies {
    api(project(":core"))
    implementation(project(":extension:spi"))
    implementation(project(":jso:impl"))
    implementation(project(":platform"))
    implementation(project(":classlib"))
    implementation(libs.asm.commons)

    classlibInput(project(":classlib"))
    classlibInput(project(":jso:core"))
    classlibInput(project(":jso:apis"))
    classlibAuxInput(project(":core"))
    classlibAuxInput(project(":jso:impl"))
    classlibAuxInput(project(":interop:core"))
    classlibAuxInput(project(":platform"))
    classlibAuxInput(project(":platform"))
    classlibAuxInput(libs.jzlib)
}

val classLibTmpDir = project.layout.buildDirectory.dir("tmp/teavm-classlib")

val unpackTeaVMClasslib = tasks.register<Copy>("unpackTeaVMClassLib") {
    dependsOn(classlibInput)
    from(provider {
        classlibInput.map { zipTree(it) }
    })
    exclude("META-INF/MANIFEST.MF")
    into(classLibTmpDir)
}

val unpackCldr = tasks.register<Copy>("unpackCldr") {
    dependsOn(unpackTeaVMClasslib)
    from(classLibTmpDir.map { it.file("org/teavm/classlib/impl/unicode/cldr-json.zip") }.map { zipTree(it) })
    into(project.layout.buildDirectory.dir("tmp/cldr"))
}

val repackCldr = tasks.register<Zip>("repackCldr") {
    from(unpackCldr)
    destinationDirectory = project.layout.buildDirectory.dir("tmp")
    archiveFileName = "cldr-json.zip"
    include("root/**")
    include("en/**")
    include("supplemental/**")
}

val prepareTeaVMClassLib = tasks.register<Copy>("prepareTeaVMClassLib") {
    dependsOn(unpackTeaVMClasslib, repackCldr, classlibAuxInput)
    from(unpackTeaVMClasslib) {
        exclude("org/teavm/classlib/impl/unicode/cldr-json.zip")
    }
    from(provider {
        classlibAuxInput.map {
            zipTree(it)
        }
    }) {
        include("org/teavm/classlib/**")
        include("org/teavm/platform/**")
        include("org/teavm/jso/**")
        include("org/teavm/runtime/**")
        include("org/teavm/interop/**")
        include("org/teavm/backend/wasm/runtime/**")
        include("org/teavm/backend/wasm/WasmRuntime*.class")
        include("org/teavm/backend/wasm/WasmHeap*.class")
        include("com/jcraft/jzlib/**")
    }
    from(repackCldr) {
        eachFile {
            path = "org/teavm/classlib/impl/unicode/" + path
            if (path == "META-INF/MANIFEST.MF") {
                exclude()
            }
        }
    }
    into(project.layout.buildDirectory.dir("tmp/classlib-runtime"))
}

val buildTeaVMClassLib = tasks.register<JavaExec>("buildTeaVMClassLib") {
    dependsOn(prepareTeaVMClassLib)
    // Don't use full runtime classpath here, since it includes resources, which in turn include output of this task
    classpath(sourceSets.main.map { it.output.classesDirs })
    val inputDir = project.layout.buildDirectory.dir("tmp/classlib-runtime")
    val outputFile = project.layout.buildDirectory.file("classlib/runtime-classlib-teavm.bin")
    inputs.dir(inputDir)
    outputs.file(outputFile)
    argumentProviders.add {
        listOf(
            inputDir.get().asFile.absolutePath,
            outputFile.get().asFile.absolutePath
        )
    }
    mainClass = "org.teavm.compiler.bootstrap.tools.ArchiveBuilder"
}

tasks.processResources {
    from(buildTeaVMClassLib) {
        into("org/teavm/compiler/bootstrap")
    }
}

teavmPublish {
    artifactId = "teavm-bootstrap-compiler"
}
