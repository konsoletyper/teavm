TeaVM, GWT, HotSpot, JBox2D Benchmark
===============================================

Compares the speed of execution on a complex [JBox2D](http://www.jbox2d.org/) CPU extensive
computation. JavaScript produced by TeaVM and GWT can be compared by running 

```
$ gradle appStartWar
```

Also, in Linux it's possible to build and run native file:

```
$ gradle runNativeLinux
```

Please, refer to your Linux distribution documentation to find out how to install required dependencies 
(cmake and gtk-devel).