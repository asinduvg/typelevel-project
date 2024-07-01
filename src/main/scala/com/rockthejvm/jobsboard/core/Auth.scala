package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import cats.data.OptionT
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import com.rockthejvm.jobsboard.config.{SecurityConfig}
import tsec.authentication.{JWTAuthenticator, IdentityStore, BackingStore}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.common.SecureRandomId

import concurrent.duration.DurationInt

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signup(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): F[Either[String, Option[User]]]
  def delete(email: String): F[Boolean]
  def authenticator: Authenticator[F]
  // TODO: password recovery via email
}

class LiveAuth[F[_]: Async: Logger] private (
    users: Users[F],
    override val authenticator: Authenticator[F]
) extends Auth[F] {

  override def login(email: String, password: String): F[Option[JwtToken]] =
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

      // return a new token if password matches
      maybeJwtToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    } yield maybeJwtToken

  override def signup(newUserInfo: NewUserInfo): F[Option[User]] =
    // find the user in the db, if we did => None
    users.find(newUserInfo.email).flatMap {
      case Some(_) => None.pure[F]
      case None =>
        for {
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
    def updateUser(user: User, newPassword: String): F[Option[User]] =
      for {
        // if password ok, hash new password
        newHashedPw <- BCrypt.hashpw[F](newPassword)
        // update
        updatedUser <- users.update(user.copy(hashedPassword = newHashedPw))
      } yield updatedUser

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
}

object LiveAuth {
  def apply[F[_]: Async: Logger](
      users: Users[F]
  )(securityConfig: SecurityConfig): F[LiveAuth[F]] = {

    // 1. identity store: String => OptionT[F, User]
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // 2. backing store for JWT tokens: BackingStore[F, id, JwtToken]
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT(ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. hashing key
    val keyF =
      HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8")) // TODO: move to config

    // 4. authenticator
    for {
      key        <- keyF
      tokenStore <- tokenStoreF
      authenticator = JWTAuthenticator.backed.inBearerToken(
        expiryDuration = securityConfig.jwtExpiryDuration, // expiry of tokens
        maxIdle = None,                                    // max idle (optional)
        identityStore = idStore,                           // identity store
        tokenStore = tokenStore,
        signingKey = key // hash key
      )
    } yield new LiveAuth[F](users, authenticator) // 5. Live auth
  }
}
