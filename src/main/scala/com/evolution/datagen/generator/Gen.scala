package com.evolution.datagen.generator

import cats.effect.IO
import cats.effect.concurrent.Ref

import java.time.Instant
import java.util.UUID
import com.evolution.datagen.model.DataModel._
import com.evolution.datagen.model.SpecModel.{DataSpec, GeneratorSpec}
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec._
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec

import scala.util.Random

object Gen {

  def from(dataSpec: DataSpec): Field = {
    dataSpec match {
      case ObjectSpec(name, fields) => ObjectField(name, fields.map(Gen.from))
      case spec: DataSpec.FieldSpec => fromField(spec)
    }
  }

  private[generator] def fromField(fieldSpec: DataSpec.FieldSpec): Field = {
    fieldSpec match {
      case UUIDFieldSpec(name) => StringField(name, UUID.randomUUID().toString)
      case StringFieldSpec(name, minSize, maxSize) => StringField(name, alphanumeric(minSize.get, maxSize.get))
      case EnumFieldSpec(name, values) => StringField(name, values(Random.nextInt(values.size)))
      //TODO: Reimplement (prev implementation was based on Atomic Ref an was part of sampler object.
      case MonotonicIDFieldSpec(name, from) => NumericField(name, 1)
      case BoolFieldSpec(name) => BoolField(name, Random.nextBoolean())
      case NumericFieldSpec(name, from, to) => NumericField(name, Random.between(from.get, to.get))
      case TimestampFieldSpec(name, from) => NumericField(name, Random.between(from.get, Instant.now.toEpochMilli))
    }
  }

  private def alphanumeric(from: Long, to: Long) = Random.alphanumeric.take(Random.between(from, to).toInt).mkString

}
