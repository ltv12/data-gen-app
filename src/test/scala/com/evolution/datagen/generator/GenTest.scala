package com.evolution.datagen.generator

import java.time.Instant
import java.util.UUID

import com.evolution.datagen.model.DataModel.{BoolField, NumericField, ObjectField, StringField}
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec.{
  BoolFieldSpec,
  EnumFieldSpec,
  MonotonicIDFieldSpec,
  NumericFieldSpec,
  StringFieldSpec,
  TimestampFieldSpec,
  UUIDFieldSpec
}
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GenTest extends AnyFlatSpec with Matchers {

  "fromField#UUIDFieldSpec" should "generate pair with name and uuid value" in {

    val actualField = Gen.fromField(UUIDFieldSpec("id")).sample.asInstanceOf[StringField]

    actualField.name shouldBe "id"
    actualField.value shouldBe UUID.fromString(actualField.value).toString

  }

  "fromField#StringFieldSpec" should "generate pair with name and alhanumeric string value" in {

    val spec @ StringFieldSpec(expectedName, Some(min), Some(max)) = StringFieldSpec("name")
    val actualField = Gen.fromField(spec).sample.asInstanceOf[StringField]

    actualField.name shouldBe expectedName
    val beSizeWithinSpec = (be >= min and be <= max)
    actualField.value.length should beSizeWithinSpec
  }

  "fromField#EnumFieldSpec" should "generate pair with name and string from one of the provided values" in {

    val spec @ EnumFieldSpec(expectedName, values) = EnumFieldSpec("country", List("Russia", "Germany", "Italy"))
    val actualField = Gen.fromField(spec).sample.asInstanceOf[StringField]

    actualField.name shouldBe expectedName
    List(actualField.value) should contain atLeastOneElementOf values
  }

  "fromField#NumericFieldSpec" should "generate pair with name and string from one of the provided values" in {

    val spec @ NumericFieldSpec(expectedName, Some(from), Some(to)) = NumericFieldSpec("amount")
    val actualField = Gen.fromField(spec).sample.asInstanceOf[NumericField]

    actualField.name shouldBe expectedName
    actualField.value should between(from, to)
  }

  "fromField#TimestampFieldSpec" should "generate pair with name and long value as epoch that between from and now " in {

    val spec @ TimestampFieldSpec(expectedName, Some(from)) = TimestampFieldSpec("ts")
    val actualField = Gen.fromField(spec).sample.asInstanceOf[NumericField]

    actualField.name shouldBe expectedName
    actualField.value should between(from, Instant.now.toEpochMilli)
  }

  "fromField#BoolFieldSpec" should "generate pair with name and string from one of the provided values" in {

    val spec @ BoolFieldSpec(expectedName) = BoolFieldSpec("has_account")
    val actualField = Gen.fromField(spec).sample.asInstanceOf[BoolField]

    actualField.name shouldBe expectedName
  }

  "fromField#MonotonicIdFieldSpec" should "generate pair with name and long value started that increases each time" in {
    val spec @ MonotonicIDFieldSpec(expectedName, Some(startFrom)) = MonotonicIDFieldSpec("id")
    val gen = Gen.fromField(spec)
    val actualField = gen.sample.asInstanceOf[NumericField]

    actualField.name shouldBe expectedName
    actualField.value shouldBe startFrom + 1
    actualField.value should between(startFrom, Long.MaxValue)

    val actualFieldV2 = gen.sample.asInstanceOf[NumericField]

    actualFieldV2.name shouldBe expectedName
    actualFieldV2.value shouldBe startFrom + 2
    actualFieldV2.value should between(startFrom, Long.MaxValue)
  }

  "from#ObjectSpec" should "generate object for provided spec" in {
    val spec = ObjectSpec(
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

    val gen = Gen.from(spec)
    val sampleObject = gen.sample.asInstanceOf[ObjectField]

    sampleObject.fields.count(_.name == "nested") shouldBe 1
    sampleObject.fields.size shouldBe 7
  }
  private def between(from: Long, to: Long) = (be >= from and be <= to)
}
