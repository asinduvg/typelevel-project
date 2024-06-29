package com.rockthejvm.jobsboard.domain

import tsec.mac.jca.HMACSHA256
import tsec.authentication.{AugmentedJWT, SecuredRequest, JWTAuthenticator}

import org.http4s.Response

import com.rockthejvm.jobsboard.domain.user.User

object security {
  type Crypto              = HMACSHA256
  type JwtToken            = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]     = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
}
