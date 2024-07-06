package com.rockthejvm.jobsboard.fixtures

import cats.data.* 
import cats.effect.* 
import tsec.mac.jca.{HMACSHA256}
import tsec.jws.mac.JWTMac
import tsec.authentication.{IdentityStore, JWTAuthenticator}

import scala.concurrent.duration.*

import com.rockthejvm.jobsboard.domain.user.* 
import com.rockthejvm.jobsboard.domain.security.*
import org.http4s.Request
import org.http4s.headers.Authorization
import org.http4s.Credentials
import org.http4s.AuthScheme
import tsec.authentication.SecuredRequestHandler

trait SecuredRouteFixture extends UserFixture {
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

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        // Authorization: Bearer {jwt}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }

  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
