package org.teavm.samples.scala

import org.teavm.samples.scala.Grammar._

object Calculator {
  def eval(expr : Expr) : BigInt = expr match {
    case Add(a, b) => eval(a) + eval(b)
    case Subtract(a, b) => eval(a) - eval(b)
    case Multiply(a, b) => eval(a) * eval(b)
    case Divide(a, b) => eval(a) / eval(b)
    case Negate(n) => -eval(n)
    case Number(v) => v
  }

  def print(expr : Expr) : String = expr match {
    case Add(a, b) => "(" + print(a) + " + " + print(b) + ")"
    case Subtract(a, b) => "(" + print(a) + " - " + print(b) + ")"
    case Multiply(a, b) => "(" + print(a) + " * " + print(b) + ")"
    case Divide(a, b) => "(" + print(a) + " / " + print(b) + ")"
    case Negate(n) => "-" + print(n)
    case Number(v) => v.toString()
  }

  def parse(str : Seq[Char]) = additive.parse(str)

  def additive = multiplicative ~ ((keyword("+") | keyword("-")) ~ multiplicative).* >> {
    case (h, t) => t.foldLeft(h) {
      case (left, ("+", right)) => Add(left, right)
      case (left, ("-", right)) => Subtract(left, right)
    }
  }
  def multiplicative = unary ~ ((keyword("*") | keyword("/")) ~ unary).* >> {
    case (h, t) => t.foldLeft(h) {
      case (left, ("*", right)) => Multiply(left, right)
      case (left, ("/", right)) => Divide(left, right)
    }
  }
  def unary : Rule[Expr] = keyword("-").? ~ primitive >> {
    case (Some(_), v) => Negate(v)
    case (None, v) => v
  }
  def primitive : Rule[Expr] = Rule.firstOf(number >> Number, group)
  def group : Rule[Expr] = keyword("(") ~ additive ~ keyword(")") >> { case ((_, result), _) => result }

  def number : Rule[Int] = range('1', '9') ~ range('0', '9').* ~ ws >> {
    case ((h, t), _) => t.foldLeft(h - '0')((n, c) => n * 10 + (c - '0'))
  }

  def keyword(str : String) = s(str) ~ ws >> { case (s, _) => s }
  def ws = s(" ").* >> { case _ => Unit }
}

sealed abstract class Expr
case class Number(value : Int) extends Expr
case class Add(left : Expr, right : Expr) extends Expr
case class Subtract(left : Expr, right : Expr) extends Expr
case class Multiply(left : Expr, right : Expr) extends Expr
case class Divide(left : Expr, right : Expr) extends Expr
case class Negate(argument : Expr) extends Expr