package org.teavm.samples.scala

import org.teavm.jso.dom.events._
import java.util.function.Consumer

object DOM {
  implicit def toEventListener[T <: Event](f : T => Any) : EventListener[T] = {
    new EventListener[T]() {
      def handleEvent(e : T) = f(e)
    }
  }

  implicit def toConsumer[T](f : T => Any) : Consumer[T] = {
    new Consumer[T]() {
      def accept(e : T) = f(e)
    }
  }
}