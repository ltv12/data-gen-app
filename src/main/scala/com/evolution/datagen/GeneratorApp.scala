package com.evolution.datagen

import java.util.concurrent.TimeUnit
import cats.effect.concurrent.Semaphore
import cats.effect.{ExitCode, IO, IOApp}
import com.evolution.datagen.generator.{Gen, RateLimiter}
import com.evolution.datagen.model.DataModel.ObjectField
import com.evolution.datagen.model.SpecModel.GeneratorSpec

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant
import java.time.format.DateTimeFormatter

object GeneratorApp extends IOApp {

  import cats.syntax.parallel._
  import com.evolution.datagen.model.DataModel.Protocol._
  import io.circe.syntax._

  def process: IO[Unit] = {
    val eventsNumber = 10000
    (for {
      dataSpec          <- GeneratorSpec.of("data_spec_example.yaml")
      clockStart        <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      semaphore         <- Semaphore[IO](eventsNumber)
      rateLimitedFunction = RateLimiter.of(semaphore, () => IO.delay(Gen.from(dataSpec).asInstanceOf[ObjectField].asJson.noSpaces))
      allResults        <- Range(0, eventsNumber).toList.parTraverse(_ => rateLimitedFunction)
      clockGenEnd       <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _                 <- IO(println(s"sample of data ${allResults.take(1).mkString(System.lineSeparator)}."))
      _                 <- IO(println(s"generation of ${allResults.size} taken ${clockGenEnd - clockStart} milliseconds"))
      clockWriteFileEnd <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      eventsAsFile      <- IO(allResults.mkString(System.lineSeparator).getBytes(StandardCharsets.UTF_8))
      fileName = s"target/tmp/${Instant.ofEpochMilli(clockStart)}-events.json"
      _                 <- IO(println(s"writing of $fileName has taken ${clockWriteFileEnd - clockGenEnd} milliseconds"))
      _                 <- IO(Files.write(Paths.get(fileName), eventsAsFile))
    } yield ()) *> IO.suspend(process)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    process.as(ExitCode.Success)
  }
}
