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
import com.rockthejvm.jobsboard.config.EmberConfig
import com.rockthejvm.jobsboard.config.syntax.*
import com.rockthejvm.jobsboard.http.HttpApi

/*
    1. Add a plain healthpoint to the application
    2. Add minimal configuration
    3. Basic http server layout
 */

object Application extends IOApp.Simple {

  val configSource = ConfigSource.default.load[EmberConfig]

  override def run = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host) // String, need Host
      .withPort(config.port)
      .withHttpApp(HttpApi[IO].endpoints.orNotFound)
      .build
      .use(_ => IO.println("Server ready!") *> IO.never)
  }
}
