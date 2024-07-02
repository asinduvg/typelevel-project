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

import tsec.authentication.{asAuthed, SecuredRequestHandler}

import java.util.UUID
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.logging.syntax.*
import org.http4s.HttpRoutes

import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.domain.pagination.*

import scala.language.implicitConversions

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F], authenticator: Authenticator[F])
    extends HttpValidationDsl[F] {

  private val securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)

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
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed _ =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        id   <- jobs.create("todo@rockthejvm.com", jobInfo)
        resp <- Created(id)
      } yield resp
    }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] = {
    case req @ PUT -> Root / UUIDVar(uuid) asAuthed user =>
      req.request.validate[JobInfo] { jobInfo =>
        jobs.find(uuid).flatMap {
          case None =>
            NotFound(FailureResponse(s"Cannot update job $uuid: not found"))
          case Some(job) if user.owns(job) || user.isAdmin =>
            jobs.update(uuid, jobInfo) *> Ok()
          case _ => Forbidden(FailureResponse("You can only update your own jobs"))
        }
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = { case DELETE -> Root / UUIDVar(uuid) asAuthed user =>
    jobs.find(uuid).flatMap {
      case None => NotFound(FailureResponse(s"Cannot delete job $uuid: not found"))
      case Some(job) if user.owns(job) || user.isAdmin =>
        jobs.delete(uuid) *> Ok()
      case _ => Forbidden(FailureResponse("You can only delete your own jobs"))
    }
  }

  val unauthedRoutes = allJobsRoute <+> findJobRoute
  val authedRoutes = securedHandler.liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](
      jobs: Jobs[F],
      authenticator: Authenticator[F]
  ): JobRoutes[F] = new JobRoutes[F](jobs, authenticator)
}
