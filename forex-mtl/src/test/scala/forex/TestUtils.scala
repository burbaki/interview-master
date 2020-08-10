package forex

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.{Request, Response}
import org.scalatest.matchers.should.Matchers

object TestUtils extends Matchers {

  def client(response: Response[IO], expectedRequest: Option[Request[IO]]): Client[IO] = Client.apply(
    req => {
      expectedRequest.isDefined shouldBe true
      val expectedReq = expectedRequest.get
      req.uri shouldBe expectedReq.uri
      req.method shouldBe expectedReq.method
      req.headers shouldBe expectedReq.headers
      Resource.apply[IO, Response[IO]](IO(response -> IO(())))
    }
  )
}
