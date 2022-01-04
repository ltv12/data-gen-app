package com.evolution.datagen.model

import java.time.Instant

import cats.syntax.functor._
import cats.syntax.option._
import com.evolution.datagen.generator.Gen
import com.evolution.datagen.model.DataModel.ObjectField
import com.evolution.datagen.model.SpecModel.DataSpec.{FieldSpec, ObjectSpec}
import io.circe.parser._
import io.circe.syntax._
import io.circe.yaml.parser
import io.circe.{Codec, Decoder, Encoder}

import scala.io.Source

object SpecModel {
  final case class GeneratorSpec(definitions: List[ObjectSpec])

  object GeneratorSpec {

    def load(yamlPath: String): ObjectSpec = {

      def readConfig =
        GeneratorSpec.fromYaml(
          Source.fromResource(yamlPath).getLines().mkString(System.lineSeparator)
        )

      readConfig.definitions.head
    }

    def fromYaml(yaml: String): GeneratorSpec = {
      import Protocol._
      parser.parse(yaml).flatMap(json => decode[GeneratorSpec](json.noSpaces)) match {
        case Left(e) => throw new RuntimeException("Failed to read config", e)
        case Right(spec) => spec
      }
    }
  }

  sealed trait DataSpec {
    val name: String
  }

  object DataSpec {
    case class ObjectSpec(name: String, fields: List[DataSpec]) extends DataSpec
    sealed trait FieldSpec extends DataSpec
    object FieldSpec {
      case class UUIDFieldSpec(name: String) extends FieldSpec
      case class StringFieldSpec(name: String, minSize: Option[Int] = 0.some, maxSize: Option[Int] = 50.some) extends FieldSpec
      case class EnumFieldSpec(name: String, values: List[String]) extends FieldSpec
      case class MonotonicIDFieldSpec(name: String, from: Option[Long] = 0L.some) extends FieldSpec
      case class BoolFieldSpec(name: String) extends FieldSpec
      case class NumericFieldSpec(name: String, from: Option[Long] = 0L.some, to: Option[Long] = Long.MaxValue.some) extends FieldSpec
      case class TimestampFieldSpec(name: String, from: Option[Long] = Some(Instant.EPOCH.toEpochMilli)) extends FieldSpec
    }

    implicit class ObjectSpecOps(dataSpec: DataSpec) {
      def toSample: ObjectField = Gen.toSample(dataSpec).asInstanceOf[ObjectField]
      def toJsonSample: String = toSample.asJson.noSpaces
    }

  }

  object Protocol {

    import io.circe.generic.extras._
    import io.circe.generic.extras.semiauto._

    implicit val config: Configuration = Configuration.default
      .withDiscriminator("type")
      .withDefaults
      .copy(transformConstructorNames = _.toLowerCase.dropRight("FieldSpec".length))

    implicit val fieldSpecDecoder: Decoder[FieldSpec] =
      deriveConfiguredDecoder[FieldSpec]
    implicit val fieldSpecEncoder: Encoder[FieldSpec] =
      deriveConfiguredEncoder[FieldSpec]

    implicit val objectSpecDecoder: Decoder[ObjectSpec] =
      deriveConfiguredDecoder[ObjectSpec]
    implicit val objectSpecEncoder: Encoder[ObjectSpec] =
      deriveConfiguredEncoder[ObjectSpec]

    implicit val dataUnitSpecSpecDecoder: Decoder[DataSpec] =
      List[Decoder[DataSpec]](objectSpecDecoder.widen, fieldSpecDecoder.widen)
        .reduceLeft(_ or _)
    implicit val dataUnitSpecSpecEncoder: Encoder[DataSpec] =
      Encoder.instance {
        case objectSpec: ObjectSpec => objectSpec.asJson
        case fieldSpec: FieldSpec => fieldSpec.asJson
      }

    implicit val generatorSpecEncoder: Codec[GeneratorSpec] = deriveConfiguredCodec[GeneratorSpec]
  }

}
