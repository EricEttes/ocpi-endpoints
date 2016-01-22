package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{ImageCategory, Image, BusinessDetails}
import Credentials._
import OcpiJsonProtocol._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class CredentialsSpecs extends SpecificationWithJUnit {


  "CredentialsResp" should {
    "deserialize" in new CredentialsTestScope {
      credentialsJson1.convertTo[Creds] mustEqual credentials1
    }
    "serialize" in new CredentialsTestScope {
      credentials1.toJson.toString mustEqual credentialsJson1.compactPrint
    }
  }


  private trait CredentialsTestScope extends Scope {

    val credentials1 = Creds(
      token = "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
      url = "https://example.com/ocpi/cpo/",
      business_details = BusinessDetails(
        "Example Operator",
        Some(Image("https://example.com/img/logo.jpg", ImageCategory.Operator, "jpeg")),
        Some("http://example.com")
      )
    )


    val credentialsJson1 =
      s"""
         |{
         |    "token": "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
         |    "url": "https://example.com/ocpi/cpo/",
         |    "business_details": {
         |        "name": "Example Operator",
         |        "logo": {
         |            "url": "https://example.com/img/logo.jpg",
         |            "category": "OPERATOR",
         |            "type": "jpeg"
         |        },
         |        "website": "http://example.com"
         |    }
         |}
     """.stripMargin.parseJson





  }
}
