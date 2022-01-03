package com.evolution.datagen.model

import io.circe.{Encoder, Json}

object DataModel {

  sealed trait Field {
    def name: String
  }
  final case class StringField(name: String, value: String) extends Field
  final case class NumericField(name: String, value: Long) extends Field
  final case class BoolField(name: String, value: Boolean) extends Field
  final case class ObjectField(name: String, fields: List[Field]) extends Field

  object Protocol {

    import io.circe.generic.extras._

    implicit val config: Configuration = Configuration.default

    implicit val encodeFoo: Encoder[ObjectField] = new Encoder[ObjectField] {
      final def apply(a: ObjectField): Json =
        Json.obj(a.fields.map {
          case StringField(name, value) => (name, Json.fromString(value))
          case NumericField(name, value) => (name, Json.fromLong(value))
          case BoolField(name, value) => (name, Json.fromBoolean(value))
          case obj @ ObjectField(name, _) => (name, apply(obj))
        }: _*)
    }
  }
}
