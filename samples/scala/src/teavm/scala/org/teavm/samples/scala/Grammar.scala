package org.teavm.samples.scala

object Grammar {
  def rule[T](f: Seq[Char] => (Option[T], Seq[Char])): Rule[T] = Rule.rule(f)

  def s(str: String): Rule[String] = Rule.expect(str)

  def range(first: Char, last: Char): Rule[Char] = Rule.range(first, last)
}

trait Rule[T] {
  def parse(chars: Seq[Char]): (Option[T], Seq[Char])

  def ~[S](other: Rule[S]): Rule[(T, S)] = Rule.concat(this, other)

  def ~(other: => String): Rule[(T, String)] = Rule.concat(this, Rule.expect(other))

  def |(other: => Rule[T]): Rule[T] = Rule.firstOf(this, other)

  def * : Rule[List[T]] = Rule.unlimited(this)

  def >>[S](f: T => S): Rule[S] = Rule.andThen(this, f)

  def ? : Rule[Option[T]] = Rule.optional(this)

  def debug(str: String) = Rule.rule { x =>
    val (result, rem) = parse(x)
    println(str + ":" + result + ":" + rem)
    (result, rem)
  }
}

object Rule {
  def rule[T](f: Seq[Char] => (Option[T], Seq[Char])): Rule[T] = (chars: Seq[Char]) => f(chars)

  def concat[S, T](left: Rule[S], right: => Rule[T]): Rule[(S, T)] = {
    rule(chars => left.parse(chars) match {
      case (Some(leftResult), rem) => right.parse(rem) match {
        case (Some(rightResult), rem2) => (Some((leftResult, rightResult)), rem2)
        case (None, _) => (None, rem)
      }
      case (None, rem) => (None, rem)
    })
  }

  def unlimited[T](inner: Rule[T]): Rule[List[T]] = {
    def iter(chars: Seq[Char]): (List[T], Seq[Char]) = {
      inner.parse(chars) match {
        case (Some(result), rem) =>
          val (tail, rem2) = iter(rem)
          (result :: tail, rem2)
        case (None, rem) => (Nil, rem)
      }
    }

    rule(chars => {
      val (result, rem) = iter(chars)
      (Some(result), rem)
    })
  }

  def firstOf[T](first: Rule[T], second: => Rule[T]): Rule[T] = {
    rule(chars => first.parse(chars) match {
      case (Some(result), rem) => (Some(result), rem)
      case (None, _) => second.parse(chars) match {
        case (Some(result), rem) => (Some(result), rem)
        case (None, _) => (None, chars)
      }
    })
  }

  def optional[T](inner: Rule[T]): Rule[Option[T]] = {
    rule(chars => inner.parse(chars) match {
      case (Some(result), rem) => (Some(Some(result)), rem)
      case (None, rem) => (Some(None), chars)
    })
  }

  implicit def expect(str: String): Rule[String] = {
    def iter(chars: Seq[Char], index: Int): (Boolean, Seq[Char]) = {
      if (index == str.length())
        (true, chars)
      else chars match {
        case Seq() => (false, Nil);
        case c +: tail if c == str.charAt(index) => iter(tail, index + 1)
        case _ => (false, chars)
      }
    }

    rule(chars => {
      val (matched, rem) = iter(chars, 0)
      (if (matched) Some(str) else None, rem)
    })
  }

  def andThen[T, S](inner: Rule[T], f: T => S): Rule[S] = {
    rule(chars => inner.parse(chars) match {
      case (Some(result), rem) => (Some(f(result)), rem)
      case (None, rem) => (None, rem)
    })
  }

  def range(first: Char, last: Char): Rule[Char] = {
    rule {
      case c +: rem if c >= first && c <= last => (Some(c), rem)
      case chars => (None, chars)
    }
  }
}