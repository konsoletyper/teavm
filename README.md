TeaVM
=====

What is TeaVM?
--------------

In short, TeaVM gets a bytecode, running over JVM, and translates it to the JavaScript code,
which does exactly the same thing as the original bytecode does.
It is based on its cross-compiler which transforms `class` files to JavaScript.
But there is something more:

  * a sophisticated per-method dependency manager, which greatly reduces the JavaScript output;
  * an optimizer capable of things like devirtualization, inlining, constant propagation,
    loop invariant motion and many other;
  * implementation of subset of core Java library;

How to use
----------

There is no TeaVM artifacts in the central Maven repository yet.
So first you need to clone project and install it into the local repository.
In order to install project, just run `mvn install` when you are in the project's root directory.

There are several options of using TeaVM. One is the maven build. First, you write your code as if it were an
ordinary Java project:

```Java
package org.teavm.samples;

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
```

Second, you include the following plugin in your `pom.xml` build section:

```XML
    <plugin>
      <groupId>org.teavm</groupId>
      <artifactId>teavm-maven-plugin</artifactId>
      <version>0.1</version>
      <dependencies>
        <dependency>
          <groupId>org.teavm</groupId>
          <artifactId>teavm-classlib</artifactId>
          <version>0.1</version>
        </dependency>
      </dependencies>
      <executions>
        <execution>
          <id>generate-javascript</id>
          <goals>
            <goal>build-javascript</goal>
          </goals>
          <phase>process-classes</phase>
          <configuration>
            <minifying>true</minifying>
            <mainClass>org.teavm.samples.HelloWorld</mainClass>
            <mainPageIncluded>true</mainPageIncluded>
          </configuration>
        </execution>
      </executions>
    </plugin>
```

Now you can execute `mvn clean package` and get the generated JavaScript files in `target/javascript` folder.
Just open `target/javascript/main.html` page in your browser, open developer's console and press *Refresh* and
see what happen.

There is [teavm-samples](teavm-samples) module,
containing a complete buildable and runnable example.

DukeScript
----------

[DukeScript](http://wiki.apidesign.org/wiki/DukeScript) is a set of APIs that allows Java applications
easily talk to JavaScript environment to (usually) animate an HTML page. While DukeScript has its own
implementation of JVM, called [Bck2Brwsr](http://wiki.apidesign.org/wiki/Bck2Brwsr), TeaVM also provides
support for running DukeScript applications, using [teavm-html4j](teavm-html4j) plugin.

Live example
------------

Thanks to [Jaroslav Tulach](http://wiki.apidesign.org/wiki/User:JaroslavTulach), author of DukeScript, we have several
DukeScript example applications. One is the minesweeper game.
You can try its TeaVM-compiled version [here](http://xelfi.cz/minesweeper/teavm/), and then take a look at
[source code](http://source.apidesign.org/hg/html~demo/file/4dce5ea7e13a/minesweeper/src/main/java/org/apidesign/demo/minesweeper/MinesModel.java)
and [HTML page](http://source.apidesign.org/hg/html~demo/file/4dce5ea7e13a/minesweeper/src/main/webapp/pages/index.html).

Advantages over GWT
-------------------

You may notice that TeaVM idea is much similar to GWT. So why we need TeaVM instead of GWT?

Unlinke GWT, TeaVM gets the compiled bytecode, not Java sources.
Thereby it **does not depend on a specific language syntax**. Even not on a specific language.
So, when the next Java version gets a new feature, you can use it in your source code 
and TeaVM compiler remains unbroken. Also you may want thigs Scala, Kotlin or Ceilon. TeaVM supports them.

To represent a source code, GWT uses abstract syntax trees (AST).
TeaVM uses control flow graph (CFG) of methods. CFG are much easier to optimize,
so TeaVM **applies aggressive optimizations** to you code to make it running faster.

TeaVM compiler is faster. And TeaVM does not produce permutations.
So with TeaVM you have no permutation explosion problem.

Advantages over JavaScript
--------------------------

JavaScript suffers of its dynamic typing. When you write a new code, dynamic typing accelerates
the development process, allowing you to write less boilerplate code.
But when you are to maintain a large code base, you may need static typing.
Also, it is not dynamic typing that really makes code short.
Good static typed languages can [infer variable types for you](http://en.wikipedia.org/wiki/Type_inference).
And they usually have a lot more useful features like [lambda functions](http://en.wikipedia.org/wiki/Lambda_function),
[lexical closures](http://en.wikipedia.org/wiki/Closure_%28computer_science%29),
[implicit type casting](http://en.wikipedia.org/wiki/Type_conversion#Implicit_type_conversion), etc.

With JavaScript you sometimes have to include large library for only one feature. Or you include many different
libraries for different purposes and your project size grows. TeaVM translates only the methods which
are really needed. So you can depend on as much libraries as you want and get 

With JavaScript you are limited to one language. TeaVM supports many of the JVM languages.

