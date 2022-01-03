package com.evolution.datagen

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Semaphore
import cats.effect.{ContextShift, ExitCode, IO, IOApp, Timer}
import com.evolution.datagen.generator.Gen
import com.evolution.datagen.model.DataModel.ObjectField
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec.{
  BoolFieldSpec,
  MonotonicIDFieldSpec,
  NumericFieldSpec,
  StringFieldSpec,
  TimestampFieldSpec,
  UUIDFieldSpec
}
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec

import scala.concurrent.duration._

object Utils {
  def rateLimited[A, B](semaphore: Semaphore[IO], function: () => IO[B])(implicit T: Timer[IO], CS: ContextShift[IO]) = {
    for {
      _          <- semaphore.acquire
      timerFiber <- IO.sleep(1.second).start
      result     <- function()
      _          <- timerFiber.join
      _          <- semaphore.release
    } yield result
  }

}

object GeneratorApp extends IOApp {

  val dataGen = Gen.from(
    ObjectSpec(
      "event",
      List(
        MonotonicIDFieldSpec("id"),
        UUIDFieldSpec("uuid"),
        StringFieldSpec("name"),
        BoolFieldSpec("is_new"),
        NumericFieldSpec("amount"),
        TimestampFieldSpec("ts"),
        ObjectSpec(
          "nested",
          List(
            MonotonicIDFieldSpec("id"),
            StringFieldSpec("name")
          )
        )
      )
    )
  )

  import io.circe.syntax._
  import com.evolution.datagen.model.DataModel.Protocol._

  import cats.syntax.parallel._

//  import cats.implicits._
//

  def process: IO[Unit] = {
    for {
      clock_start <- timer.clock.monotonic(TimeUnit.MILLISECONDS)
      _           <- IO(println(s"start is ${clock_start}"))
      semaphore   <- Semaphore[IO](10000)
      rateLimitedFunction = Utils.rateLimited(semaphore, () => IO.delay(dataGen.sample.asInstanceOf[ObjectField].asJson))
      allResults  <- Range(0, 10000).toList.parTraverse(_ => rateLimitedFunction)
      _           <- IO.delay(println(s"Size is ${allResults.size}"))
      clock_end   <- timer.clock.monotonic(TimeUnit.MILLISECONDS)
      _           <- IO(println(s"end is ${clock_start}"))
      _           <- IO(println(s"duration is ${clock_end - clock_start}"))
    } yield IO.unit

  }

  override def run(args: List[String]): IO[ExitCode] = {
    process.as(ExitCode.Success)
  }
}
