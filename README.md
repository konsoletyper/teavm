# TeaVM

[![.github/workflows/ci.yml](https://github.com/konsoletyper/teavm/actions/workflows/ci.yml/badge.svg)](https://github.com/konsoletyper/teavm/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.teavm/teavm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.teavm/teavm-maven-plugin) 
[![Download](https://teavm.org/maven/latestBadge.svg)](https://teavm.org/maven/_latest)
[![Discord](https://img.shields.io/discord/1368634309849911386
)](https://discord.gg/QvcBHmdE5J)

See documentation at the [project web site](https://teavm.org/).

Useful links:

* [Getting started](https://teavm.org/docs/intro/getting-started.html)
* [Gallery](https://teavm.org/gallery.html)
* [Site source code repository](https://github.com/konsoletyper/teavm-site)
* [Discussion on Google Groups](https://groups.google.com/forum/#!forum/teavm)


## Building TeaVM

Simply clone source code (`git clone https://github.com/konsoletyper/teavm.git`)
and run Gradle build (`./gradlew publishToMavenLocal` or `gradlew.bat publishToMavenLocal`).
You should build samples separately, as described in [corresponding readme file](samples/README.md).


### Useful Gradle tasks

* `:tools:classlib-comparison-gen:build` &ndash; build Java class library compatibility report.
  result is available at: `tools/classlib-comparison-gen/build/jcl-support`


## Embedding TeaVM

If you are not satisfied with Maven, you can embed TeaVM in your program 
or even create your own plugin for any build tool, like Ant or Gradle.
The starting point for you may be `org.teavm.tooling.TeaVMTool` class from `teavm-tooling` artifact. 
You may want to go deeper and use `org.teavm.vm.TeaVM` from `teavm-core` artifact, learn how `TeaVMTool` initializes it. 
To learn how to use `TeaVMTool` class itself, find its usages across project source code. 
You most likely encounter Maven and IDEA plugins.
  
Please, notice that these APIs for embedding are still unstable and may change between versions.


## License
 
TeaVM is distributed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
TeaVM does not rely on OpenJDK or code or other (L)GPL code.
TeaVM has its own reimplementation of Java class library, which is either implemented from scratch or
based on non-(L)GPL projects:

* [Apache Harmony](https://harmony.apache.org/) (Apache 2.0)
* [Joda-Time](https://github.com/JodaOrg/joda-time) (Apache 2.0)
* [jzlib](https://github.com/ymnk/jzlib) (BSD style license)

If you want to contribute code to implementation of Java class library, 
please make sure it's not based on OpenJDK or other code licensed under (L)GPL.


## Feedback

More information is available at the official site: https://teavm.org.

Ask your questions by email: info@teavm.org. Also, you can report issues on a project's
[issue tracker](https://github.com/konsoletyper/teavm/issues).
