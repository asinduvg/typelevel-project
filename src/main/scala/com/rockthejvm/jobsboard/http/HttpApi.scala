package com.rockthejvm.jobsboard.http

import cats.*
import cats.effect.*
import org.http4s.server.Router
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger]: HttpApi[F] = new HttpApi[F]
}
