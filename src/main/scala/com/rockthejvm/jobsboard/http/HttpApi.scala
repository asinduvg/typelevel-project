package com.rockthejvm.jobsboard.http

import cats.Monad
import org.http4s.server.Router
import cats.syntax.all.*

class HttpApi[F[_]: Monad] private {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Monad]: HttpApi[F] = new HttpApi[F]
}
