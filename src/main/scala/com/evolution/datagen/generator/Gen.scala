package com.evolution.datagen.generator

import cats.effect.IO
import cats.effect.concurrent.Ref

import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import com.evolution.datagen.model.DataModel._
import com.evolution.datagen.model.SpecModel.{DataSpec, GeneratorSpec}
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec._
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec

import scala.io.Source
import scala.util.Random

object Gen {
  def of(yamlPath: String): IO[Gen.Samplable] = {

    def readConfig =
      GeneratorSpec.fromYaml(
        Source.fromResource(yamlPath).getLines().mkString(System.lineSeparator)
      )

    for {
      genSpec <- Ref.of[IO, GeneratorSpec](readConfig)
      dataGen <- genSpec.get.map(_.definitions.head).map(Gen.from)
    } yield dataGen
  }
  def from(objectSpec: ObjectSpec): Samplable = fromObject(objectSpec)

  private def fromObject(dataSpec: DataSpec): Samplable = {
    dataSpec match {
      case DataSpec.ObjectSpec(name, fields) =>
        new Samplable {
          val gen = fields.map(fromObject)
          override def sample: ObjectField = ObjectField(name, gen.map(_.sample))
        }
      case spec: DataSpec.FieldSpec => fromField(spec)
    }
  }

  private[generator] def fromField(fieldSpec: DataSpec.FieldSpec): Samplable = {
    fieldSpec match {
      case UUIDFieldSpec(name) =>
        new Samplable {
          override def sample: StringField = StringField(name, UUID.randomUUID().toString)
        }
      case StringFieldSpec(name, minSize, maxSize) =>
        new Samplable {
          override def sample: StringField = {
            StringField(name, Random.alphanumeric.take(Random.between(minSize.get, maxSize.get)).mkString)
          }
        }
      case EnumFieldSpec(name, values) =>
        new Samplable {
          override def sample: StringField = StringField(name, values(Random.nextInt(values.size)))
        }
      case MonotonicIDFieldSpec(name, from) =>
        new Samplable {
          val id = new AtomicLong(from.get)
          override def sample: NumericField = NumericField(name, id.incrementAndGet())
        }
      case BoolFieldSpec(name) =>
        new Samplable {
          override def sample: BoolField = BoolField(name, Random.nextBoolean())
        }
      case NumericFieldSpec(name, from, to) =>
        new Samplable {
          override def sample: NumericField = NumericField(name, Random.between(from.get, to.get))
        }
      case TimestampFieldSpec(name, from) =>
        new Samplable {
          override def sample: NumericField = NumericField(name, Random.between(from.get, Instant.now.toEpochMilli))
        }
    }
  }

  trait Samplable {
    def sample: Field
  }

}
