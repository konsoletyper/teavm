package org.teavm.samples.scala

import org.teavm.samples.scala.DOM._
import org.teavm.samples.scala.Calculator.parse
import org.teavm.samples.scala.Calculator.print
import org.teavm.samples.scala.Calculator.eval
import org.teavm.jso.browser.Window
import org.teavm.jso.dom.html._
import org.teavm.jso.dom.events._

object Client {
  def main(args: Array[String]) {
    var doc = HTMLDocument.current
    var exprElem = doc.getElementById("expr").asInstanceOf[HTMLInputElement]
    var calcElem = doc.getElementById("calculate")
    var resultList = doc.getElementById("result-list");
    calcElem.listenClick((e : MouseEvent) => {
       parse(exprElem.getValue().toSeq) match {
         case (None, _) => Window.alert("Error parsing expression");
         case (Some(x), Nil) => {
           resultList.insertBefore(doc.createElement("div", (elem : HTMLElement) => {
             elem.withChild("span", (child : HTMLElement) =>
                 child.withAttr("class", "plan").withText(print(x) + " = "))
             elem.withChild("span", (child : HTMLElement) =>
                 child.withAttr("class", "result").withText(eval(x).toString))
           }), resultList.getFirstChild)
         }
         case (_, err) => Window.alert("Error parsing expression: " + err);
       }
    })
  }
}
