package com.rockthejvm.jobsboard.modules

import cats.*
import cats.effect.*
import org.http4s.server.Router
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.http.routes.{HealthRoutes, JobRoutes, AuthRoutes}

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F](core.jobs).routes
  private val authRoutes   = AuthRoutes[F](core.auth).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core))
}
