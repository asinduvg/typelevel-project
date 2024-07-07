package com.rockthejvm.jobsboard

import cats.effect.IOApp
import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import cats.effect.*
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.port
import pureconfig.ConfigSource
import com.rockthejvm.jobsboard.config.AppConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.modules.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.syntax.*
import org.http4s.server.middleware.CORS

object Application extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig, tokenConfig, emailServiceConfig) =>
      val appResource = for {
        xa      <- Database.makePostgresResource[IO](postgresConfig) // DB connection
        core    <- Core[IO](xa, tokenConfig, emailServiceConfig)     // DB layer
        httpApi <- HttpApi[IO](core, securityConfig)                 // Business Logic layer
        server <- EmberServerBuilder // Server layer
          .default[IO]
          .withHost(emberConfig.host) // String, need Host
          .withPort(emberConfig.port)
          .withHttpApp(CORS(httpApi.endpoints).orNotFound) // TODO: remove this when deploying
          .build
      } yield server

      appResource.use(_ => IO.println("Server ready!") *> IO.never)
  }
}
