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

