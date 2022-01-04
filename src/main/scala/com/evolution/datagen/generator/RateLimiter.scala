package com.evolution.datagen.generator

import cats.effect.concurrent.Semaphore
import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.duration._

object RateLimiter {
  def of[A, B](semaphore: Semaphore[IO], function: () => IO[B])(implicit T: Timer[IO], CS: ContextShift[IO]) = {
    for {
      _          <- semaphore.acquire
      timerFiber <- IO.sleep(1.second).start
      result     <- function()
      _          <- timerFiber.join
      _          <- semaphore.release
    } yield result
  }

}
//TODO: Make target version
//class RateLimiter[F[_]: Concurrent: Timer] {
//  import scala.concurrent.duration._
//  import cats.effect._
//
// import cats.syntax.flatMap._
// import cats.syntax.functor._
//  def use[A, B](semaphore: Semaphore[F], logic: () => F[B]): F[B] = {
//    val F = implicitly[Concurrent[F]]
//    val timer = implicitly[Timer[F]]
//    for {
//      _          <- semaphore.acquire
//      timerFiber <- F.start(timer.sleep(1.second))
//      result     <- logic()
//      _          <- timerFiber.join
//      _          <- semaphore.acquire
//    } yield result
//  }
//}
