package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.credentials.CredentialsRoutes
import com.thenewmotion.ocpi.versions.VersionsRoutes
import com.typesafe.scalalogging.LazyLogging
import spray.routing._
import spray.routing.authentication.{Authentication, BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{ExecutionContext, Future}

abstract class OcpiRestActor extends HttpServiceActor with TopLevelRoutes {

  implicit private val rejectionHandler: RejectionHandler = OcpiRejectionHandler.Default

  override def receive: Receive =
    runRoute(allRoutes )
}

trait TopLevelRoutes extends HttpService with VersionsRoutes with CredentialsRoutes with CurrentTimeComponent{
  import scala.concurrent.ExecutionContext.Implicits.global
  val tldh: TopLevelRouteDataHanlder
  val adh: AuthDataHandler
  lazy val auth = new Authenticator(adh)
  val currentTime = new CurrentTime

  def allRoutes =
    headerValueByName("Authorization") { access_token =>
      authenticate(auth.validate(access_token)) { _ =>
        pathPrefix(tldh.namespace) {
          versionsRoute  ~
          path(Segment) { version =>
            versionDetailsRoute(version) ~ credentialsRoute(version, access_token)
          }
        }
      }
    }
}

class Authenticator(adh: AuthDataHandler)(implicit ec: ExecutionContext) {

  def validate(token: String): Future[Authentication[ApiUser]] = {
    Future {
      extractTokenValue(token) match {
        case Some(tokenVal) => adh.apiuser(tokenVal)
          .toRight(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        case None => Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
      }
    }
  }

  def extractTokenValue(token: String): Option[String] = {
    val authScheme = "Token "
    if (token.startsWith(authScheme)){
      val tokenVal = token.substring(authScheme.length)
      if (!tokenVal.isEmpty) Some(tokenVal) else None
    }
    else None
  }

}

trait CurrentTimeComponent {
  import org.joda.time.DateTime
  val currentTime: CurrentTime
  class CurrentTime {
    def instance = DateTime.now()
  }
}