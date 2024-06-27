package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.*
import cats.effect.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.Http
import java.net.http.HttpRequest
import cats.syntax.all.*

import java.util.UUID
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.logging.syntax.*
import org.http4s.HttpRoutes

import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.domain.pagination.*

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F] {

  object OffsetQueryParam   extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?limit=x&offset=y { filters } // TODO: add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitsetQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter <- req.as[JobFilter]
        jobs   <- jobs.all(filter, Pagination(limit, offset))
        resp   <- Ok(jobs)
      } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(uuid) =>
    for {
      job <- jobs.find(uuid)
      resp <- job match
        case Some(job) => Ok(job)
        case None      => NotFound(FailureResponse(s"Job with UUID $uuid not found"))
    } yield resp
  }

  // POST /jobs/create { jobsInfo }
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          id   <- jobs.create("todo@rockthejvm.com", jobInfo)
          resp <- Created(id)
        } yield resp
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(uuid) =>
      req.validate[JobInfo] { jobInfo =>
        for {
          maybeNewJob <- jobs.update(uuid, jobInfo)
          resp <- maybeNewJob match
            case None    => NotFound(FailureResponse(s"Cannot update job $uuid: not found"))
            case Some(_) => Ok()
        } yield resp
      }

  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(uuid) =>
      for {
        maybeJob <- jobs.find(uuid)
        resp <- maybeJob match
          case None => NotFound(FailureResponse(s"Cannot delete job $uuid: not found"))
          case Some(_) =>
            for {
              _    <- jobs.delete(uuid)
              resp <- Ok()
            } yield resp
      } yield resp
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]): JobRoutes[F] = new JobRoutes[F](jobs)
}
