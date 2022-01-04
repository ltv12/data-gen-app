package com.evolution.datagen

import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Semaphore
import cats.effect.{ExitCode, IO, IOApp}
import com.evolution.datagen.generator.{Gen, RateLimiter}
import com.evolution.datagen.model.DataModel.ObjectField

object GeneratorApp extends IOApp {

  import cats.syntax.parallel._
  import com.evolution.datagen.model.DataModel.Protocol._
  import io.circe.syntax._

  def process: IO[Unit] = {
    val eventsNumber = 1000000
    for {
      gen         <- Gen.of("data_spec_example.yaml")
      clock_start <- timer.clock.monotonic(TimeUnit.MILLISECONDS)
      semaphore   <- Semaphore[IO](eventsNumber)
      rateLimitedFunction = RateLimiter.of(semaphore, () => IO.delay(gen.sample.asInstanceOf[ObjectField].asJson.noSpaces))
      allResults  <- Range(0, eventsNumber).toList.parTraverse(_ => rateLimitedFunction)
      clock_end   <- timer.clock.monotonic(TimeUnit.MILLISECONDS)
      _           <- IO(println(s"sample of data ${allResults.take(10).mkString(System.lineSeparator)}."))
      _           <- IO(println(s"generation of ${allResults.size} taken ${clock_end - clock_start} millisec"))
      _           <- IO(println(s"Size is ${allResults.size}"))
    } yield ()

  }

  override def run(args: List[String]): IO[ExitCode] = {
    process.as(ExitCode.Success)
  }
}
