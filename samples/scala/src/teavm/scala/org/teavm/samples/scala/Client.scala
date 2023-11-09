package org.teavm.samples.scala

import org.teavm.jso.browser.Window
import org.teavm.jso.dom.events._
import org.teavm.jso.dom.html._
import org.teavm.samples.scala.Calculator.{eval, parse, print}

object Client {
  def main(args: Array[String]): Unit = {
    val doc = HTMLDocument.current
    val exprElem = doc.getElementById("expr").asInstanceOf[HTMLInputElement]
    val calcElem = doc.getElementById("calculate")
    val resultList = doc.getElementById("result-list")
    calcElem.listenClick((e: MouseEvent) => {
      parse(exprElem.getValue.toSeq) match {
        case (None, _) => Window.alert("Error parsing expression");
        case (Some(x), Nil) =>
          resultList.insertBefore(doc.createElement("div", (elem: HTMLElement) => {
            elem.withChild("span", (child: HTMLElement) =>
              child.withAttr("class", "plan").withText(print(x) + " = "))
            elem.withChild("span", (child: HTMLElement) =>
              child.withAttr("class", "result").withText(eval(x).toString))
          }), resultList.getFirstChild)
        case (_, err) => Window.alert("Error parsing expression: " + err);
      }
    })
  }
}
