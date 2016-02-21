TeaVM, GWT, HotSpot, Bck2Brwsr JBox2D Benchmark
===============================================

Compares the speed of execution on a complex [JBox2D](http://www.jbox2d.org/) CPU extensive
computation. JavaScript produced by TeaVM and GWT can be compared by running 

```
$ mvn clean install
```

then just open the generated HTML versions

```
$ open target/teavm-samples-benchmark-*-SNAPSHOT/index.html 
```

In addition to that one can run the same demo with classical HotSpot virtual machine. Just try:

```
$ mvn -Pfx exec:java
```


