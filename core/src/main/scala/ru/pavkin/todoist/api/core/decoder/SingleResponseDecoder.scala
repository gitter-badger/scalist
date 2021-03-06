package ru.pavkin.todoist.api.core.decoder

import cats.{FlatMap, Apply}
import shapeless.{HList, HNil, ::}
import cats.syntax.flatMap._
import cats.syntax.apply._

trait SingleResponseDecoder[F[_], Base] extends ResponseDecoder[F, Base] {self =>
  def combine[Out2](other: ResponseDecoder.Aux[F, Base, Out2])
                   (implicit A: Apply[F]): MultipleResponseDecoder.Aux[F, Base, Out2 :: self.Out :: HNil] =
    new MultipleResponseDecoder[F, Base] {
      type Out = Out2 :: self.Out :: HNil
      def parse(resource: Base): F[Out] = self.parse(resource).map2(other.parse(resource))((a, b) => b :: a :: HNil)
    }

  def compose[Out2](other: SingleResponseDecoder.Aux[F, Out, Out2])
                   (implicit F: FlatMap[F]): SingleResponseDecoder.Aux[F, Base, Out2] =
    new SingleResponseDecoder[F, Base] {
      type Out = Out2
      def parse(resource: Base): F[Out] = self.parse(resource).flatMap(other.parse)
    }

  def compose[Out2 <: HList](other: MultipleResponseDecoder.Aux[F, Out, Out2])
                            (implicit F: FlatMap[F]): MultipleResponseDecoder.Aux[F, Base, Out2] =
    new MultipleResponseDecoder[F, Base] {
      type Out = Out2
      def parse(resource: Base): F[Out] = self.parse(resource).flatMap(other.parse)
    }

  def compose[Out2, Command]
  (other: SingleCommandResponseDecoder.Aux[F, Command, Out, Out2])
  (implicit F: FlatMap[F]): SingleCommandResponseDecoder.Aux[F, Command, Base, Out2] =
    new SingleCommandResponseDecoder[F, Command, Base] {
      type Out = Out2
      def parse(command: Command)(resource: Base): F[Out] =
        self.parse(resource).flatMap(other.parse(command))
    }

  def compose[Out2 <: HList, Command <: HList]
  (other: MultipleCommandResponseDecoder.Aux[F, Command, Out, Out2])
  (implicit F: FlatMap[F]): MultipleCommandResponseDecoder.Aux[F, Command, Base, Out2] =
    new MultipleCommandResponseDecoder[F, Command, Base] {
      type Out = Out2
      def parse(command: Command)(resource: Base): F[Out] =
        self.parse(resource).flatMap(other.parse(command))
    }
}


object SingleResponseDecoder {
  def apply[F[_], Base, Out0](implicit ev: Aux[F, Base, Out0]): Aux[F, Base, Out0] = ev

  type Aux[F[_], Base, Out0] = SingleResponseDecoder[F, Base] {type Out = Out0}

  def using[F[_], Base, Out0](f: Base => F[Out0]): Aux[F, Base, Out0] = new SingleResponseDecoder[F, Base] {
    type Out = Out0
    def parse(resource: Base): F[Out] = f(resource)
  }
}
