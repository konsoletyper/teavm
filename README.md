TeaVM
=====
 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.teavm/teavm-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.teavm/teavm-maven-plugin) 
[![Download](https://teavm.org/maven/latestBadge.svg)](https://teavm.org/maven/_latest)
[![Gitter chat](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/teavm/Lobby)
[![Code Quality: Java](https://img.shields.io/lgtm/grade/java/g/konsoletyper/teavm.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/konsoletyper/teavm/context:java)
[![Total Alerts](https://img.shields.io/lgtm/alerts/g/konsoletyper/teavm.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/konsoletyper/teavm/alerts)

See documentation at the [project web site](https://teavm.org/).

Useful links:

* [Getting started](https://teavm.org/docs/intro/getting-started.html)
* [Gallery](https://teavm.org/gallery.html)
* [Flavour source code repository](https://github.com/konsoletyper/teavm-flavour)
* [Site source code repository](https://github.com/konsoletyper/teavm-site)
* [Discussion on Google groups](https://groups.google.com/forum/#!forum/teavm)


Building TeaVM
--------------

Simply clone source code (`git clone https://github.com/konsoletyper/teavm.git`) and run maven build (`mvn clean install`). You can do things a little faster (`mvn clean -DskipTests`) or even a little more faster (`mvn clean -DskipTests -Dteavm.build.all=false`).


Preview builds
--------------

You may want to access new features and don't want to wait until stable release is published on Maven Central. In this case you can get latest development build from [bintray](https://bintray.com/konsoletyper/teavm/teavm-dev). All you need is to put the following in your `pom.xml`:

```xml
  <repositories>
    <repository>
      <id>teavm-dev</id>
      <url>https://teavm.org/maven/repository</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>teavm-dev</id>
      <url>https://teavm.org/maven/repository</url>
    </pluginRepository>
  </pluginRepositories>
```

IDEA plugin is also available in preview builds. You need to add corresponding repository manually to IDEA. Open *Settings* -> *Plugins* -> *Browse repositories...* -> *Manage repositories...*, click *Add* button and enter `https://teavm.org/idea/dev/teavmRepository.xml`. Then get back to *Browse repositories* and pick TeaVM plugin from list. 


Embedding TeaVM
---------------

If you are not satisfied with Maven, you can embed TeaVM in your program or even create your own plugin for any build tool, like Ant or Gradle. The starting point for you may be `org.teavm.tooling.TeaVMTool` class from `teavm-tooling` artifact. You may want to go deeper and use `org.teavm.vm.TeaVM` from `teavm-core` artifact, learn how `TeaVMTool` initializes it. To learn how to use `TeaVMTool` class itself, find its usages across project source code. You most likely encounter Maven and IDEA plugins.
  
Please, notice that these APIs for embedding are still unstable and may change between versions.


WebAssembly
-----------

WebAssembly support is in experimental status. It may lack major features available in JavaScript backend. There's no documentation yet and you should do many things by hands (like embedding generated `wasm` file into your page, importing JavaScript objects, etc). Look at [samples/benchmark](https://github.com/konsoletyper/teavm/blob/master/samples/benchmark/) module. You should first examine `pom.xml` file to learn how to build `wasm` file from Java. Then you may want to examine `index-teavm.html` and `index-teavm.js` to learn how to embed WebAssembly into your web page.


License
-------
 
TeaVM is distributed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
TeaVM does not rely on OpenJDK or code or other (L)GPL code.
TeaVM has its own reimplementation of Java class library, which is either implemented from scratch or based on non-(L)GPL projects:

* [Apache Harmony](https://harmony.apache.org/) (Apache 2.0)
* [Joda-Time](https://github.com/JodaOrg/joda-time) (Apache 2.0)
* [jzlib](https://github.com/ymnk/jzlib) (BSD style license)

If you want to contribute code to implementation of Java class library, please make sure it's no based on OpenJDK or other code licensed under (L)GPL.


Feedback
--------

More information is available at the official site: https://teavm.org.

Ask your questions by email: info@teavm.org. Also you can report issues on a project's [issue tracker](https://github.com/konsoletyper/teavm/issues).
