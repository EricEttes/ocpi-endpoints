package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi
import com.thenewmotion.ocpi._
import Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.{EndpointIdentifier, VersionDetailsResp}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import spray.http.Uri
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

abstract class HandshakeService(implicit system: ActorRefFactory) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  def client: HandshakeClient = new HandshakeClient

  def reactToHandshakeRequest(version: String, existingTokenToConnectToUs: String, credsToConnectToThem: Creds, ourVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"Handshake initiated: token for party to connect to us is $existingTokenToConnectToUs, " +
      s"chosen version: $version.\nCredentials for us: $credsToConnectToThem")
    val result = for {
      res <- getTheirDetails(version, credsToConnectToThem.token, Uri(credsToConnectToThem.url))
    } yield res
    result.map {
      case -\/(error) => -\/(error)
      case \/-(verDetails) =>
        verDetails.data.endpoints.map(ep =>
          persistTheirEndpoint(version, existingTokenToConnectToUs, credsToConnectToThem.token, ep.identifier.name, ep.url))
        val newTokenToConnectToUs = ApiTokenGenerator.generateToken
        logger.debug(s"issuing new token for party '${credsToConnectToThem.business_details.name}'")
        persistTheirPrefs(version, existingTokenToConnectToUs, credsToConnectToThem)
        persistNewTokenToConnectToUs(existingTokenToConnectToUs, newTokenToConnectToUs)
        \/-(generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl))
    }
  }

  def initiateHandshakeProcess(tokenToConnectToThem: String, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    logger.info(s"initiate handshake process with: $theirVersionsUrl, $tokenToConnectToThem")
    val newTokenToConnectToUs = ApiTokenGenerator.generateToken
    logger.debug(s"issuing new token for party with initial authorization token: '$tokenToConnectToThem'")

    ourVersionsUrl match {
      case -\/(error) => Future.successful(-\/(error))
      case \/-(ourVersUrl) =>

        (for {
          theirVerDet <- result(getTheirDetails(ocpi.ourVersion, tokenToConnectToThem, theirVersionsUrl))
          theirCredEndpoint = theirVerDet.data.endpoints.filter(_.identifier == EndpointIdentifier.Credentials).head
          newCredToConnectToThem <- result(client.sendCredentials(theirCredEndpoint.url, tokenToConnectToThem,
            generateCredsToConnectToUs(newTokenToConnectToUs, ourVersUrl)))
        } yield newCredToConnectToThem).run


        (for {
          theirVerDet <- result(getTheirDetails(ocpi.ourVersion, tokenToConnectToThem, theirVersionsUrl))
          theirCredEndpoint = theirVerDet.data.endpoints.filter(_.identifier == EndpointIdentifier.Credentials).head
          newCredToConnectToThem <- result(client.sendCredentials(theirCredEndpoint.url, tokenToConnectToThem,
            generateCredsToConnectToUs(newTokenToConnectToUs, ourVersUrl)))
          //TODO: Something like this, I think inside the for comprehension
          // persist their credentials
          // persist their endpoints (theirVerDet)
        } yield newCredToConnectToThem).run
    }
  }

  /** Get versions, choose the one that match with the 'version' parameter, request the details of this version,
  * and return them if no error happened, otherwise return the error. It doesn't store them cause could be the party
  * is not still registered
  */
  // FIXME: I need to split this method in two, cause I want to use it in the initiation but without storing (since I can't do it yet)
  private[ocpi] def getTheirDetails(version: String, tokenToConnectToThem: String, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {

    def findCommonVersion(versionResp: Versions.VersionsResp): Future[HandshakeError \/ Versions.Version] = {
      versionResp.data.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None => Future.successful(-\/(SelectedVersionNotHosted))
      }
    }

    (for {
      theirVers <- result(client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem))
      ver <- result(findCommonVersion(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(ver.url, tokenToConnectToThem))
    } yield theirVerDetails).run
  }


  private[ocpi] def generateCredsToConnectToUs(tokenToConnectToUs: String, ourVersionsUrl: Uri): Creds = {
    import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails

    Creds(tokenToConnectToUs, ourVersionsUrl.toString(), BusinessDetails(ourPartyName, ourLogo, ourWebsite))
  }

  def persistTheirPrefs(version: String, tokenToConnectToUs: String, credsToConnectToThem: Creds): HandshakeError \/ Unit

  def persistNewTokenToConnectToUs(oldToken: String, newToken: String): HandshakeError \/ Unit

  def persistTheirEndpoint(version: String, existingTokenToConnectToUs: String, tokenToConnectToThem: String, endpName: String, url: Url): HandshakeError \/ Unit

  def ourPartyName: String

  def ourLogo: Option[Url]

  def ourWebsite: Option[Url]

  def ourVersionsUrl: HandshakeError \/ Uri
}

object ApiTokenGenerator {

  import java.security.SecureRandom

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken: String =
    generateToken(TOKEN_LENGTH)

  def generateToken(tokenLength: Int): String =
    if (tokenLength == 0) ""
    else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
      generateToken(tokenLength - 1)

}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}