package forex

import java.time.OffsetDateTime

import forex.domain.{Currency, Price}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser._

object Playground extends App {
  val x = OffsetDateTime.parse("2020-08-09T06:40:29.409Z")
  println(x)

//  implicit val config: Configuration = Configuration.default.copy(
//    transformMemberNames = {
//      case "timestamp" => "time_stamp"
//      case other        => other
//    }
//  )
  implicit val responseDecoder: Decoder[OneFrameResponse] =
    deriveDecoder[OneFrameResponse]

  val json =
    """
      |[
      |    {
      |        "from": "USD",
      |        "to": "EUR",
      |        "bid": 0.6118225421857174,
      |        "ask": 0.8243869101616611,
      |        "price": 0.71810472617368925,
      |        "time_stamp": "2020-08-09T06:40:29.409Z"
      |    }
      |]
      |""".stripMargin

  val parsed = parse(json).right.get
  println(parsed)

  val value = parsed.as[List[OneFrameResponse]]

  println(value)

  case class OneFrameResponse(
      from: Currency,
      to: Currency,
      bid: BigDecimal,
      ask: BigDecimal,
      price: Price,
      timestamp: OffsetDateTime
  )

  import io.circe._
  import io.circe.parser._

  val json1: String = """
{
"id": "c730433b-082c-4984-9d66-855c243266f0",
"name": "Foo",
"counts": [1, 2, 3],
"values": {
"bar": true,
"baz": 100.001,
"qux": ["a", "b"]
}
} """

  val doc: Json       = parse(json1).getOrElse(Json.Null)
  val cursor: HCursor = doc.hcursor

  val nameResult =
    cursor
      .withFocus(_.mapObject { x =>
        val value = x("name")
        value.map(x.add("timestamp212", _)).getOrElse(x)
      })
      .top
      .get

  println(nameResult)
}
