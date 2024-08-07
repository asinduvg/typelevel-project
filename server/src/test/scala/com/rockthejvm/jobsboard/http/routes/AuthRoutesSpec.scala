package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.data.*
import cats.implicits.*

import io.circe.generic.auto.*

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Method, Status, Credentials, AuthScheme}
import org.http4s.implicits.*
import org.http4s.headers.Authorization
import org.http4s.circe.CirceEntityCodec.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax

import tsec.mac.jca.HMACSHA256
import tsec.jws.mac.JWTMac
import tsec.authentication.{IdentityStore, JWTAuthenticator, SecuredRequestHandler}

import scala.concurrent.duration.DurationInt

import com.rockthejvm.jobsboard.fixtures.{UserFixture, SecuredRouteFixture}
import com.rockthejvm.jobsboard.core.Auth
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UserFixture
    with SecuredRouteFixture {

//////////////// prep ////////////////

  val mockedAuth: Auth[IO] = probedAuth(None)

  def probedAuth(userMap: Option[Ref[IO, Map[String, String]]]): Auth[IO] = new Auth[IO] {

    override def login(email: String, password: String): IO[Option[User]] =
      if (email == danielEmail && password == danielPassword)
        IO(Some(Daniel))
      else IO.pure(None)

    override def signup(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == riccardoEmail)
        IO(Some(Riccardo))
      else IO.pure(None)

    override def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == danielEmail)
        if (newPasswordInfo.oldPassword == danielPassword)
          IO.pure(Right(Some(Daniel)))
        else
          IO.pure(Left("Invalid password"))
      else
        IO.pure(Right(None))

    override def delete(email: String): IO[Boolean] = IO.pure(true)

    override def sendPasswordRecoveryToken(email: String): IO[Unit] =
      userMap
        .traverse { userMapRef =>
          userMapRef.modify { userMap =>
            (userMap + (email -> "abc123"), ())
          }
        }
        .map(_ => ())

    override def recoverPasswordFromToken(
        email: String,
        token: String,
        newPassword: String
    ): IO[Boolean] =
      userMap
        .traverse { userMapRef =>
          userMapRef.get
            .map { userMap =>
              userMap.get(email).filter(_ == token) // Option[String]
            }                                       // IO[Option[String]]
            .map(_.nonEmpty)                        // IO[Boolean]
        }                                           // IO[Option[Boolean]]
        .map(_.getOrElse(false))
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  ////////////// tests ////////////
  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, "wrongpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - OK + a JWT if login is successful" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(danielEmail, danielPassword))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 - Bad Request if the user to create already exists" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserDaniel)
        )
      } yield {
        // assertions here
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 - Created if the user to creation succeeds" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserRiccardo)
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 - Ok if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if logging out without a valid JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    // change password - user doesn't exist => 404
    "should return a 404 - Not Found if changing password for a user that doesn't exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(riccardoPassword, "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.NotFound
      }
    }

    // change password - invalid old password => 403 - Forbidden
    "should return a 403 - Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Forbidden
      }
    }

    // change password - user JWT is invalid => 401 Unauthorized
    "should return a 401 - Unauthorized if changing password without a JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    // change password - happy path => 200 OK
    "should return a 200 - OK if changing password for a user with valid JWT and password" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if a non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(riccardoEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if a admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(danielEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/daniel@rockthejvm.com")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(danielPassword, "newpassword"))
        )
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 200 - Ok when resetting a password, and an email should be triggered" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map())
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/reset")
            .withEntity(ForgotPasswordInfo(danielEmail))
        )
        userMap <- userMapRef.get
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
        userMap should contain key danielEmail
      }
    }

    "should return a 200 - Ok when recovering a password for a correct user/token combination" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(danielEmail -> "abc123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(danielEmail, "abc123", "rockthejvm"))
        )
        userMap <- userMapRef.get
      } yield {
        // assertions here
        response.status shouldBe Status.Ok
      }
    }

    "should return a 403 - Forbidden when recovering a password for a user with an incorrect token" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(danielEmail -> "abc123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(danielEmail, "wrongToken", "rockthejvm"))
        )
        userMap <- userMapRef.get
      } yield {
        // assertions here
        response.status shouldBe Status.Forbidden
      }
    }
  }
}
