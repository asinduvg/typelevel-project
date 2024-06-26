package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.data.OptionT

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import tsec.authentication.JWTAuthenticator

import concurrent.duration.DurationInt

import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import com.rockthejvm.jobsboard.domain.security.Authenticator
import com.rockthejvm.jobsboard.fixtures.UserFixture
import com.rockthejvm.jobsboard.config.{SecurityConfig}

import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UserFixture {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO] {

    override def find(email: String): IO[Option[User]] =
      if (email == danielEmail) IO.pure(Some(Daniel))
      else IO.pure(None)

    override def create(user: User): IO[String] =
      IO.pure(user.email)

    override def update(user: User): IO[Option[User]] =
      IO.pure(Some(user))

    override def delete(email: String): IO[Boolean] =
      IO.pure(true)
  }

  val mockedConfig = SecurityConfig("secret", 1.day)

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedConfig)
        maybeToken <- auth.login("user@rockthejvm.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedConfig)
        maybeToken <- auth.login(danielEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers)(mockedConfig)
        maybeToken <- auth.login(danielEmail, "rockthejvm")
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers)(mockedConfig)
        maybeUser <- auth.signup(
          NewUserInfo(
            danielEmail,
            "somePassword",
            Some("Daniel"),
            Some("Whatever"),
            Some("Other company")
          )
        )
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a completely new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers)(mockedConfig)
        maybeUser <- auth.signup(
          NewUserInfo(
            "bob@rockthejvm.com",
            "somePassword",
            Some("Bob"),
            Some("Jones"),
            Some("Company")
          )
        )
      } yield maybeUser

      program.asserting {
        case Some(user) =>
          user.email shouldBe "bob@rockthejvm.com"
          user.firstName shouldBe Some("Bob")
          user.lastName shouldBe Some("Jones")
          user.company shouldBe Some("Company")
          user.role shouldBe Role.RECRUITER
        case _ =>
          fail()
      }
    }

    "change password should return Right(None) if the user doesn't exist" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers)(mockedConfig)
        result <- auth.changePassword("alice@rockthejvm.com", NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "change password should correctly change password if all details are correct" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers)(mockedConfig)
        result <- auth.changePassword(danielEmail, NewPasswordInfo("rockthejvm", "scalarocks"))
        isNicePassword <- result match
          case Right(Some(user)) =>
            BCrypt
              .checkpwBool[IO](
                "scalarocks",
                PasswordHash[BCrypt](user.hashedPassword)
              )
          case _ =>
            IO.pure(false)

      } yield isNicePassword

      program.asserting(_ shouldBe true)
    }
  }
}
