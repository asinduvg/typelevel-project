package com.rockthejvm.jobsboard.http

import org.http4s.HttpRoutes
import cats.Monad
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.Http
import java.net.http.HttpRequest
import cats.syntax.all.*

class JobRoutes[F[_]: Monad] extends Http4sDsl[F] {

  // POST /jobs?offset=x&limit=y { filters } // TODO: add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok("TODO")
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(uuid) =>
    Ok(s"TODO find job for $uuid")
  }

  // POST /jobs/create { jobsInfo }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok("TODO")
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case PUT -> Root / UUIDVar(uuid) =>
    Ok(s"TODO update job for $uuid")
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(uuid) =>
      Ok(s"TODO delete job for $uuid")
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Monad]: JobRoutes[F] = new JobRoutes[F]
}
