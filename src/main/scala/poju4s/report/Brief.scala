package poju4s.report

import poju4s.result._
import java.util.concurrent._

object Brief {
  def apply(toRun: List[() => Result]) = new Brief().apply(toRun)
}

class Brief extends Styled {

  def apply(toRun: List[() => Result]) = {
    def println(str: String) = System.out.println(str);System.out.flush
    def print(str: String) = System.out.print(str);System.out.flush

    val results = toRun.map(x => {
      x() match {
        case s: Success => print(style.success(".")); s
        case i: Ignored => print(style.ignored("I")); i
        case x: Fixed => print(style.fixed("X")); x
        case p: Pending => print(style.pending("P")); p
        case f: Failure => print(style.failure("F")); f
        case e: Error => print(style.error("E")); e
        case r => print("?"); r
      }
    })
    println("")
    val parent = this
    new Summary(results) {
      override val style = parent.style
    }
  }
}
