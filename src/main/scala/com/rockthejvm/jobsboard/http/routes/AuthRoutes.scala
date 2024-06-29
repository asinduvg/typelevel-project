package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.http.validation.syntax.HttpValidationDsl
import com.rockthejvm.jobsboard.core.Auth
import org.http4s.server.Router
import org.http4s.HttpRoutes
import com.rockthejvm.jobsboard.domain.auth.LoginInfo
import com.rockthejvm.jobsboard.domain.user.NewUserInfo
import org.http4s.headers.Authorization

class AuthRoutes[F[_]: Concurrent: Logger] private (auth: Auth[F]) extends HttpValidationDsl[F] {

  // POST /auth/login { LoginInfo } => Ok with JWT as Authorization: Bearer {jwt}
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    // for {
    //   loginInfo <- req.as[LoginInfo]
    //   jwtToken  <- auth.login(loginInfo.email, loginInfo.password)
    //   resp <- jwtToken match
    //     case Some(jwt) => Ok(jwt.identity)
    //     case None      => Ok("Unauthorized")
    // } yield resp
    Ok("TODO")
  }

  // POST /auth/users { NewUserInfo } => 201 created
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      // for {
      //     newUserInfo <- req.as[NewUserInfo]
      //     header <- req.headers.get[Authorization]
      // } yield ???
      Ok("TODO")
  }

// PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {jwt} } => 200 Ok
  private val changePasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / "users" / "password" =>
      Ok("TODO")
  }

  // POST /auth/logout { Authorization: Bearer {jwt} } => 200 Ok
  private val logoutRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "logout" =>
    Ok("TODO")
  }

  val routes = Router(
    "/auth" -> (loginRoute <+> createUserRoute <+> changePasswordRoute <+> logoutRoute)
  )

}

object AuthRoutes {
  def apply[F[_]: Concurrent: Logger](auth: Auth[F]) =
    new AuthRoutes[F](auth)
}
