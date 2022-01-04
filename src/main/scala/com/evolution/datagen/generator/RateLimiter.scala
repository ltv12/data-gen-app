package com.evolution.datagen.generator

import cats.effect.concurrent.Semaphore
import cats.syntax.flatMap._
import cats.syntax.functor._
import scala.concurrent.duration._
import cats.effect.{Concurrent, Timer}

object RateLimiter {

  def use[F[_]: Concurrent: Timer, A, B](semaphore: Semaphore[F], logic: () => F[B]): F[B] = {
    val F = implicitly[Concurrent[F]]
    val timer = implicitly[Timer[F]]
    for {
      _          <- semaphore.acquire
      timerFiber <- F.start(timer.sleep(1.second))
      result     <- logic()
      _          <- timerFiber.join
      _          <- semaphore.release
    } yield result
  }
}
