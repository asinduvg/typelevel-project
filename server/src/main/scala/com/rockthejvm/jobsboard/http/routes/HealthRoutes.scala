package com.rockthejvm.jobsboard.http.routes

import org.http4s.HttpRoutes
import cats.Monad
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class HealthRoutes[F[_]: Monad] extends Http4sDsl[F] {
  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok("All going good!")
  }

  val routes = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad]: HealthRoutes[F] = new HealthRoutes[F]
}
