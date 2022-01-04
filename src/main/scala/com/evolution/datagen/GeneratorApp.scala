package com.evolution.datagen

import java.util.concurrent.TimeUnit
import cats.effect.concurrent.Semaphore
import cats.effect.{ExitCode, IO, IOApp}
import com.evolution.datagen.generator.{Gen, RateLimiter}
import com.evolution.datagen.model.DataModel.ObjectField
import com.evolution.datagen.model.SpecModel.{DataSpec, GeneratorSpec}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant

object GeneratorApp extends IOApp {

  import cats.syntax.parallel._
  import com.evolution.datagen.model.DataModel.Protocol._
  import io.circe.syntax._

  def generateData(spec: DataSpec, numberToGenerate: Int, numberPerSecond: Int): IO[List[String]] = {
    for {
      clockStart  <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      semaphore   <- Semaphore[IO](numberPerSecond)
      rateLimitedFunction = RateLimiter.of(semaphore, () => IO.delay(Gen.toSample(spec).asInstanceOf[ObjectField].asJson.noSpaces))
      sample      <- (1 to (numberToGenerate)).toList.parTraverse(_ => rateLimitedFunction)
      clockGenEnd <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _           <- IO(println(s"generation of ${sample.size} taken ${clockGenEnd - clockStart} milliseconds"))
    } yield sample
  }

  def toFile(data: List[String]): IO[Unit] =
    for {
      clockStart     <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      eventsAsString <- IO(data.mkString(System.lineSeparator).getBytes(StandardCharsets.UTF_8))
      fileName = s"target/tmp/${Instant.ofEpochMilli(clockStart)}-events.json"
      _              <- IO(Files.write(Paths.get(fileName), eventsAsString))
      clockEnd       <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _              <- IO(println(s"writing of $fileName has taken ${clockEnd - clockStart} milliseconds"))
    } yield ()

  def process(dataSpecIO: IO[DataSpec]): IO[Nothing] = {
    val numberToGenerate = 1500
    val numberPerSecond = 1000
    (for {
      dataSpec <- dataSpecIO
      sample   <- generateData(dataSpec, numberToGenerate, numberPerSecond)
      _        <- toFile(sample)
    } yield dataSpec).flatMap(spec => process(IO(spec)))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    process(GeneratorSpec.of("data_spec_example.yaml")).as(ExitCode.Success)
  }
}
