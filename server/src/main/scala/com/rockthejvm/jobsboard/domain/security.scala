package com.rockthejvm.jobsboard.domain

import tsec.mac.jca.HMACSHA256
import tsec.authentication.{
  AugmentedJWT,
  SecuredRequest,
  JWTAuthenticator,
  TSecAuthService,
  SecuredRequestHandler
}
import tsec.authorization.{BasicRBAC, AuthorizationInfo}

import cats.{Applicative, MonadThrow, Monad}
import cats.implicits.*
import cats.kernel.Semigroup

import org.http4s.Response

import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.*
import org.http4s.Status

object security {
  type Crypto              = HMACSHA256
  type JwtToken            = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  // type aliases for http routes
  type AuthRBAC[F[_]]       = BasicRBAC[F, Role, User, JwtToken]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  object SecuredHandler {
    def apply[F[_]](using handler: SecuredHandler[F]): SecuredHandler[F] = handler
  }

  // RBAC
  // BasicRBAC[F, Role, User, JwtToken]
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.RECRUITER)

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(Role.ADMIN)

  // authorization
  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations {
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  // AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute
  // 1. AuthRoute -> Authorizations = .restrictedTo extension method
  extension [F[_]](authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  // 2. Authorizations -> TSecAuthService = implicit conversion
  given auth2tsec[F[_]: Monad]: Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] =
    authz => {
      val unauthorizedService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

      authz.rbacRoutes // map[RBAC, List[AuthRoute[F]]]
        .toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          // merge routes into one
          val bigRoute = routes.reduce(_.orElse(_))
          // build a new service, fall back to the acc if rbac/route fails
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
    }

  // 3.
}
