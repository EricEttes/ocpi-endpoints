# OCPI endpoints [![Build Status](https://travis-ci.org/NewMotion/ocpi-endpoints.png?branch=master)](https://travis-ci.org/NewMotion/ocpi-endpoints)


The New Motion implementation of common endpoints of the OCPI protocol.

See http://github.com/ocpi/ocpi

# Notions
To make clear when the parameters are about the application making use of the application or the party the application is
connecting to they are referred as: `our`/`us` or `theirs`/`them`

# Dependencies
To find out if there is any dependency to be updated you can run `sbt dependencyUpdates` to see what could be updated.
See https://github.com/rtimush/sbt-updates

# Usage

See the example app in the example folder.  You can run it with sbt by typing `project example` followed by `run`. 
Once running you can make version requests with

```
curl -X GET -H "Authorization: Token abc" "http://localhost:8080/example/versions"
```

# Serialization support

You can use either Circe (recommended) or Spray-Json for (de)serialization of JSON

## Circe

In build.sbt

```
libraryDependencies += "com.thenewmotion.ocpi" %% s"ocpi-msgs-circe" % "0.7.6"
```

Then in the route or client you want to use

```
import com.thenewmotion.ocpi.msgs.circe.v2_1.protocol._
```

## Spray Json

In build.sbt

```
libraryDependencies += "com.thenewmotion.ocpi" %% s"ocpi-msgs-spray-json" % "0.7.6"
```

Then in the route or client you want to use

```
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._
```

# Changelog

## 0.7.10

* Pass the requester's globalPartyId to MspTokenService methods
* Stop passing the ExecutionContext from Routes into Services in some cases

## 0.7.9

* Require non empty ids
* Improved Generic merging or patches using Shapeless 
* Validation for Latitude and Longitude

## 0.7.8

* Tokens Client for CPO Tokens module
* Don't include akka-http-spray-json anymore, as spray json is optional

## 0.7.7 

* Many of the modules have a method such as createOrUpdate*, this method used to take both an id from the url
and an object.Now, they only take an object, and the validation that the id of the url and and the id from
the object are validated by the library
* Remove the id fields from the Patch object, as the id cannot be changed  

## 0.7.6

* Added Sessions module
* When using a route or a client, you will need to import the Circe or Spray Json modules, as above
* You no longer need to use `new` to create a route, each one has a companion object, and then the route can be
used directly instead of having to use the `.route` method.  See the ExampleApp.
* Many of the services previously had methods returning `Future[Either[SessionError, Boolean]]` where
a `Right(true)` meant the record has been updated and a `Right(false)` meant it was updated.  This has
been replaced by `Right(Created)` and `Right(Updated)` 
