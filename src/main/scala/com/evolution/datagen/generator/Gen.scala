package com.evolution.datagen.generator

import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import com.evolution.datagen.model.DataModel._
import com.evolution.datagen.model.SpecModel.DataSpec
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec._
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec

import scala.util.Random

object Gen {

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
