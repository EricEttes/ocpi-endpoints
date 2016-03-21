package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{SuccessResponse, Url}
import com.thenewmotion.ocpi.msgs.{Enumerable, Nameable}
import org.joda.time.DateTime

object Versions {


  case class Version(
    version: String,
    url:  Url
    )

  case class Endpoint(
    identifier: EndpointIdentifier,
    url: Url
    )

  case class VersionsRequest(
    token: String,
    url: Url
  )

  case class VersionDetails(
    version: String,
    endpoints: List[Endpoint]
    )

  case class VersionDetailsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime = DateTime.now(),
    data: VersionDetails
    ) extends SuccessResponse {
    require(data.endpoints.exists(_.identifier == EndpointIdentifier.Credentials), "Missing credentials endpoint type details")
  }


  case class VersionsResp(
    status_code: Int,
    status_message: Option[String],
    timestamp: DateTime = DateTime.now(),
    data: List[Version]
    ) extends SuccessResponse

  sealed trait EndpointIdentifier extends Nameable
  object EndpointIdentifier extends Enumerable[EndpointIdentifier] {
    case object Locations extends EndpointIdentifier {val name = "locations"}
    case object Credentials extends EndpointIdentifier {val name = "credentials"}
    case object Versions extends EndpointIdentifier {val name = "versions"}
    case object Tariffs extends EndpointIdentifier {val name = "tariffs"}
    case object Tokens extends EndpointIdentifier {val name = "tokens"}
    case object Cdrs extends EndpointIdentifier {val name = "cdrs"}
    case object Sessions extends EndpointIdentifier {val name = "sessions"}

    val values = List(Locations, Credentials, Versions, Tariffs, Tokens, Cdrs, Sessions)
  }
}

