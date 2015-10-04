package org.teavm.samples.kotlin

import org.teavm.jso.*
import org.teavm.jso.browser.*
import org.teavm.jso.dom.html.*
import org.teavm.jso.dom.events.*

fun main(args : Array<String>) {
    var document = Window.current().getDocument();

    document.getElementById("hello-kotlin").addEventListener("click", EventListener<MouseEvent>() {
       Window.alert("Hello, developer!");
    })
}
