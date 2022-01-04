package com.evolution.datagen.model

import cats.syntax.option._
import com.evolution.datagen.model.SpecModel.DataSpec.FieldSpec._
import com.evolution.datagen.model.SpecModel.DataSpec.ObjectSpec
import com.evolution.datagen.model.SpecModel.{DataSpec, GeneratorSpec}
import io.circe.Error
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeneratorSpecTest extends AnyFlatSpec with Matchers {

  "fromYaml" should "parse yaml when correct structure" in {

    val yaml =
      """
          |definitions:
          |    - name: event
          |      fields:
          |        - name: "id"
          |          type: uuid  # or could be monotonic
          |""".stripMargin
    val configurationEither = GeneratorSpec.fromYaml(yaml)

    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        UUIDFieldSpec("id")
      )
    }
  }
  "fromYaml" should "parse yaml definitions with nested object type" in {
    val yaml =
      """
        |definitions:
        |    - name: event
        |      fields:
        |        - name: "nested_object"
        |          fields:
        |             - name: id 
        |               type: monotonicid
        |             - name: uuid 
        |               type: uuid
        |             - name: name 
        |               type: string 
        |             - name: is_new 
        |               type: bool 
        |             - name: amount 
        |               type: numeric 
        |             - name: ts 
        |               type: timestamp 
        |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        ObjectSpec(
          "nested_object",
          List(
            MonotonicIDFieldSpec("id"),
            UUIDFieldSpec("uuid"),
            StringFieldSpec("name"),
            BoolFieldSpec("is_new"),
            NumericFieldSpec("amount"),
            TimestampFieldSpec("ts")
          )
        )
      )
    }
  }

  "fromYaml" should "parse yaml definitions with monotonicID type" in {
    val yaml =
      """
          |definitions:
          |    - name: event
          |      fields:
          |        - name: "id_from_1000"
          |          type: monotonicid
          |          from: 1000
          |        - name: "id"
          |          type: monotonicid
          |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        MonotonicIDFieldSpec("id"),
        MonotonicIDFieldSpec("id_from_1000", 1000L.some)
      )
    }
  }
  "fromYaml" should "parse yaml definitions with string ype" in {
    val yaml =
      """
        |definitions:
        |    - name: event
        |      fields:
        |        - name: "str"
        |          type: string
        |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(StringFieldSpec("str"))
    }
  }

  "fromYaml" should "parse yaml definitions with bool ype" in {
    val yaml =
      """
          |definitions:
          |    - name: event
          |      fields:
          |        - name: "is_deleted"
          |          type: bool 
          |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(BoolFieldSpec("is_deleted"))
    }
  }

  "fromYaml" should "parse yaml definitions with numeric type" in {
    val yaml =
      """
        |definitions:
        |    - name: event
        |      fields:
        |        - name: "random"
        |          type: numeric 
        |        - name: "random"
        |          type: numeric 
        |          from: 1000
        |          to: 10000
        |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        NumericFieldSpec("random"),
        NumericFieldSpec("random", 1000L.some, 10000L.some)
      )
    }
  }

  "fromYaml" should "parse yaml definitions with timestamp type" in {
    val yaml =
      """
          |definitions:
          |    - name: event
          |      fields:
          |        - name: "ts"
          |          type: timestamp 
          |        - name: "ts_from"
          |          type: timestamp 
          |          from: 1000
          |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        TimestampFieldSpec("ts"),
        TimestampFieldSpec("ts_from", 1000L.some)
      )
    }
  }

  "fromYaml" should "parse yaml definitions with enum type" in {
    val yaml =
      """
        |definitions:
        |    - name: event
        |      fields:
        |        - name: "platform"
        |          type: enum 
        |          values: [ "IOS", "Android" ] 
        |""".stripMargin

    val configurationEither = GeneratorSpec.fromYaml(yaml)
    getFieldSpec(configurationEither, "event") { fields =>
      fields should contain theSameElementsAs List(
        EnumFieldSpec(
          "platform",
          List(
            "IOS",
            "Android"
          )
        )
      )
    }
  }

  private def getFieldSpec(genSpec: GeneratorSpec, definition: String)(check: (List[DataSpec]) => Unit): Unit = {

    val fields = genSpec.definitions.find(_.name == definition).map(_.fields)
    fields.size should be > 0
    check(fields.getOrElse(List.empty))
  }
}
