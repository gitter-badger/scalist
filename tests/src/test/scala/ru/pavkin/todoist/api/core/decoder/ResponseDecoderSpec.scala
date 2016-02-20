package ru.pavkin.todoist.api.core.decoder

import cats.Id
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import shapeless.test.illTyped
import shapeless.{::, HNil}

import scala.util.Try

class ResponseDecoderSpec extends FunSuite with Checkers {

  case class Smth(n: Int)
  val intParser = SingleResponseDecoder.using[Id, String, Int]((s: String) => Try(s.toInt).getOrElse(0))
  val doubleParser = SingleResponseDecoder.using[Id, String, Double]((s: String) => Try(s.toDouble).getOrElse(0.0))
  val intLengthParser = SingleResponseDecoder.using[Id, Int, Long]((s: Int) => s.toString.length.toLong)
  val identityParser = SingleResponseDecoder.using[Id, Boolean, Boolean]((s: Boolean) => s)
  val smthParser = SingleResponseDecoder.using[Id, Int, Smth]((n: Int) => Smth(n))

  test("ResponseDecoder") {
    implicit val p1 = intParser
    implicit val p2 = doubleParser

    implicitly[MultipleResponseDecoder.Aux[Id, String, Double :: Int :: HNil]]
    implicitly[MultipleResponseDecoder.Aux[Id, String, Int :: Double :: HNil]]
    implicitly[MultipleResponseDecoder.Aux[Id, String, Int :: HNil]]

    illTyped("""implicitly[MultipleResponseDecoder.Aux[Id, String, String :: Int :: HNil]]""")
  }

  test("ResponseDecoder identity") {
    check { (a: Boolean) => identityParser.parse(a) == a }
  }

  test("ResponseDecoder combination") {
    check { (a: String) =>
      intParser.combine(doubleParser).parse(a) == intParser.parse(a) :: doubleParser.parse(a) :: HNil
    }
  }

  test("ResponseDecoder composition") {
    check { (a: String) =>
      intParser.compose(intLengthParser).parse(a) == intLengthParser.parse(intParser.parse(a))
    }
  }

  test("ResponseDecoder implicit composition") {
    implicit val p1 = intParser
    implicit val p2 = intLengthParser
    val p3 = implicitly[SingleResponseDecoder.Aux[Id, String, Long]]

    check { (a: String) =>
      p3.parse(a) == intParser.compose(intLengthParser).parse(a)
    }

    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Boolean]]""")
  }

  test("ResponseDecoder composition with multiple") {
    check { (a: String) =>
      intParser.compose(intLengthParser.combine(smthParser)).parse(a) ==
        intLengthParser.combine(smthParser).parse(intParser.parse(a))
    }
  }


  test("ResponseDecoder implicit composition with multiple") {
    implicit val p1 = intParser
    implicit val p2 = intLengthParser
    implicit val p3 = smthParser

    val p4 = implicitly[MultipleResponseDecoder.Aux[Id, String, Smth :: Long :: HNil]]
    implicitly[MultipleResponseDecoder.Aux[Id, String, Long :: Smth :: HNil]]

    check { (a: String) =>
      p4.parse(a) == intParser.compose(intLengthParser.combine(smthParser)).parse(a)
    }

    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Smth :: Boolean :: HNil]]""")
    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Boolean :: Long :: HNil]]""")
  }
}
