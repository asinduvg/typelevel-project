package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import cats.data.OptionT
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.*
import com.rockthejvm.jobsboard.config.{SecurityConfig}
import tsec.authentication.{JWTAuthenticator, IdentityStore, BackingStore}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.common.SecureRandomId

import concurrent.duration.DurationInt

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[User]]
  def signup(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
  // allow password recovery
  def sendPasswordRecoveryToken(email: String): F[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): F[Boolean]
}

class LiveAuth[F[_]: Async: Logger] private (users: Users[F], tokens: Tokens[F], emails: Emails[F])
    extends Auth[F] {

  override def login(email: String, password: String): F[Option[User]] =
    for {
      // find the user in the DB -> return None if no user
      maybeUser <- users.find(email)
      // check password
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt
          .checkpwBool[F](
            password,
            PasswordHash[BCrypt](user.hashedPassword)
          )
      )
    } yield maybeValidatedUser

  override def signup(newUserInfo: NewUserInfo): F[Option[User]] =
    // find the user in the db, if we did => None
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
          _ <- Logger[F].info(s"Creating new user ${newUserInfo.email}")
          // hash the password
          hashedPw <- BCrypt.hashpw[F](newUserInfo.password)
          user <- User(
            newUserInfo.email,
            hashedPw,
            newUserInfo.firstName,
            newUserInfo.lastName,
            newUserInfo.company,
            Role.RECRUITER
          ).pure[F]
          // create a new user in the db
          _ <- users.create(user)
        } yield Some(user)
    }

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]] =
    def checkAndUpdate(
        user: User,
        oldPassword: String,
        newPassword: String
    ): F[Either[String, Option[User]]] =
      for {
        // if user, check password
        validPassword <- BCrypt
          .checkpwBool[F](
            oldPassword,
            PasswordHash[BCrypt](user.hashedPassword)
          )
        result <-
          if (validPassword) updateUser(user, newPassword).map(Right(_))
          else Left("Invalid password").pure[F]
      } yield result

    // find user
    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
        checkAndUpdate(user, oldPassword, newPassword)
    }

  override def delete(email: String): F[Boolean] =
    users.delete(email)

  // password recovery
  override def sendPasswordRecoveryToken(email: String): F[Unit] =
    tokens.getToken(email).flatMap {
      case Some(token) => emails.sendPasswordRecoveryEmail(email, token)
      case None        => ().pure[F]
    }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): F[Boolean] = for {
    maybeUser    <- users.find(email)
    tokenIsValid <- tokens.checkToken(email, token)
    result <- (maybeUser, tokenIsValid) match {
      case (Some(user), true) => updateUser(user, newPassword).map(_.nonEmpty)
      case _                  => false.pure[F]
    }
  } yield result

  private def updateUser(user: User, newPassword: String): F[Option[User]] =
    for {
      // if password ok, hash new password
      newHashedPw <- BCrypt.hashpw[F](newPassword)
      // update
      updatedUser <- users.update(user.copy(hashedPassword = newHashedPw))
    } yield updatedUser

}

object LiveAuth {
  def apply[F[_]: Async: Logger](
      users: Users[F],
      tokens: Tokens[F],
      emails: Emails[F]
  ): F[LiveAuth[F]] = {
    new LiveAuth[F](users, tokens, emails).pure[F]
  }
}
