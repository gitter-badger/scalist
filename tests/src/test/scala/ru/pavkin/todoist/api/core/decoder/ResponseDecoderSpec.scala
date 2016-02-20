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

  val smthCommandDecoder = SingleCommandResponseDecoder.using[Id, Smth, Smth, Boolean] {
    (smth: Smth, n: Smth) => smth.n == n.n
  }
  val smthStringLengthDecoder = SingleCommandResponseDecoder.using[Id, String, Smth, String] {
    (command: String, base: Smth) => (base.n + command.length).toString
  }

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

  //  test("ResponseDecoder implicit composition") {
  //    implicit val p1 = intParser
  //    implicit val p2 = intLengthParser
  //    val p3 = implicitly[SingleResponseDecoder.Aux[Id, String, Long]]
  //
  //    check { (a: String) =>
  //      p3.parse(a) == intParser.compose(intLengthParser).parse(a)
  //    }
  //
  //    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Boolean]]""")
  //  }

  test("ResponseDecoder composition with multiple") {
    check { (a: String) =>
      intParser.compose(intLengthParser.combine(smthParser)).parse(a) ==
        intLengthParser.combine(smthParser).parse(intParser.parse(a))
    }
  }


//  test("ResponseDecoder implicit composition with multiple") {
//    implicit val p1 = intParser
//    implicit val p2 = intLengthParser
//    implicit val p3 = smthParser
//
//    val p4 = implicitly[MultipleResponseDecoder.Aux[Id, String, Smth :: Long :: HNil]]
//    implicitly[MultipleResponseDecoder.Aux[Id, String, Long :: Smth :: HNil]]
//
//    check { (a: String) =>
//      p4.parse(a) == intParser.compose(intLengthParser.combine(smthParser)).parse(a)
//    }
//
//    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Smth :: Boolean :: HNil]]""")
//    illTyped("""implicitly[SingleResponseDecoder.Aux[Id, String, Boolean :: Long :: HNil]]""")
//  }

  test("ResponseDecoder composition with single command decoder") {
    check { (s: Int, a: Int) =>
      smthParser.compose(smthCommandDecoder).parse(Smth(s))(a) == (s == a)
    }
  }

  //  test("ResponseDecoder implicit composition single command decoder") {
  //    implicit val p1 = smthParser
  //    implicit val p2 = smthCommandDecoder
  ////    implicit val notNeeded1 = intLengthParser
  ////    implicit val notNeeded2 = identityParser
  //
  //    val p3 = implicitly[SingleCommandResponseDecoder.Aux[Id, Smth, Int, Boolean]]
  //
  //    check { (s: Int, a: Int) =>
  //      p3.parse(Smth(s))(a) == smthParser.compose(smthCommandDecoder).parse(Smth(s))(a)
  //    }
  //
  //    illTyped("""implicitly[SingleCommandResponseDecoder.Aux[Id, Int, Int, Boolean]]""")
  //    illTyped("""implicitly[SingleCommandResponseDecoder.Aux[Id, Smth, String, Boolean]]""")
  //    illTyped("""implicitly[SingleCommandResponseDecoder.Aux[Id, Smth, Int, String]]""")
  //  }

  test("ResponseDecoder composition with multiple command decoder") {
    check { (c1: Int, c2: String, base: Int) =>
      smthParser.compose(
        smthCommandDecoder.combine(smthStringLengthDecoder)
      ).parse(c2 :: Smth(c1) :: HNil)(base) == {
        val nBase = smthParser.parse(base)
        smthStringLengthDecoder.parse(c2)(nBase) :: smthCommandDecoder.parse(Smth(c1))(nBase) :: HNil
      }
    }
  }

  test("ResponseDecoder implicit composition multiple command decoder") {
    implicit val p1 = smthParser
    implicit val p2 = smthCommandDecoder
    implicit val p3 = smthStringLengthDecoder

    val p4 = implicitly[MultipleCommandResponseDecoder.Aux[Id, String :: Smth :: HNil, Int, String :: Boolean :: HNil]]

    check { (c1: Int, c2: String, base: Int) =>
      val command = c2 :: Smth(c1) :: HNil
      p4.parse(command)(base) ==
        smthParser.compose(smthCommandDecoder.combine(smthStringLengthDecoder)).parse(command)(base)
    }

    implicitly[MultipleCommandResponseDecoder.Aux[Id, Smth :: String :: HNil, Int, Boolean :: String :: HNil]]

    illTyped("""implicitly[MultipleCommandResponseDecoder.Aux[Id, String :: Smth :: HNil, Int, Boolean :: String :: HNil]]""")
    illTyped("""implicitly[MultipleCommandResponseDecoder.Aux[Id, Smth :: String :: HNil, Int,String :: Boolean ::  HNil]]""")
    illTyped("""implicitly[MultipleCommandResponseDecoder.Aux[Id, Int :: String :: HNil, Int, Boolean :: String :: HNil]]""")
    illTyped("""implicitly[MultipleCommandResponseDecoder.Aux[Id, Smth :: String :: HNil, Int, Int :: String :: HNil]]""")
  }
}
