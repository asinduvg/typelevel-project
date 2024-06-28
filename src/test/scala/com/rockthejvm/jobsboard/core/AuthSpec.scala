package com.rockthejvm.jobsboard.core

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import com.rockthejvm.jobsboard.fixtures.UsersFixture
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import cats.effect.*
import com.rockthejvm.jobsboard.domain.user.User
import com.rockthejvm.jobsboard.domain.security.Authenticator
import tsec.mac.jca.HMACSHA256
import tsec.authentication.IdentityStore
import cats.data.OptionT
import tsec.authentication.JWTAuthenticator
import concurrent.duration.DurationInt
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

class AuthSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with UsersFixture {

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

  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to retrieved users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == danielEmail) OptionT.pure(Daniel)
      else if (email == riccardoEmail) OptionT.pure(Riccardo)
      else OptionT.none[IO, User]
    // jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,   // expiry of tokens
      None,    // max idle (optional)
      idStore, // identity store
      key      // hash key
    )
  }

  "Auth 'algebra'" - {
    "login should return None if the user doesn't exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login("user@rockthejvm.com", "password")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return None if the user exists but the password is wrong" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(danielEmail, "wrongpassword")
      } yield maybeToken

      program.asserting(_ shouldBe None)
    }

    "login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login(danielEmail, "rockthejvm")
      } yield maybeToken

      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
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
        auth <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
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
        auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        result <- auth.changePassword("alice@rockthejvm.com", NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "change password should correctly change password if all details are correct" in {
      val program = for {
        auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
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