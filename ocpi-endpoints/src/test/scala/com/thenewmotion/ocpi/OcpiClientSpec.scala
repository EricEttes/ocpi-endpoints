package com.thenewmotion.ocpi

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.Page
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http._, HttpHeaders._, StatusCodes._, ContentTypes._
import scala.language.reflectiveCalls
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

class OcpiClientSpec extends Specification with FutureMatchers {

  "generic client" should {

    "download all paginated data" in new TestScope {

      import GenericRespTypes._
      override val firstPage = HttpResponse(
        OK, HttpEntity(`application/json`, firstPageBody.getBytes),
        List(
          Link(Uri(s"$dataUrl?offset=1&limit=1"), Link.next),
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      override val lastPage = HttpResponse(
        OK, HttpEntity(`application/json`, lastPageBody.getBytes),
        List(
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      client.getData(dataUrl, "auth") must beLike[\/[GenericError, Iterable[TestData]]]{
        case \/-(r) =>
          r.size === 2
          r.head.id === "DATA1"
          r.tail.head.id === "DATA2"
      }.await
    }

    "return errors for wrong urls" in new TestScope {
      import GenericRespTypes._
      client.getData("http://localhost", "auth") must beLike[\/[GenericError, Iterable[TestData]]] {
        case -\/(r) =>
          r mustEqual GenericErrorInstance
      }.await
    }
  }

  object GenericRespTypes {
    case class TestData(id: String)
    sealed trait GenericError
    case object GenericErrorInstance extends GenericError

    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    implicit val testDataFormat = jsonFormat1(TestData)
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    val dataUrl = "http://localhost:8095/cpo/versions/2.0/somemodule"

    val firstPageBody = s"""
                       |{
                       |  "status_code": 1000,
                       |  "timestamp": "2010-01-01T00:00:00Z",
                       |  "data": [{
                       |    "id": "DATA1"
                       |  }]
                       |}
                       |""".stripMargin

    val lastPageBody = firstPageBody.replace("DATA1","DATA2")

    def firstPage: HttpResponse = HttpResponse(
      OK, HttpEntity(`application/json`,
      firstPageBody.getBytes), List(
        Link(Uri(s"$dataUrl?offset=1&limit=1"), Link.next),
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1")
      )
    )
    def lastPage: HttpResponse = HttpResponse(
      OK, HttpEntity(`application/json`, lastPageBody.getBytes),
      List(
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1"))
    )




    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    lazy val client = new OcpiClient {

      import scala.concurrent.ExecutionContext.Implicits.global
      import spray.client.pipelining._
      import spray.httpx.SprayJsonSupport._
      import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

      import GenericRespTypes._


      val urlPattern = s"$dataUrl\\?offset=([0-9]+)&limit=[0-9]+".r

      override def sendAndReceive = (req:HttpRequest) => req.uri.toString match {
        case urlPattern(offset) if offset == "0" => Future.successful(firstPage)
        case urlPattern(offset) if offset == "1" => Future.successful(lastPage)
        case urlPattern(offset) => println(s"got offset $offset. "); throw new RuntimeException()
        case x => println(s"got request url |$x|. "); throw new RuntimeException()
      }

      def getData(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[GenericError \/ Iterable[TestData]] =
        traversePaginatedResource(uri, auth, GenericErrorInstance)(unmarshal[Page[TestData]])
    }

  }
}
