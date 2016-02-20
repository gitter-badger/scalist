package ru.pavkin.todoist.api.core.decoder

import cats.{FlatMap, Apply, Functor}
import shapeless.{HNil, ::, HList}
import cats.syntax.apply._
import cats.syntax.functor._

trait MultipleResponseDecoder[F[_], Base] extends ResponseDecoder[F, Base] {self =>
  type Out <: HList

  def combine[Out2](other: ResponseDecoder.Aux[F, Base, Out2])
                   (implicit A: Apply[F]): MultipleResponseDecoder.Aux[F, Base, Out2 :: self.Out] =
    new MultipleResponseDecoder[F, Base] {
      type Out = Out2 :: self.Out
      def parse(resource: Base): F[Out] = self.parse(resource).map2(other.parse(resource))((a, b) => b :: a)
    }
}

object MultipleResponseDecoder {
  type Aux[F[_], Base, Out0 <: HList] = MultipleResponseDecoder[F, Base] {type Out = Out0}

  def using[F[_], Base, Out0 <: HList](f: Base => F[Out0]): Aux[F, Base, Out0] = new MultipleResponseDecoder[F, Base] {
    type Out = Out0
    def parse(resource: Base): F[Out] = f(resource)
  }

  implicit def singleHListParser[F[_] : Functor, Base, Out0](implicit p: SingleResponseDecoder.Aux[F, Base, Out0])
  : MultipleResponseDecoder.Aux[F, Base, Out0 :: HNil] =
    new MultipleResponseDecoder[F, Base] {
      type Out = Out0 :: HNil
      def parse(resource: Base): F[Out0 :: HNil] = p.parse(resource).map(_ :: HNil)
    }

  implicit def recurse[F[_] : Apply, Base, OutH, OutT <: HList]
  (implicit
   h: SingleResponseDecoder.Aux[F, Base, OutH],
   t: MultipleResponseDecoder.Aux[F, Base, OutT]): MultipleResponseDecoder.Aux[F, Base, OutH :: OutT] =
    new MultipleResponseDecoder[F, Base] {
      type Out = OutH :: OutT
      def parse(resource: Base): F[OutH :: OutT] = t.combine(h).parse(resource)
    }

  implicit def decoderComposer[F[_] : FlatMap, Base, Out0, OutL <: HList]
  (implicit
   p1: SingleResponseDecoder.Aux[F, Base, Out0],
   p2: MultipleResponseDecoder.Aux[F, Out0, OutL]): MultipleResponseDecoder.Aux[F, Base, OutL] =
    p1.compose(p2)

}
