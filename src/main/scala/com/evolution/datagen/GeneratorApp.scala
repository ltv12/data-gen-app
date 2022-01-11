package com.evolution.datagen

import java.util.concurrent.TimeUnit

import cats.effect.concurrent.Semaphore
import cats.effect.{ExitCode, IO, IOApp}
import com.evolution.datagen.generator.{RateLimiter}
import com.evolution.datagen.model.SpecModel.{DataSpec, GeneratorSpec}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant

import com.typesafe.config.{Config, ConfigFactory}

object GeneratorApp extends IOApp {

  import cats.syntax.parallel._
  def generateData(spec: DataSpec)(implicit appConfig: Config): IO[List[String]] = {
    for {
      clockStart  <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      semaphore   <- Semaphore[IO](appConfig.getInt("rate.events.perSecond"))
      rateLimitedFunction = RateLimiter.use[IO, String](semaphore, () => spec.toJsonSample)
      sample      <- (1 to (appConfig.getInt("rate.events.toGenerate"))).toList.parTraverse(_ => rateLimitedFunction)
      clockGenEnd <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _           <- IO(println(s"Generation of ${sample.size} taken ${clockGenEnd - clockStart} milliseconds"))
    } yield sample
  }

  def toFile(data: List[String])(implicit appConfig: Config): IO[Unit] =
    for {
      clockStart    <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      eventsAsBytes <- IO(data.mkString(System.lineSeparator).getBytes(StandardCharsets.UTF_8))
      _             <- IO(println(s"Average size of event is ${eventsAsBytes.size.toDouble / data.size} bytes"))
      fileName = s"${appConfig.getString("storage.events.path")}/${Instant.ofEpochMilli(clockStart)}-events.json"
      _             <- IO(Files.write(Paths.get(fileName), eventsAsBytes))
      clockEnd      <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _             <- IO(println(s"Writing of $fileName has taken ${clockEnd - clockStart} milliseconds"))
    } yield ()

  implicit val appConfig: Config = ConfigFactory.load()

  def process(dataSpec: DataSpec): IO[Nothing] = {
    (for {
      sample <- generateData(dataSpec)
      _      <- toFile(sample)
    } yield ()) *> IO.suspend(process(dataSpec))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    process(GeneratorSpec.load("data_spec_example.yaml")).as(ExitCode.Success)
  }
}
