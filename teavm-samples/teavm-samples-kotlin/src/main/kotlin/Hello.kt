package org.teavm.samples.kotlin

import org.teavm.jso.*
import org.teavm.dom.browser.*
import org.teavm.dom.html.*
import org.teavm.dom.events.*

fun main(args : Array<String>) {
    var window = JS.getGlobal() as Window;
    var document = window.getDocument();

    document.getElementById("hello-kotlin").addEventListener("click", EventListener<MouseEvent>() {
       window.alert("Hello, developer!");
    })
}
