package com.thenewmotion.ocpi
package registration

import msgs.OcpiStatusCode.GenericSuccess
import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import akka.stream.ActorMaterializer
import msgs.Ownership.Ours
import msgs.{GlobalPartyId, SuccessWithDataResp}

class RegistrationRoute(service: RegistrationService)(implicit mat: ActorMaterializer) extends JsonApi {
  import msgs.v2_1.OcpiJsonProtocol._
  import msgs.v2_1.Credentials._

  def route(accessedVersion: Version, user: GlobalPartyId)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds[Ours]]) { credsToConnectToThem =>
        complete {
          service
            .reactToPostCredsRequest(accessedVersion, user, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    } ~
    get {
      complete {
        service
          .credsToConnectToUs(user)
          .map(x => SuccessWithDataResp(GenericSuccess, data = x))
      }
    } ~
    put {
      entity(as[Creds[Ours]]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(accessedVersion, user, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}